package com.nexvary.trafficfixture;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
/** Separate UID traffic generator installed by CI only; never packaged in the product. */
public final class TrafficFixtureActivity extends Activity {
    @Override public void onCreate(Bundle saved){super.onCreate(saved);new Thread(()->{
        String result;
        try{
            if("https".equals(getIntent().getStringExtra("mode"))){https();return;}
            String tcp;
            try(Socket socket=new Socket()){
                socket.connect(new InetSocketAddress("10.0.2.2",18777),10000);socket.setSoTimeout(10000);
                socket.getOutputStream().write("GET /fixture HTTP/1.1\r\nHost: fixture\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[1024];int n;while((n=socket.getInputStream().read(b))!=-1)out.write(b,0,n);tcp=out.toString("UTF-8");
            }
            if(!tcp.contains("NEXVARY_TCP_OK"))throw new IOException("TCP fixture response");
            try(DatagramSocket socket=new DatagramSocket()){
                socket.setSoTimeout(10000);byte[] b="NEXVARY_UDP_OK".getBytes(StandardCharsets.US_ASCII);
                socket.send(new DatagramPacket(b,b.length,InetAddress.getByName("10.0.2.2"),18778));
                DatagramPacket reply=new DatagramPacket(new byte[100],100);socket.receive(reply);
                if(!new String(reply.getData(),0,reply.getLength(),StandardCharsets.US_ASCII).equals("NEXVARY_UDP_OK"))throw new IOException("UDP fixture response");
            }
            try(DatagramSocket dns=new DatagramSocket()){
                dns.setSoTimeout(10000);ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
                out.writeShort(0x1234);out.writeShort(0x0100);out.writeShort(1);out.writeShort(0);out.writeShort(0);out.writeShort(0);
                for(String label:"nexvary-fixture.invalid".split("\\.")){out.writeByte(label.length());out.writeBytes(label);}out.writeByte(0);out.writeShort(1);out.writeShort(1);
                byte[] query=bytes.toByteArray();dns.send(new DatagramPacket(query,query.length,InetAddress.getByName("198.18.0.2"),53));
                DatagramPacket response=new DatagramPacket(new byte[512],512);dns.receive(response);
                byte[] b=response.getData();int size=response.getLength();
                if(size<query.length+16||b[0]!=0x12||b[1]!=0x34||b[6]!=0||b[7]==0)throw new IOException("Mapped DNS response");
                boolean mapped=false;for(int i=query.length;i<size-3;i++)if((b[i]&255)==100&&(b[i+1]&192)==64)mapped=true;
                if(!mapped)throw new IOException("Mapped DNS address absent");
            }
            result="TCP_UDP_DNS_OK";
        }catch(Exception error){result="ERROR: "+error;}
        sendBroadcast(new Intent("com.nexvary.trafficlab.FIXTURE_RESULT").setPackage("com.nexvary.trafficlab").putExtra("result",result));
        runOnUiThread(this::finish);
    },"fixture-traffic").start();}
    private void https(){
        String result;
        try{
            java.security.KeyStore trust=java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());trust.load(null);
            java.security.cert.Certificate certificate=java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(getIntent().getByteArrayExtra("certificate")));trust.setCertificateEntry("lab",certificate);
            javax.net.ssl.TrustManagerFactory tm=javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());tm.init(trust);
            javax.net.ssl.SSLContext tls=javax.net.ssl.SSLContext.getInstance("TLS");tls.init(null,tm.getTrustManagers(),null);
            try(javax.net.ssl.SSLSocket socket=(javax.net.ssl.SSLSocket)tls.getSocketFactory().createSocket("10.0.2.2",18779)){
                socket.setSoTimeout(15000);javax.net.ssl.SSLParameters parameters=socket.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(parameters);socket.startHandshake();
                byte[] body="price=100".getBytes(StandardCharsets.US_ASCII);
                socket.getOutputStream().write(("POST /lesson HTTP/1.1\r\nHost: 10.0.2.2\r\nContent-Length: "+body.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().write(body);socket.getOutputStream().flush();
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[1024];int n;while((n=socket.getInputStream().read(b))!=-1)out.write(b,0,n);
                String response=out.toString("UTF-8");if(!response.contains("accepted:price=1")||response.contains("price=100"))throw new IOException("Edited HTTPS response absent");
            }
            result="HTTPS_EDITED_OK";
        }catch(Exception error){result="ERROR: "+error;}
        sendBroadcast(new Intent("com.nexvary.trafficlab.FIXTURE_RESULT").setPackage("com.nexvary.trafficlab").putExtra("result",result));runOnUiThread(this::finish);
    }

}
