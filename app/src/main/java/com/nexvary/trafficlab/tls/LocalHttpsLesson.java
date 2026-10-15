package com.nexvary.trafficlab.tls;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.*;

/** Offline lesson: loopback TLS API and client trust only this device's lab CA. */
public final class LocalHttpsLesson implements AutoCloseable {
    private final SSLContext trusted;
    private final SSLServerSocket server;
    private final ExecutorService threads=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"nexvary-local-https-lesson");t.setDaemon(true);return t;});
    private volatile Socket client,accepted;
    public volatile String result="READY";
    public LocalHttpsLesson(LabCertificateAuthority ca)throws Exception{
        KeyStore trust=KeyStore.getInstance(KeyStore.getDefaultType());trust.load(null);trust.setCertificateEntry("local-lesson",ca.certificate());
        TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(trust);
        trusted=SSLContext.getInstance("TLS");trusted.init(null,tm.getTrustManagers(),null);
        server=(SSLServerSocket)ca.serverContext("127.0.0.1").getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
    }
    public int port(){return server.getLocalPort();}
    public SSLContext trustedUpstream(){return trusted;}
    private static String header(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();int state=0;
        while(out.size()<32768){int b=in.read();if(b<0)throw new EOFException();out.write(b);state=(state<<8)|b;if(state==0x0d0a0d0a)return out.toString("ISO-8859-1");}throw new IOException("Lesson header limit");
    }
    public void begin(int proxyPort){
        result="WAITING_FOR_INSTRUCTOR";
        threads.execute(()->{try(SSLSocket socket=(SSLSocket)server.accept()){
            accepted=socket;socket.setSoTimeout(15000);String head=header(socket.getInputStream());
            byte[] body=com.nexvary.trafficlab.core.Http1Body.read(head,socket.getInputStream(),65536);
            // Deliberately vulnerable local training endpoint: echoes client-controlled data.
            byte[] response=("Local HTTPS lesson received: "+new String(body,StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: "+response.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().write(response);socket.getOutputStream().flush();
        }catch(Exception error){if(!server.isClosed())result="ERROR: "+error.getMessage();}finally{accepted=null;}});
        threads.execute(()->{try(Socket tcp=new Socket("127.0.0.1",proxyPort)){
            client=tcp;tcp.setSoTimeout(300000);
            tcp.getOutputStream().write(("CONNECT 127.0.0.1:"+port()+" HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes(StandardCharsets.US_ASCII));tcp.getOutputStream().flush();
            if(!header(tcp.getInputStream()).startsWith("HTTP/1.1 200"))throw new IOException("Lesson tunnel rejected");
            try(SSLSocket tls=(SSLSocket)trusted.getSocketFactory().createSocket(tcp,"127.0.0.1",port(),false)){
                SSLParameters parameters=tls.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");tls.setSSLParameters(parameters);tls.setSoTimeout(300000);tls.startHandshake();
                byte[] body="{\"price\":100,\"lesson\":\"offline-https\"}".getBytes(StandardCharsets.UTF_8);
                tls.getOutputStream().write(("POST /lesson HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: "+body.length+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));tls.getOutputStream().write(body);tls.getOutputStream().flush();
                ByteArrayOutputStream received=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;
                while((n=tls.getInputStream().read(buffer))!=-1){if(received.size()+n>65536)throw new IOException("Lesson response limit");received.write(buffer,0,n);}
                result=new String(received.toByteArray(),StandardCharsets.UTF_8);
            }
        }catch(Exception error){if(!server.isClosed())result="ERROR: "+error.getMessage();}finally{client=null;}});
    }
    @Override public void close(){try{server.close();}catch(IOException ignored){}for(Socket socket:new Socket[]{client,accepted})if(socket!=null)try{socket.close();}catch(IOException ignored){}threads.shutdownNow();}
}
