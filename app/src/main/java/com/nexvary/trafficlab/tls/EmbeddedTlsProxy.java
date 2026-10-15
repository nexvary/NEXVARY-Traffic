package com.nexvary.trafficlab.tls;

import javax.net.ssl.*;
import com.nexvary.trafficlab.core.CapturedTraffic;
import com.nexvary.trafficlab.core.Http1Body;
import com.nexvary.trafficlab.core.ResponseEditor;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Explicit, on-device HTTP/1.1 TLS proxy. Upstream TLS and hostname verification stay enabled. */
public final class EmbeddedTlsProxy implements AutoCloseable {
    public static final int MAX_BODY = 1024 * 1024, MAX_SAMPLE = 65536;
    public static final class Flow {
        public final long id; public final String host;
        private long forwardingStarted,upstreamElapsed;
        public volatile String responseTemplate="", responseEditError="";
        private final CountDownLatch responseDecision=new CountDownLatch(1);
        private volatile String responseEdited;
        private volatile boolean responseDropped;
        private String sent=""; private boolean responseTruncated;
        public volatile String request = "", response = "", status = "CONNECTING", error = "";
        private final CountDownLatch decision = new CountDownLatch(1);
        private volatile boolean dropped;
        private volatile String edited;
        Flow(long id, String host) { this.id=id; this.host=host; }
    }
    private final LabCertificateAuthority ca;
    private final SSLContext upstream;
    private final String allowedHost;
    private final int allowedPort;
    private final boolean pause,pauseResponse;
    private final List<Flow> flows = new ArrayList<>();
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    private final AtomicLong sequence = new AtomicLong();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(4,4,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8));
    private volatile ServerSocket listener;
    private volatile boolean running;
    private long captureGeneration;
    public EmbeddedTlsProxy(LabCertificateAuthority ca, String host, int port, boolean pause) throws Exception {
        this(ca, host, port, pause, SSLContext.getDefault());
    }
    public EmbeddedTlsProxy(LabCertificateAuthority ca, String host, int port, boolean pause, SSLContext upstream) {
        this(ca,host,port,pause,false,upstream);
    }
    public EmbeddedTlsProxy(LabCertificateAuthority ca,String host,int port,boolean pause,boolean pauseResponse,SSLContext upstream) {
        if (host == null || !host.matches("[A-Za-z0-9][A-Za-z0-9.-]{0,252}") || port < 1 || port > 65535)
            throw new IllegalArgumentException("Invalid lab host or port");
        this.ca=ca; this.allowedHost=host.toLowerCase(Locale.ROOT); this.allowedPort=port; this.pause=pause; this.pauseResponse=pauseResponse; this.upstream=upstream;
    }
    public synchronized int start(int port) throws IOException {
        if (running) return listener.getLocalPort();
        listener = new ServerSocket(); listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),port));
        captureGeneration=CapturedTraffic.openSession();
        running=true;
        Thread accept = new Thread(() -> {
            while (running) {
                try {
                    Socket socket=listener.accept(); socket.setSoTimeout(15000); sockets.add(socket);
                    try { workers.execute(() -> handle(socket)); }
                    catch (RejectedExecutionException full) { sockets.remove(socket); try {socket.close();} catch(IOException ignored){} }
                } catch (IOException closed) { if (running) close(); }
            }
        }, "nexvary-embedded-https"); accept.setDaemon(true); accept.start();
        return listener.getLocalPort();
    }
    public boolean isRunning() { return running; }
    public synchronized List<Flow> flows() { return new ArrayList<>(flows); }
    private synchronized Flow newFlow() {
        Flow flow = new Flow(sequence.incrementAndGet(), allowedHost);
        // Keep pending requests, bounded by the four active worker slots.
        while (flows.size() >= 32) {
            int index=0; while(index<flows.size() && !java.util.Arrays.asList("COMPLETE","FAILED","DROPPED").contains(flows.get(index).status)) index++;
            if(index == flows.size()) break;
            flows.remove(index);
        }
        flows.add(flow); return flow;
    }
    public void decide(long id, String edited, boolean drop) {
        for (Flow flow : flows()) if(flow.id == id && flow.status.equals("PAUSED")) {
            if (edited != null && edited.getBytes(StandardCharsets.UTF_8).length > MAX_BODY) throw new IllegalArgumentException("Edited request too large");
            synchronized(flow) {
                if(flow.decision.getCount()==0) throw new IllegalStateException("Request already decided");
                flow.edited=edited; flow.dropped=drop; flow.status="QUEUED"; flow.decision.countDown();
            }
            return;
        }
        throw new IllegalStateException("Request is no longer paused");
    }
    public void decideResponse(long id,String edited,boolean drop) throws IOException {
        for(Flow flow:flows())if(flow.id==id){synchronized(flow){
            if(!flow.status.equals("RESPONSE_PAUSED")||flow.responseDecision.getCount()==0)throw new IllegalStateException("Response is no longer paused");
            if(edited!=null){if(flow.responseTemplate.isEmpty())throw new IllegalStateException(flow.responseEditError);ResponseEditor.normalize(edited);}
            flow.responseEdited=edited;flow.responseDropped=drop;flow.status="RESPONSE_QUEUED";flow.responseDecision.countDown();return;
        }}throw new IllegalStateException("Response is no longer available");
    }
    private void handle(Socket client) {
        Flow flow = null;
        try (client) {
            String connect = headers(client.getInputStream());
            String[] first=connect.split("\\r\\n",2)[0].split(" ");
            String authority=allowedHost+":"+allowedPort;
            if(first.length != 3 || !first[0].equals("CONNECT") || !first[1].equalsIgnoreCase(authority)) {
                reply(client,403,"Destination is outside this lab session"); return;
            }
            flow=newFlow();
            client.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII)); client.getOutputStream().flush();
            try (SSLSocket decrypted=(SSLSocket)ca.serverContext(allowedHost).getSocketFactory().createSocket(client, allowedHost, allowedPort, false)) {
                decrypted.setUseClientMode(false); decrypted.setSoTimeout(15000); decrypted.startHandshake();
                String head=headers(decrypted.getInputStream());
                if(head.toLowerCase(Locale.ROOT).contains("\r\nexpect:")) throw new IOException("Expect negotiation not supported yet");
                byte[] body=Http1Body.read(head,decrypted.getInputStream(),MAX_BODY);
                byte[] request=normalize(head,body);
                flow.request=head.toLowerCase(Locale.ROOT).contains("transfer-encoding:")?new String(request,StandardCharsets.UTF_8):head+new String(body,StandardCharsets.UTF_8);
                if(pause) {
                    flow.status="PAUSED";
                    if(!flow.decision.await(120,TimeUnit.SECONDS)) throw new IOException("Paused request expired");
                    if(flow.dropped) { flow.status="DROPPED"; reply(decrypted,403,"Dropped in NEXVARY Lab"); return; }
                    if(flow.edited != null) {
                        int boundary=flow.edited.indexOf("\r\n\r\n");
                        if(boundary<0) throw new IOException("Expected CRLF header separator");
                        request=normalize(flow.edited.substring(0,boundary+4),flow.edited.substring(boundary+4).getBytes(StandardCharsets.UTF_8));
                    }
                }
                flow.sent=new String(request,StandardCharsets.UTF_8);
                flow.forwardingStarted=System.nanoTime();
                flow.status="FORWARDING";
                try (Socket tcp=new Socket()) {
                    sockets.add(tcp);
                    try {
                        tcp.connect(new InetSocketAddress(allowedHost,allowedPort),5000);
                        try(SSLSocket remote=(SSLSocket)upstream.getSocketFactory().createSocket(tcp,allowedHost,allowedPort,true)) {
                            remote.setSoTimeout(15000);
                            SSLParameters parameters=remote.getSSLParameters(); parameters.setEndpointIdentificationAlgorithm("HTTPS"); remote.setSSLParameters(parameters);
                            remote.startHandshake(); remote.getOutputStream().write(request); remote.getOutputStream().flush();
                            ByteArrayOutputStream sample=new ByteArrayOutputStream(); byte[] chunk=new byte[8192]; int count; long total=0;
                            while((count=remote.getInputStream().read(chunk))!=-1) {
                                total+=count; if(total > 16L*1024*1024) throw new IOException("Response exceeds session limit");
                                if(!pauseResponse)decrypted.getOutputStream().write(chunk,0,count);
                                int sampleLimit=pauseResponse?MAX_BODY:MAX_SAMPLE;
                                if(pauseResponse && total>MAX_BODY)throw new IOException("Paused response exceeds 1 MiB; disable response pause to stream larger responses");
                                if(sample.size()<sampleLimit) sample.write(chunk,0,Math.min(count,sampleLimit-sample.size()));
                            }
                            flow.upstreamElapsed=(System.nanoTime()-flow.forwardingStarted)/1000000L;
                            byte[] received=sample.toByteArray();
                            if(pauseResponse){
                                try{flow.responseTemplate=ResponseEditor.template(received);}catch(IOException unsupported){flow.responseEditError=unsupported.getMessage();}
                                flow.response=new String(received,StandardCharsets.UTF_8);flow.status="RESPONSE_PAUSED";
                                if(!flow.responseDecision.await(120,TimeUnit.SECONDS))throw new IOException("Paused response expired");
                                if(flow.responseDropped){flow.status="DROPPED";reply(decrypted,403,"Response dropped in NEXVARY Lab");return;}
                                if(flow.responseEdited!=null)received=ResponseEditor.normalize(flow.responseEdited);
                                decrypted.getOutputStream().write(received);
                            }
                            decrypted.getOutputStream().flush();flow.responseTruncated=total>MAX_SAMPLE||received.length>MAX_SAMPLE;
                            flow.response=new String(received,0,Math.min(received.length,MAX_SAMPLE),StandardCharsets.UTF_8);flow.status="COMPLETE";
                        }
                    } finally { sockets.remove(tcp); }
                }
            }
        } catch(Exception error) {
            if(flow!=null) { flow.error=error.getClass().getSimpleName()+": "+error.getMessage(); flow.status="FAILED"; }
        } finally {
            if(flow!=null && flow.status.equals("COMPLETE"))CapturedTraffic.completed(captureGeneration,flow.request,flow.sent,flow.response,flow.upstreamElapsed,flow.responseTruncated);
            if(flow!=null){
                if(flow.request.length()>MAX_SAMPLE)flow.request=flow.request.substring(0,MAX_SAMPLE);
                flow.sent="";flow.edited=null;flow.responseEdited=null;flow.responseTemplate="";
            }
            sockets.remove(client);
        }
    }
    private byte[] normalize(String head, byte[] body) throws IOException {
        if(body.length > MAX_BODY) throw new IOException("Request too large");
        String[] lines=head.split("\r\n"); String[] first=lines[0].split(" ");
        if(first.length!=3 || !first[0].matches("[A-Z]+") || !first[1].startsWith("/") || !first[2].equals("HTTP/1.1")) throw new IOException("HTTP/1.1 origin-form request required");
        StringBuilder normalized=new StringBuilder(lines[0]).append("\r\nHost: ").append(allowedHost).append(':').append(allowedPort).append("\r\n");
        for(int i=1;i<lines.length;i++) {
            if(lines[i].isEmpty()) continue;
            int colon=lines[i].indexOf(':'); if(colon<1) throw new IOException("Malformed header");
            String name=lines[i].substring(0,colon);
            if(!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) throw new IOException("Invalid header name");
            if(name.equalsIgnoreCase("Upgrade")) throw new IOException("Protocol upgrades are not supported yet");
            if(!name.equalsIgnoreCase("Transfer-Encoding") && !name.equalsIgnoreCase("Trailer") && !name.equalsIgnoreCase("Host") && !name.equalsIgnoreCase("Content-Length") && !name.equalsIgnoreCase("Connection") && !name.equalsIgnoreCase("Proxy-Connection")) normalized.append(lines[i]).append("\r\n");
        }
        normalized.append("Content-Length: ").append(body.length).append("\r\nConnection: close\r\n\r\n");
        ByteArrayOutputStream out=new ByteArrayOutputStream(); out.write(normalized.toString().getBytes(StandardCharsets.ISO_8859_1)); out.write(body); return out.toByteArray();
    }
    private static String headers(InputStream in) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); int last=0;
        while(bytes.size()<32768) {
            int next=in.read(); if(next<0) throw new EOFException("Incomplete HTTP headers"); bytes.write(next);
            last=(last<<8)|next; if(last==0x0d0a0d0a) return new String(bytes.toByteArray(),StandardCharsets.ISO_8859_1);
        }
        throw new IOException("Headers exceed 32 KiB");
    }
    private static void reply(Socket socket,int status,String message) throws IOException {
        byte[] body=message.getBytes(StandardCharsets.UTF_8);
        socket.getOutputStream().write(("HTTP/1.1 "+status+" Lab\r\nContent-Length: "+body.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII)); socket.getOutputStream().write(body); socket.getOutputStream().flush();
    }
    @Override public synchronized void close() {
        running=false;
        for(Flow flow:flows) {flow.dropped=true; flow.responseDropped=true; flow.decision.countDown();flow.responseDecision.countDown();}
        try {if(listener!=null) listener.close();}catch(IOException ignored){}
        for(Socket socket:sockets) try{socket.close();}catch(IOException ignored){}
        sockets.clear(); workers.shutdownNow();
    }
}
