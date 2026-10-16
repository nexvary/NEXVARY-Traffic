package com.nexvary.trafficlab.tls;
import com.nexvary.trafficlab.core.LabTarget;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

/** Single user-initiated GET through the running local proxy, never a background scan. */
public final class LabProbe {
    private static volatile Socket active;
    public static volatile String state="IDLE",result="";
    private static long generation;
    private LabProbe(){}
    public static synchronized void start(LabTarget target,LabCertificateAuthority ca)throws Exception{
        if(active!=null||state.equals("RUNNING"))throw new IOException("A test is already running");
        KeyStore trust=KeyStore.getInstance(KeyStore.getDefaultType());trust.load(null);trust.setCertificateEntry("lab",ca.certificate());
        TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(trust);
        SSLContext ssl=SSLContext.getInstance("TLS");ssl.init(null,tm.getTrustManagers(),null);
        state="RUNNING";result="";long expected=++generation;
        Thread worker=new Thread(()->{
            String next="COMPLETE",summary;
            try(Socket tcp=new Socket()){
                synchronized(LabProbe.class){if(expected!=generation)return;active=tcp;}
                tcp.connect(new InetSocketAddress("127.0.0.1",8888),3000);tcp.setSoTimeout(300000);
                tcp.getOutputStream().write(("CONNECT "+target.host()+":"+target.port()+" HTTP/1.1\r\nHost: "+target.host()+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                if(!header(tcp.getInputStream()).startsWith("HTTP/1.1 200"))throw new IOException("Proxy rejected the selected destination");
                try(SSLSocket tls=(SSLSocket)ssl.getSocketFactory().createSocket(tcp,target.host(),target.port(),false)){
                    SSLParameters p=tls.getSSLParameters();p.setEndpointIdentificationAlgorithm("HTTPS");p.setApplicationProtocols(new String[]{"http/1.1"});tls.setSSLParameters(p);tls.setSoTimeout(300000);tls.startHandshake();
                    tls.getOutputStream().write(("GET "+target.path()+" HTTP/1.1\r\nHost: "+target.host()+":"+target.port()+"\r\nAccept: */*\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));tls.getOutputStream().flush();
                    ByteArrayOutputStream preview=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int n;long total=0;
                    while((n=tls.getInputStream().read(chunk))!=-1){total+=n;if(total>16L*1024*1024)throw new IOException("Response exceeds 16 MiB");if(preview.size()<32768)preview.write(chunk,0,Math.min(n,32768-preview.size()));}
                    summary=new String(preview.toByteArray(),StandardCharsets.UTF_8);if(summary.isEmpty())throw new IOException("Server returned no response");
                }
            }catch(Exception error){next="ERROR";summary=error.getClass().getSimpleName()+": "+error.getMessage();}
            synchronized(LabProbe.class){if(expected==generation){state=next;result=summary;active=null;}}
        },"nexvary-guided-probe");worker.setDaemon(true);worker.start();
    }
    private static String header(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();int tail=0;while(out.size()<32768){int b=in.read();if(b<0)throw new EOFException();out.write(b);tail=(tail<<8)|b;if(tail==0x0d0a0d0a)return out.toString("ISO-8859-1");}throw new IOException("Header limit");}
    public static synchronized void stop(){generation++;try{if(active!=null)active.close();}catch(IOException ignored){}active=null;state="IDLE";result="";}
}
