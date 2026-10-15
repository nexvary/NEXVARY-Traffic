package com.nexvary.trafficlab;

import android.test.InstrumentationTestCase;
import com.nexvary.trafficlab.tls.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.concurrent.*;

public class EmbeddedHttpsTest extends InstrumentationTestCase {
    private LabCertificateAuthority ca;
    private SSLContext trusted;
    @Override protected void setUp() throws Exception {
        super.setUp();ca=LabCertificateAuthority.open(getInstrumentation().getTargetContext());
        KeyStore trust=KeyStore.getInstance(KeyStore.getDefaultType());trust.load(null);trust.setCertificateEntry("lab",ca.certificate());
        TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(trust);
        trusted=SSLContext.getInstance("TLS");trusted.init(null,tm.getTrustManagers(),null);
    }
    private String header(InputStream in) throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();int state=0;while(out.size()<32768){int b=in.read();if(b<0)throw new EOFException();out.write(b);state=(state<<8)|b;if(state==0x0d0a0d0a)return out.toString("ISO-8859-1");}throw new IOException("headers");}
    private String all(InputStream in) throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}
    private String client(int proxyPort,int targetPort,String body) throws Exception {
        try(Socket tcp=new Socket("127.0.0.1",proxyPort)){
            tcp.setSoTimeout(10000);tcp.getOutputStream().write(("CONNECT 127.0.0.1:"+targetPort+" HTTP/1.1\r\nHost: lab\r\n\r\n").getBytes(StandardCharsets.US_ASCII));tcp.getOutputStream().flush();
            assertTrue(header(tcp.getInputStream()).startsWith("HTTP/1.1 200"));
            try(SSLSocket ssl=(SSLSocket)trusted.getSocketFactory().createSocket(tcp,"127.0.0.1",targetPort,false)){
                SSLParameters p=ssl.getSSLParameters();p.setEndpointIdentificationAlgorithm("HTTPS");ssl.setSSLParameters(p);ssl.setSoTimeout(10000);ssl.startHandshake();
                byte[] payload=body.getBytes(StandardCharsets.UTF_8);
                ssl.getOutputStream().write(("POST /lesson HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: "+payload.length+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));ssl.getOutputStream().write(payload);ssl.getOutputStream().flush();return all(ssl.getInputStream());
            }
        }
    }
    private EmbeddedTlsProxy.Flow paused(EmbeddedTlsProxy proxy) throws Exception {
        long until=System.currentTimeMillis()+10000;
        while(System.currentTimeMillis()<until){for(EmbeddedTlsProxy.Flow f:proxy.flows())if(f.status.equals("PAUSED"))return f;Thread.sleep(20);}
        fail("No paused decrypted request: "+(proxy.flows().isEmpty()?"no flows":proxy.flows().get(0).error));return null;
    }
    public void testDecryptModifyForwardAndResponse() throws Exception {
        ExecutorService threads=Executors.newFixedThreadPool(2);
        try(SSLServerSocket server=(SSLServerSocket)ca.serverContext("127.0.0.1").getServerSocketFactory().createServerSocket(0,10,InetAddress.getByName("127.0.0.1"))){
            server.setSoTimeout(15000);int targetPort=server.getLocalPort();
            Future<String> upstream=threads.submit(()->{try(SSLSocket socket=(SSLSocket)server.accept()){
                socket.setSoTimeout(10000);String h=header(socket.getInputStream());int length=0;for(String line:h.split("\r\n"))if(line.startsWith("Content-Length:"))length=Integer.parseInt(line.substring(15).trim());
                byte[] b=new byte[length];new DataInputStream(socket.getInputStream()).readFully(b);String observed=new String(b,StandardCharsets.UTF_8);
                byte[] response=("accepted:"+observed).getBytes(StandardCharsets.UTF_8);socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: "+response.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().write(response);socket.getOutputStream().flush();return observed;
            }});
            try(EmbeddedTlsProxy proxy=new EmbeddedTlsProxy(ca,"127.0.0.1",targetPort,true,trusted)){
                int port=proxy.start(0);Future<String> client=threads.submit(()->client(port,targetPort,"{\"price\":100}"));
                EmbeddedTlsProxy.Flow flow=paused(proxy);assertTrue(flow.request.contains("price\":100"));
                proxy.decide(flow.id,flow.request.replace("price\":100","price\":1"),false);
                String reply=client.get(15,TimeUnit.SECONDS);assertTrue(reply,reply.contains("accepted:{\"price\":1}"));assertEquals("{\"price\":1}",upstream.get(10,TimeUnit.SECONDS));
                assertEquals("COMPLETE",flow.status);assertTrue(flow.response.contains("accepted:"));
            }
        }finally{threads.shutdownNow();}
    }
    public void testDropDoesNotContactUpstream() throws Exception {
        ExecutorService threads=Executors.newSingleThreadExecutor();
        try(ServerSocket upstream=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));EmbeddedTlsProxy proxy=new EmbeddedTlsProxy(ca,"127.0.0.1",upstream.getLocalPort(),true,trusted)){
            int port=proxy.start(0);Future<String> client=threads.submit(()->client(port,upstream.getLocalPort(),"drop-me"));EmbeddedTlsProxy.Flow flow=paused(proxy);
            proxy.decide(flow.id,null,true);assertTrue(client.get(10,TimeUnit.SECONDS).contains("403"));assertEquals("DROPPED",flow.status);
            upstream.setSoTimeout(200);try(Socket unexpected=upstream.accept()){fail("Dropped request reached upstream");}catch(SocketTimeoutException expected){}
        }finally{threads.shutdownNow();}
    }
    public void testOutOfScopeConnectIsRejected() throws Exception {
        try(EmbeddedTlsProxy proxy=new EmbeddedTlsProxy(ca,"localhost",443,false,trusted)){
            int port=proxy.start(0);try(Socket client=new Socket("127.0.0.1",port)){
                client.setSoTimeout(2000);client.getOutputStream().write("CONNECT outside.example:443 HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.US_ASCII));client.getOutputStream().flush();assertTrue(all(client.getInputStream()).startsWith("HTTP/1.1 403"));assertTrue(proxy.flows().isEmpty());
            }
        }
    }
    public void testCaIsPersistentAndSigningKeyNotExportable() throws Exception {
        assertTrue(java.util.Arrays.equals(ca.encoded(),LabCertificateAuthority.open(getInstrumentation().getTargetContext()).encoded()));
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);assertNull(store.getKey("nexvary.traffic.lab.ca.v1",null).getEncoded());
    }
    public void testProductionProxyRejectsUntrustedUpstreamCertificate() throws Exception {
        ExecutorService threads=Executors.newFixedThreadPool(2);
        try(SSLServerSocket server=(SSLServerSocket)ca.serverContext("127.0.0.1").getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
            EmbeddedTlsProxy proxy=new EmbeddedTlsProxy(ca,"127.0.0.1",server.getLocalPort(),false)){
            server.setSoTimeout(10000);
            Future<?> handshake=threads.submit(()->{try(SSLSocket socket=(SSLSocket)server.accept()){socket.setSoTimeout(5000);socket.startHandshake();}catch(Exception expected){}});
            int port=proxy.start(0);
            try{client(port,server.getLocalPort(),"untrusted");}catch(Exception expected){}
            long deadline=System.currentTimeMillis()+5000;
            while((proxy.flows().isEmpty()||!proxy.flows().get(0).status.equals("FAILED"))&&System.currentTimeMillis()<deadline)Thread.sleep(20);
            assertFalse(proxy.flows().isEmpty());assertEquals("FAILED",proxy.flows().get(0).status);
            assertTrue(proxy.flows().get(0).error,proxy.flows().get(0).error.contains("SSLHandshakeException"));
            handshake.get(10,TimeUnit.SECONDS);
        }finally{threads.shutdownNow();}
    }
}
