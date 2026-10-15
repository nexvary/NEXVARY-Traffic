package com.nexvary.trafficlab;

import android.os.ParcelFileDescriptor;
import android.system.*;
import android.test.InstrumentationTestCase;
import com.nexvary.trafficlab.vpn.*;
import hev.htproxy.TProxyService;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Native TUN packet path using datagram descriptors: no root or external servers. */
public final class NativeTunnelTest extends InstrumentationTestCase {
    private static int word(byte[] b,int o){return ((b[o]&255)<<8)|(b[o+1]&255);}
    private static void word(byte[] b,int o,int n){b[o]=(byte)(n>>>8);b[o+1]=(byte)n;}
    private static int checksum(byte[] b,int from,int to,int seed){long s=seed;for(int i=from;i<to;i+=2)s+=((b[i]&255)<<8)|(i+1<to?b[i+1]&255:0);while((s>>>16)!=0)s=(s&65535)+(s>>>16);return (int)(~s)&65535;}
    private static byte[] udp(boolean ipv6,int port,byte[] payload)throws Exception{
        int h=ipv6?40:20;byte[] packet=new byte[h+8+payload.length];
        byte[] src=InetAddress.getByName(ipv6?"fd77::2":"198.18.0.3").getAddress();
        byte[] dst=InetAddress.getByName(ipv6?"2001:db8::7":"198.51.100.7").getAddress();
        if(ipv6){packet[0]=0x60;word(packet,4,8+payload.length);packet[6]=17;packet[7]=64;System.arraycopy(src,0,packet,8,16);System.arraycopy(dst,0,packet,24,16);}
        else{packet[0]=0x45;word(packet,2,packet.length);packet[8]=64;packet[9]=17;System.arraycopy(src,0,packet,12,4);System.arraycopy(dst,0,packet,16,4);word(packet,10,checksum(packet,0,20,0));}
        word(packet,h,32000);word(packet,h+2,port);word(packet,h+4,8+payload.length);System.arraycopy(payload,0,packet,h+8,payload.length);
        int seed=17+8+payload.length;for(int i=0;i<src.length;i+=2){seed+=word(src,i);seed+=word(dst,i);}
        int sum=checksum(packet,h,packet.length,seed);word(packet,h+6,sum==0?65535:sum);return packet;
    }
    public void testNativeIpv4AndIpv6UdpForwardingAndRestart()throws Exception{
        for(boolean ipv6:new boolean[]{false,true}){
            ExecutorService threads=Executors.newSingleThreadExecutor();
            File config=new File(getInstrumentation().getTargetContext().getCacheDir(),"native-test.yml");
            FileDescriptor left=new FileDescriptor(),right=new FileDescriptor();
            Os.socketpair(OsConstants.AF_UNIX,OsConstants.SOCK_DGRAM,0,left,right);
            Os.setsockoptTimeval(right,OsConstants.SOL_SOCKET,OsConstants.SO_RCVTIMEO,StructTimeval.fromMillis(10000));
            ParcelFileDescriptor nativeFd=ParcelFileDescriptor.dup(left);
            LocalSocksGateway.Protector allow=new LocalSocksGateway.Protector(){public boolean protect(Socket s){return true;}public boolean protect(DatagramSocket s){return true;}};
            try(LocalSocksGateway gateway=new LocalSocksGateway(allow,null,443,8888,()->false,host->InetAddress.getByName("127.0.0.1"));
                DatagramSocket echo=new DatagramSocket(new InetSocketAddress("127.0.0.1",0))){
                echo.setSoTimeout(10000);int port=gateway.start();
                try(FileOutputStream out=new FileOutputStream(config)){out.write(TunnelConfig.create(port,gateway.username(),gateway.password()).getBytes(StandardCharsets.UTF_8));}
                Future<?> response=threads.submit(()->{try{DatagramPacket p=new DatagramPacket(new byte[200],200);echo.receive(p);echo.send(p);}catch(IOException e){throw new RuntimeException(e);}});
                assertTrue(TProxyService.TProxyStartService(config.getAbsolutePath(),nativeFd.getFd()));
                Thread.sleep(200);assertTrue("native engine did not start",TProxyService.TProxyIsRunning());
                byte[] expected=(ipv6?"native-ipv6":"native-ipv4").getBytes(StandardCharsets.US_ASCII);
                byte[] packet=udp(ipv6,echo.getLocalPort(),expected);Os.write(right,packet,0,packet.length);
                byte[] answer=new byte[2048];int count=Os.read(right,answer,0,answer.length);int h=ipv6?40:(answer[0]&15)*4;
                assertEquals(ipv6?6:4,answer[0]>>>4);assertEquals(32000,word(answer,h+2));assertEquals(expected.length,count-h-8);
                assertEquals(new String(expected,StandardCharsets.US_ASCII),new String(answer,h+8,count-h-8,StandardCharsets.US_ASCII));
                response.get(5,TimeUnit.SECONDS);
                long deadline=System.currentTimeMillis()+2000;long[] stats=TProxyService.TProxyGetStats();
                while((stats[0]==0||stats[2]==0)&&System.currentTimeMillis()<deadline){Thread.sleep(10);stats=TProxyService.TProxyGetStats();}
                assertTrue("Native counters: "+java.util.Arrays.toString(stats),stats[0]>0&&stats[2]>0);
            }finally{
                TProxyService.TProxyStopService();assertFalse(TProxyService.TProxyIsRunning());nativeFd.close();Os.close(left);Os.close(right);config.delete();threads.shutdownNow();
            }
        }
    }
}
