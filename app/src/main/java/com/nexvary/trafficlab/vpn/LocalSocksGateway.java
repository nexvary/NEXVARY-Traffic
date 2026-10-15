package com.nexvary.trafficlab.vpn;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Authenticated, loopback-only SOCKS5 gateway for the in-process TUN engine. */
public final class LocalSocksGateway implements AutoCloseable {
    public interface Protector {
        boolean protect(Socket socket);
        boolean protect(DatagramSocket socket);
    }
    public interface Resolver { InetAddress resolve(String host) throws UnknownHostException; }
    private final Protector protector;
    private final Resolver resolver;
    private final String inspectHost;
    private final int inspectPort, proxyPort;
    private final Supplier<Boolean> inspectEnabled;
    private final String username="nexvary", password;
    private final Set<Closeable> resources=ConcurrentHashMap.newKeySet();
    private final Semaphore slots=new Semaphore(24);
    private final ThreadPoolExecutor workers=new ThreadPoolExecutor(0,80,30,TimeUnit.SECONDS,new SynchronousQueue<>());
    private volatile boolean running;
    public volatile String lastError="";
    private ServerSocket server;
    public LocalSocksGateway(Protector protector,String host,int port,int proxyPort,Supplier<Boolean> enabled) {
        this(protector,host,port,proxyPort,enabled,InetAddress::getByName);
    }
    public LocalSocksGateway(Protector protector,String host,int port,int proxyPort,Supplier<Boolean> enabled,Resolver resolver) {
        this.resolver=resolver;this.protector=protector;inspectHost=host;inspectPort=port;this.proxyPort=proxyPort;inspectEnabled=enabled;
        byte[] secret=new byte[24];new SecureRandom().nextBytes(secret);
        password=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }
    public String username(){return username;}
    public String password(){return password;}
    public synchronized int start() throws IOException {
        if(running)return server.getLocalPort();
        server=new ServerSocket(0,16,InetAddress.getByName("127.0.0.1"));resources.add(server);running=true;
        Thread accept=new Thread(()->{
            while(running)try {
                Socket client=server.accept();client.setSoTimeout(15000);
                if(!slots.tryAcquire()){client.close();continue;}
                resources.add(client);
                try{workers.execute(()->{try{handle(client);}catch(Exception error){lastError=error.getClass().getSimpleName()+": "+error.getMessage();}finally{closeResource(client);slots.release();}});}
                catch(RejectedExecutionException full){closeResource(client);slots.release();}
            }catch(IOException closed){break;}
        },"nexvary-socks-accept");accept.setDaemon(true);accept.start();return server.getLocalPort();
    }
    private static String field(DataInputStream in)throws IOException {
        int n=in.readUnsignedByte();if(n==0)throw new IOException("Empty authentication field");
        byte[] b=new byte[n];in.readFully(b);return new String(b,StandardCharsets.US_ASCII);
    }
    private boolean authenticate(DataInputStream in,OutputStream out)throws IOException {
        if(in.readUnsignedByte()!=5)return false;
        int count=in.readUnsignedByte();boolean auth=false;
        for(int i=0;i<count;i++)if(in.readUnsignedByte()==2)auth=true;
        out.write(new byte[]{5,(byte)(auth?2:255)});out.flush();if(!auth)return false;
        boolean version=in.readUnsignedByte()==1;
        String user=field(in),pass=field(in);
        boolean ok=version && username.equals(user) && java.security.MessageDigest.isEqual(password.getBytes(StandardCharsets.US_ASCII),pass.getBytes(StandardCharsets.US_ASCII));
        out.write(new byte[]{1,(byte)(ok?0:1)});out.flush();return ok;
    }
    private static String address(DataInputStream in,int type)throws IOException {
        byte[] bytes;
        if(type==1||type==4){bytes=new byte[type==1?4:16];in.readFully(bytes);return InetAddress.getByAddress(bytes).getHostAddress();}
        if(type==3){int n=in.readUnsignedByte();if(n==0)throw new IOException("Empty domain");bytes=new byte[n];in.readFully(bytes);String host=new String(bytes,StandardCharsets.US_ASCII);if(!host.matches("[A-Za-z0-9_.-]+"))throw new IOException("Invalid domain");return host;}
        throw new IOException("Unsupported SOCKS address");
    }
    private static void result(OutputStream out,int status,int port)throws IOException {
        out.write(new byte[]{5,(byte)status,0,1,127,0,0,1,(byte)(port>>>8),(byte)port});out.flush();
    }
    private void handle(Socket client)throws Exception {
        DataInputStream in=new DataInputStream(client.getInputStream());OutputStream out=client.getOutputStream();
        if(!authenticate(in,out))return;
        if(in.readUnsignedByte()!=5)return;
        int command=in.readUnsignedByte();if(in.readUnsignedByte()!=0)return;
        String host=address(in,in.readUnsignedByte());int port=in.readUnsignedShort();
        if(command==3){udp(client,in,out);return;}
        if(command!=1){result(out,7,0);return;}
        Socket remote=java.nio.channels.SocketChannel.open().socket();resources.add(remote);
        boolean established=false;
        try(remote){
            if(!protector.protect(remote))throw new IOException("Socket protection failed");
            boolean inspect=inspectHost!=null && inspectHost.equalsIgnoreCase(host) && port==inspectPort && inspectEnabled.get();
            remote.connect(new InetSocketAddress(resolver.resolve(inspect?"127.0.0.1":host),inspect?proxyPort:port),5000);remote.setSoTimeout(180000);
            if(inspect){
                remote.getOutputStream().write(("CONNECT "+inspectHost+":"+inspectPort+" HTTP/1.1\r\nHost: "+inspectHost+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));remote.getOutputStream().flush();
                String header=readHeaders(remote.getInputStream());
                if(!header.startsWith("HTTP/1.1 200 "))throw new IOException("HTTPS engine rejected tunnel");
            }
            result(out,0,0);established=true;client.setSoTimeout(180000);
            Future<?> upload=workers.submit(()->{try{copy(in,remote.getOutputStream());remote.shutdownOutput();}catch(IOException error){closeResource(remote);}});
            try{copy(remote.getInputStream(),out);try{client.shutdownOutput();}catch(IOException ignored){}}
            finally{upload.cancel(true);}
        }catch(IOException error){lastError=error.getClass().getSimpleName()+": "+error.getMessage();try{if(!established)result(out,5,0);}catch(IOException ignored){}}
        finally{resources.remove(remote);}
    }
    private static String readHeaders(InputStream in)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();int state=0;
        while(bytes.size()<32768){int b=in.read();if(b<0)throw new EOFException();bytes.write(b);state=(state<<8)|b;if(state==0x0d0a0d0a)return bytes.toString("ISO-8859-1");}
        throw new IOException("Oversized proxy headers");
    }
    private static void copy(InputStream in,OutputStream out)throws IOException {
        byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){out.write(b,0,n);out.flush();}
    }
    private void udp(Socket client,DataInputStream control,OutputStream reply)throws Exception {
        DatagramSocket local=new DatagramSocket(new InetSocketAddress("127.0.0.1",0));
        DatagramSocket remote=new DatagramSocket(null);
        resources.add(local);resources.add(remote);
        try(local;remote){
            if(!protector.protect(remote))throw new IOException("UDP protection failed");
            remote.bind(new InetSocketAddress(0));remote.setSoTimeout(1000);local.setSoTimeout(1000);
            Set<InetSocketAddress> permitted=ConcurrentHashMap.newKeySet();
            java.util.concurrent.atomic.AtomicReference<InetSocketAddress> peer=new java.util.concurrent.atomic.AtomicReference<>();
            Future<?> outbound=workers.submit(()->{
                byte[] buffer=new byte[65507];
                while(running&&!local.isClosed())try {
                    DatagramPacket packet=new DatagramPacket(buffer,buffer.length);local.receive(packet);
                    if(!packet.getAddress().isLoopbackAddress())continue;
                    InetSocketAddress sender=new InetSocketAddress(packet.getAddress(),packet.getPort());
                    if(peer.get()!=null&&!peer.get().equals(sender))continue;
                    DataInputStream data=new DataInputStream(new ByteArrayInputStream(buffer,0,packet.getLength()));
                    if(data.readUnsignedShort()!=0||data.readUnsignedByte()!=0)continue;
                    String h=address(data,data.readUnsignedByte());int p=data.readUnsignedShort();
                    InetSocketAddress target=new InetSocketAddress(resolver.resolve(h),p);
                    if(!permitted.contains(target)&&permitted.size()>=64)continue;
                    peer.compareAndSet(null,sender);permitted.add(target);
                    byte[] payload=new byte[data.available()];data.readFully(payload);remote.send(new DatagramPacket(payload,payload.length,target));
                }catch(SocketTimeoutException retry){}catch(IOException invalid){if(local.isClosed())break;}
            });
            Future<?> inbound=workers.submit(()->{
                byte[] buffer=new byte[65507];
                while(running&&!remote.isClosed())try{
                    DatagramPacket packet=new DatagramPacket(buffer,buffer.length);remote.receive(packet);
                    InetSocketAddress from=new InetSocketAddress(packet.getAddress(),packet.getPort());
                    if(peer.get()==null||!permitted.contains(from))continue;
                    byte[] addr=packet.getAddress().getAddress();
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(bytes);
                    data.writeShort(0);data.writeByte(0);data.writeByte(addr.length==4?1:4);data.write(addr);data.writeShort(packet.getPort());data.write(buffer,0,packet.getLength());
                    byte[] encoded=bytes.toByteArray();if(encoded.length<=65507)local.send(new DatagramPacket(encoded,encoded.length,peer.get()));
                }catch(SocketTimeoutException retry){}catch(IOException invalid){if(remote.isClosed())break;}
            });
            try{result(reply,0,local.getLocalPort());client.setSoTimeout(0);while(control.read()!=-1){}}
            finally{local.close();remote.close();outbound.cancel(true);inbound.cancel(true);}
        }finally{resources.remove(local);resources.remove(remote);}
    }
    private void closeResource(Closeable resource){try{resource.close();}catch(IOException ignored){}resources.remove(resource);}
    @Override public synchronized void close(){running=false;for(Closeable resource:resources)try{resource.close();}catch(IOException ignored){}resources.clear();workers.shutdownNow();}
}
