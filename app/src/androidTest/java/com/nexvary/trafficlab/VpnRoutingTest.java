package com.nexvary.trafficlab;
import android.app.Activity;
import android.content.*;
import android.net.VpnService;
import android.test.InstrumentationTestCase;
import com.nexvary.trafficlab.vpn.LabVpnService;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class VpnRoutingTest extends InstrumentationTestCase {
    public void testSeparateApplicationTcpUdpDnsAndStop()throws Exception{
        Context context=getInstrumentation().getTargetContext();AuthSession.grant();
        Activity activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
        BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent intent){result.set(intent.getStringExtra("result"));done.countDown();}};
        context.registerReceiver(receiver,new IntentFilter("com.nexvary.trafficlab.FIXTURE_RESULT"),Context.RECEIVER_EXPORTED);
        try{
            assertNull("CI must grant VPN consent through appops",VpnService.prepare(context));
            getInstrumentation().runOnMainSync(()->context.startForegroundService(new Intent(context,LabVpnService.class)
                    .putExtra("packages",new String[]{"com.nexvary.trafficfixture"}).putExtra("host","lab.invalid").putExtra("port",443).putExtra("recordPcap",true)));
            long until=System.currentTimeMillis()+10000;
            while(!LabVpnService.state.equals("RUNNING")&&System.currentTimeMillis()<until)Thread.sleep(25);
            assertEquals(LabVpnService.state,"RUNNING",LabVpnService.state);
            try{com.nexvary.trafficlab.vpn.VpnPacketCapture.export(context,new java.io.ByteArrayOutputStream());fail("Export of active recording must be rejected");}catch(java.io.IOException expected){}
            // Allow ConnectivityService to publish routes before the separate UID opens sockets.
            Thread.sleep(500);
            context.startActivity(new Intent().setClassName("com.nexvary.trafficfixture","com.nexvary.trafficfixture.TrafficFixtureActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            assertTrue("Training app did not return: "+LabVpnService.state,done.await(40,TimeUnit.SECONDS));
            assertEquals(LabVpnService.diagnostics(),"TCP_UDP_DNS_OK",result.get());
            long[] counters=LabVpnService.stats();assertTrue("Traffic bypassed native tunnel",counters[0]>0&&counters[2]>0);
            context.startService(new Intent(context,LabVpnService.class).setAction("stop"));
            until=System.currentTimeMillis()+5000;while(!LabVpnService.state.equals("STOPPED")&&System.currentTimeMillis()<until)Thread.sleep(25);
            assertEquals("STOPPED",LabVpnService.state);assertFalse(hev.htproxy.TProxyService.TProxyIsRunning());
            java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
            com.nexvary.trafficlab.vpn.VpnPacketCapture.export(context,output);
            java.nio.ByteBuffer pcap=java.nio.ByteBuffer.wrap(output.toByteArray()).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            assertEquals(0xa1b2c3d4,pcap.getInt());pcap.position(20);assertEquals(101,pcap.getInt());
            int outgoing=0,incoming=0,tcp=0,udp=0,dns=0;
            while(pcap.hasRemaining()){
                assertTrue("Truncated PCAP record",pcap.remaining()>=16);int sec=pcap.getInt(),micros=pcap.getInt(),length=pcap.getInt(),original=pcap.getInt();
                assertTrue(sec>0&&micros>=0&&micros<1000000);assertEquals(original,length);assertTrue(length>=20&&length<=pcap.remaining());
                byte[] packet=new byte[length];pcap.get(packet);int version=(packet[0]&255)>>>4;
                assertTrue("Not an IP packet",version==4||version==6);
                if(version==4){
                    int total=((packet[2]&255)<<8)|(packet[3]&255);assertEquals("Packet truncated",total,length);
                    int protocol=packet[9]&255;if(protocol==6)tcp++;if(protocol==17)udp++;
                    if(packet[12]==(byte)198&&packet[13]==18)outgoing++;if(packet[16]==(byte)198&&packet[17]==18)incoming++;
                    int header=(packet[0]&15)*4;
                    if(protocol==17&&length>=header+8&&(((packet[header]&255)<<8)|(packet[header+1]&255))==53)dns++;
                }
            }
            assertTrue("Missing recorded directions/protocols: "+outgoing+"/"+incoming+"/"+tcp+"/"+udp+"/"+dns,outgoing>0&&incoming>0&&tcp>0&&udp>0&&dns>0);
            com.nexvary.trafficlab.vpn.VpnPacketCapture.clear(context);
            assertEquals("EMPTY",com.nexvary.trafficlab.vpn.VpnPacketCapture.status(context));
        }finally{
            context.unregisterReceiver(receiver);LabVpnService.stopNow();context.stopService(new Intent(context,LabVpnService.class));
            getInstrumentation().runOnMainSync(activity::finish);AuthSession.revoke();
        }
    }
    public void testVpnHttpsInspectionFromSeparateApplication()throws Exception{
        Context context=getInstrumentation().getTargetContext();AuthSession.grant();
        Activity activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
        BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent intent){result.set(intent.getStringExtra("result"));done.countDown();}};
        context.registerReceiver(receiver,new IntentFilter("com.nexvary.trafficlab.FIXTURE_RESULT"),Context.RECEIVER_EXPORTED);
        com.nexvary.trafficlab.tls.LabCertificateAuthority ca=com.nexvary.trafficlab.tls.LabCertificateAuthority.open(context);
        java.security.cert.Certificate certificate;
        try(android.os.ParcelFileDescriptor data=getInstrumentation().getUiAutomation().executeShellCommand("cat /data/local/tmp/nexvary-fixture-ca.der");
            java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(data)){
            certificate=java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        java.security.KeyStore trust=java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());trust.load(null);trust.setCertificateEntry("fixture",certificate);
        javax.net.ssl.TrustManagerFactory tm=javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());tm.init(trust);
        javax.net.ssl.SSLContext trusted=javax.net.ssl.SSLContext.getInstance("TLS");trusted.init(null,tm.getTrustManagers(),null);
        try(com.nexvary.trafficlab.tls.EmbeddedTlsProxy proxy=new com.nexvary.trafficlab.tls.EmbeddedTlsProxy(ca,"10.0.2.2",18779,true,trusted)){
            proxy.start(8888);com.nexvary.trafficlab.tls.EmbeddedProxyService.engine=proxy;
            getInstrumentation().runOnMainSync(()->context.startForegroundService(new Intent(context,LabVpnService.class).putExtra("packages",new String[]{"com.nexvary.trafficfixture"}).putExtra("host","10.0.2.2").putExtra("port",18779)));
            long deadline=System.currentTimeMillis()+10000;while(!LabVpnService.state.equals("RUNNING")&&System.currentTimeMillis()<deadline)Thread.sleep(25);assertEquals("RUNNING",LabVpnService.state);
            Thread.sleep(500);
            context.startActivity(new Intent().setClassName("com.nexvary.trafficfixture","com.nexvary.trafficfixture.TrafficFixtureActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("mode","https").putExtra("certificate",ca.encoded()));
            com.nexvary.trafficlab.tls.EmbeddedTlsProxy.Flow selected=null;deadline=System.currentTimeMillis()+10000;
            while(selected==null&&System.currentTimeMillis()<deadline){for(com.nexvary.trafficlab.tls.EmbeddedTlsProxy.Flow flow:proxy.flows())if(flow.status.equals("PAUSED"))selected=flow;if(selected==null)Thread.sleep(10);}
            assertNotNull("VPN did not reach HTTPS inspection: "+LabVpnService.diagnostics()+" / "+result.get(),selected);
            assertTrue(selected.request.contains("price=100"));proxy.decide(selected.id,selected.request.replace("price=100","price=1"),false);
            assertTrue("Training HTTPS request did not complete",done.await(20,TimeUnit.SECONDS));assertEquals(LabVpnService.diagnostics(),"HTTPS_EDITED_OK",result.get());
        }finally{
            com.nexvary.trafficlab.tls.EmbeddedProxyService.engine=null;context.unregisterReceiver(receiver);LabVpnService.stopNow();context.stopService(new Intent(context,LabVpnService.class));
            long deadline=System.currentTimeMillis()+5000;while(!LabVpnService.state.equals("STOPPED")&&System.currentTimeMillis()<deadline)Thread.sleep(25);
            getInstrumentation().runOnMainSync(activity::finish);AuthSession.revoke();
        }
    }

}
