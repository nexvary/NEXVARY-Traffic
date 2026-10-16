package com.nexvary.trafficlab.tls;

import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.*;
import io.netty.util.ReferenceCountUtil;
import com.nexvary.trafficlab.core.Http1Body;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** TLS h2 frontend with HPACK, multiplexing and flow control; verified HTTP/1.1 upstream adapter. */
final class Http2LabConnection {
    interface Exchange { byte[] run(String head,byte[] body)throws Exception; }
    private static final int LIMIT=1024*1024;
    private static final class Pending {
        final Http2FrameStream stream;final Http2Headers headers;final ByteArrayOutputStream body=new ByteArrayOutputStream();boolean dispatched;
        Pending(Http2HeadersFrame frame){stream=frame.stream();headers=frame.headers();}
    }
    private record Ready(Http2FrameStream stream,byte[] response){}
    static void run(Socket socket,String host,int port,Exchange exchange)throws Exception{
        Http2Settings settings=new Http2Settings().maxConcurrentStreams(4).maxHeaderListSize(32768).initialWindowSize(65535);
        EmbeddedChannel codec=new EmbeddedChannel(Http2FrameCodecBuilder.forServer().initialSettings(settings).build());
        ExecutorService workers=Executors.newFixedThreadPool(4,r->{Thread t=new Thread(r,"nexvary-h2-stream");t.setDaemon(true);return t;});
        Map<Integer,Pending> streams=new HashMap<>();ConcurrentLinkedQueue<Ready> ready=new ConcurrentLinkedQueue<>();
        int requests=0;socket.setSoTimeout(200);long last=System.nanoTime();
        try{
            drain(codec,socket.getOutputStream());byte[] buffer=new byte[8192];
            while(!socket.isClosed()){
                Ready completed;while((completed=ready.poll())!=null){if(streams.remove(completed.stream().id())!=null)respond(codec,completed);last=System.nanoTime();}
                codec.runPendingTasks();drain(codec,socket.getOutputStream());
                int count;
                try{count=socket.getInputStream().read(buffer);}catch(SocketTimeoutException idle){if(System.nanoTime()-last>180_000_000_000L)break;continue;}
                if(count<0)break;last=System.nanoTime();codec.writeInbound(Unpooled.copiedBuffer(buffer,0,count));
                Object frame;
                while((frame=codec.readInbound())!=null){try{
                    if(frame instanceof Http2HeadersFrame headers){
                        int id=headers.stream().id();Pending pending=streams.get(id);
                        if(pending==null){if(streams.size()>=4||requests++>=32){codec.writeOutbound(new DefaultHttp2ResetFrame(Http2Error.REFUSED_STREAM).stream(headers.stream()));continue;}pending=new Pending(headers);streams.put(id,pending);}
                        else{if(pending.dispatched)throw new IOException("Headers after end stream");for(Map.Entry<CharSequence,CharSequence> h:headers.headers())if(h.getKey().charAt(0)==':')throw new IOException("Pseudo-header in trailers");}
                        if(headers.isEndStream())dispatch(pending,host,port,exchange,workers,ready);
                    }else if(frame instanceof Http2DataFrame data){
                        Pending pending=streams.get(data.stream().id());if(pending==null||pending.dispatched)throw new IOException("DATA outside an open stream");
                        if(pending.body.size()+data.content().readableBytes()>LIMIT)throw new IOException("HTTP/2 request exceeds 1 MiB");
                        byte[] bytes=new byte[data.content().readableBytes()];data.content().readBytes(bytes);pending.body.write(bytes);
                        if(data.initialFlowControlledBytes()>0)codec.writeOutbound(new DefaultHttp2WindowUpdateFrame(data.initialFlowControlledBytes()).stream(data.stream()));
                        if(data.isEndStream())dispatch(pending,host,port,exchange,workers,ready);
                    }else if(frame instanceof Http2ResetFrame reset)streams.remove(reset.stream().id());
                    else if(frame instanceof Http2GoAwayFrame)return;
                }finally{ReferenceCountUtil.release(frame);}}
            }
        }finally{workers.shutdownNow();codec.finishAndReleaseAll();}
    }
    private static void dispatch(Pending pending,String host,int port,Exchange exchange,ExecutorService workers,Queue<Ready> ready)throws IOException{
        if(pending.dispatched)throw new IOException("Duplicate END_STREAM");pending.dispatched=true;
        String method=String.valueOf(pending.headers.method()),path=String.valueOf(pending.headers.path()),authority=String.valueOf(pending.headers.authority());
        if(!"https".contentEquals(pending.headers.scheme()==null?"":pending.headers.scheme())||!method.matches("[A-Z]+")||method.equals("CONNECT")||!path.startsWith("/")||(!authority.equalsIgnoreCase(host+":"+port)&&!(port==443&&authority.equalsIgnoreCase(host))))throw new IOException("Invalid HTTP/2 origin or method");
        StringBuilder head=new StringBuilder(method+" "+path+" HTTP/1.1\r\nHost: "+host+":"+port+"\r\n");
        for(Map.Entry<CharSequence,CharSequence> h:pending.headers){String name=h.getKey().toString();if(name.startsWith(":"))continue;
            if(Arrays.asList("connection","upgrade","proxy-connection","keep-alive","transfer-encoding").contains(name))throw new IOException("Connection-specific HTTP/2 header");
            if(!name.equals("content-length")&&!name.equals("te"))head.append(name).append(": ").append(h.getValue()).append("\r\n");
        }
        if(pending.headers.getAll("content-length").size()>1)throw new IOException("Duplicate Content-Length");
        CharSequence declared=pending.headers.get("content-length");if(declared!=null&&!declared.toString().equals(String.valueOf(pending.body.size())))throw new IOException("HTTP/2 Content-Length mismatch");
        head.append("Content-Length: ").append(pending.body.size()).append("\r\n\r\n");
        workers.execute(()->{byte[] response;try{response=exchange.run(head.toString(),pending.body.toByteArray());}catch(Exception error){response="HTTP/1.1 502 Lab\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);}ready.add(new Ready(pending.stream,response));});
    }
    private static void respond(EmbeddedChannel codec,Ready ready)throws IOException{
        byte[] raw=ready.response();int boundary=-1;for(int i=0;i+3<raw.length;i++)if(raw[i]==13&&raw[i+1]==10&&raw[i+2]==13&&raw[i+3]==10){boundary=i+4;break;}
        if(boundary<0||boundary>32768)throw new IOException("Invalid HTTP/2 adapted response");
        String head=new String(raw,0,boundary,StandardCharsets.ISO_8859_1);String[] lines=head.split("\r\n");String[] status=lines[0].split(" ",3);if(status.length<2)throw new IOException("Invalid upstream status");
        Http2Headers headers=new DefaultHttp2Headers().status(status[1]);Set<String> hop=new HashSet<>(Arrays.asList("connection","keep-alive","proxy-connection","transfer-encoding","upgrade","trailer","content-length"));
        for(String line:lines){int c=line.indexOf(':');if(c>0&&line.substring(0,c).equalsIgnoreCase("Connection"))for(String token:line.substring(c+1).split(","))hop.add(token.trim().toLowerCase(Locale.ROOT));}
        for(String line:lines){int c=line.indexOf(':');if(c>0){String name=line.substring(0,c).toLowerCase(Locale.ROOT);if(!hop.contains(name))headers.add(name,line.substring(c+1).trim());}}
        ByteArrayInputStream input=new ByteArrayInputStream(raw,boundary,raw.length-boundary);byte[] body;
        if(status[1].equals("204")||status[1].equals("304")||status[1].startsWith("1"))body=new byte[0];
        else if(head.toLowerCase(Locale.ROOT).contains("\r\ncontent-length:")||head.toLowerCase(Locale.ROOT).contains("\r\ntransfer-encoding:"))body=Http1Body.read(head,input,LIMIT);
        else{body=new byte[input.available()];if(input.read(body)!=body.length&&body.length>0)throw new EOFException();}
        headers.setInt("content-length",body.length);codec.writeOutbound(new DefaultHttp2HeadersFrame(headers,body.length==0).stream(ready.stream()));
        for(int offset=0;offset<body.length;offset+=16384){int n=Math.min(16384,body.length-offset);codec.writeOutbound(new DefaultHttp2DataFrame(Unpooled.copiedBuffer(body,offset,n),offset+n==body.length).stream(ready.stream()));}
    }
    private static void drain(EmbeddedChannel codec,OutputStream output)throws IOException{Object value;while((value=codec.readOutbound())!=null){try{if(value instanceof ByteBuf bytes){byte[] data=new byte[bytes.readableBytes()];bytes.readBytes(data);output.write(data);}}finally{ReferenceCountUtil.release(value);}}output.flush();}
}
