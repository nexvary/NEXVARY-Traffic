import com.nexvary.trafficlab.vpn.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public final class SocksGatewayTest {
    private static final LocalSocksGateway.Protector ALLOW=new LocalSocksGateway.Protector(){
        public boolean protect(Socket s){return true;} public boolean protect(DatagramSocket s){return true;}
    };
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static DataInputStream auth(Socket s,LocalSocksGateway gateway,boolean correct)throws Exception{
        s.setSoTimeout(5000);DataInputStream in=new DataInputStream(s.getInputStream());DataOutputStream out=new DataOutputStream(s.getOutputStream());
        out.write(new byte[]{5,1,2});out.flush();check(in.readUnsignedByte()==5&&in.readUnsignedByte()==2,"auth selection");
        byte[] u=gateway.username().getBytes(StandardCharsets.US_ASCII),p=(correct?gateway.password():"wrong").getBytes(StandardCharsets.US_ASCII);
        out.writeByte(1);out.writeByte(u.length);out.write(u);out.writeByte(p.length);out.write(p);out.flush();
        check(in.readUnsignedByte()==1,"auth version");check(in.readUnsignedByte()==(correct?0:1),"auth result");return in;
    }
    private static int request(Socket s,DataInputStream in,int command,String host,int port)throws Exception{
        DataOutputStream out=new DataOutputStream(s.getOutputStream());byte[] addr=InetAddress.getByName(host).getAddress();
        out.write(new byte[]{5,(byte)command,0,(byte)(addr.length==4?1:4)});out.write(addr);out.writeShort(port);out.flush();
        check(in.readUnsignedByte()==5,"reply version");check(in.readUnsignedByte()==0,"request rejected");check(in.readUnsignedByte()==0,"reserved");int type=in.readUnsignedByte();byte[] a=new byte[type==1?4:16];in.readFully(a);return in.readUnsignedShort();
    }
    public static void main(String[] args)throws Exception{
        ExecutorService threads=Executors.newCachedThreadPool();
        try(LocalSocksGateway gateway=new LocalSocksGateway(ALLOW,null,443,8888,()->false)){
            int port=gateway.start();
            try(Socket denied=new Socket("127.0.0.1",port)){auth(denied,gateway,false);}
            for(String host:new String[]{"127.0.0.1","::1"}){
                try(ServerSocket echo=new ServerSocket(0,1,InetAddress.getByName(host))){
                    Future<?> server=threads.submit(()->{try(Socket s=echo.accept()){int b=s.getInputStream().read();s.getOutputStream().write(b+1);}catch(IOException e){throw new RuntimeException(e);}});
                    try(Socket client=new Socket("127.0.0.1",port)){DataInputStream in=auth(client,gateway,true);request(client,in,1,host,echo.getLocalPort());client.getOutputStream().write(41);check(in.read()==42,"TCP "+host);}
                    server.get(5,TimeUnit.SECONDS);
                }
            }
            try(DatagramSocket echo=new DatagramSocket(new InetSocketAddress("127.0.0.1",0));Socket control=new Socket("127.0.0.1",port);DatagramSocket client=new DatagramSocket(new InetSocketAddress("127.0.0.1",0))){
                echo.setSoTimeout(5000);client.setSoTimeout(5000);
                Future<?> server=threads.submit(()->{try{byte[] b=new byte[100];DatagramPacket p=new DatagramPacket(b,b.length);echo.receive(p);check(p.getLength()==3,"UDP body");echo.send(p);}catch(IOException e){throw new RuntimeException(e);}});
                DataInputStream in=auth(control,gateway,true);int udp=request(control,in,3,"0.0.0.0",0);
                ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.write(new byte[]{0,0,0,1,127,0,0,1});out.writeShort(echo.getLocalPort());out.write(new byte[]{10,20,30});
                byte[] data=b.toByteArray();client.send(new DatagramPacket(data,data.length,InetAddress.getByName("127.0.0.1"),udp));
                DatagramPacket reply=new DatagramPacket(new byte[100],100);client.receive(reply);check(reply.getLength()==13&&reply.getData()[12]==30,"UDP encapsulation");server.get(5,TimeUnit.SECONDS);
            }
            try(Socket active=new Socket("127.0.0.1",port)){auth(active,gateway,true);gateway.close();check(active.getInputStream().read()==-1,"stop closes active sockets");}
        }finally{threads.shutdownNow();}
        check(TunnelConfig.create(1000,"user","secret").contains("mapdns:"),"DNS configuration");
        try{TunnelConfig.create(1000,"user","'\nbad");throw new AssertionError("YAML injection");}catch(IllegalArgumentException expected){}
        System.out.println("SocksGatewayTest passed: authentication, TCP IPv4/IPv6, UDP, shutdown, configuration");
    }
}
