package com.nexvary.trafficlab;
import android.test.InstrumentationTestCase;
import com.nexvary.trafficlab.tls.*;
import com.nexvary.trafficlab.core.*;
import io.netty.buffer.*;
import io.netty.handler.codec.http2.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

public final class ProtocolIntegrationTest extends InstrumentationTestCase {
 private LabCertificateAuthority ca;private SSLContext trust;
 @Override protected void setUp()throws Exception{super.setUp();ca=LabCertificateAuthority.open(getInstrumentation().getTargetContext());KeyStore store=KeyStore.getInstance(KeyStore.getDefaultType());store.load(null);store.setCertificateEntry("lab",ca.certificate());TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(store);trust=SSLContext.getInstance("TLS");trust.init(null,tm.getTrustManagers(),null);}
 private static String head(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();int tail=0;while(out.size()<32768){int b=in.read();if(b<0)throw new EOFException();out.write(b);tail=(tail<<8)|b;if(tail==0x0d0a0d0a)return out.toString("ISO-8859-1");}throw new IOException();}
 private SSLSocket client(int proxy,int target,String protocol)throws Exception{Socket tcp=new Socket("127.0.0.1",proxy);tcp.setSoTimeout(15000);tcp.getOutputStream().write(("CONNECT 127.0.0.1:"+target+" HTTP/1.1\r\n\r\n").getBytes(StandardCharsets.US_ASCII));assertTrue(head(tcp.getInputStream()).contains("200"));SSLSocket socket=(SSLSocket)trust.getSocketFactory().createSocket(tcp,"127.0.0.1",target,true);SSLParameters p=socket.getSSLParameters();p.setEndpointIdentificationAlgorithm("HTTPS");p.setApplicationProtocols(new String[]{protocol});socket.setSSLParameters(p);socket.setSoTimeout(15000);socket.startHandshake();assertEquals(protocol,socket.getApplicationProtocol());return socket;}
 private SSLServerSocket server()throws Exception{SSLServerSocket s=(SSLServerSocket)ca.serverContext("127.0.0.1").getServerSocketFactory().createServerSocket(0,10,InetAddress.getByName("127.0.0.1"));s.setSoTimeout(15000);return s;}
 public void testRealWebSocketUpgradeTextControlAndClose()throws Exception{
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try(SSLServerSocket server=server();EmbeddedTlsProxy proxy=new EmbeddedTlsProxy(ca,"127.0.0.1",server.getLocalPort(),false,trust)){
   Future<?> upstream=pool.submit(()->{try(SSLSocket socket=(SSLSocket)server.accept()){
    socket.setSoTimeout(10000);String request=head(socket.getInputStream());assertNull(WebSocketWire.value(request,"Sec-WebSocket-Extensions"));
    socket.getOutputStream().write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "+WebSocketWire.accept(WebSocketWire.value(request,"Sec-WebSocket-Key"))+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();
    WebSocketWire.Frame text=WebSocketWire.read(socket.getInputStream(),true);assertEquals("hello",new String(text.payload(),StandardCharsets.UTF_8));socket.getOutputStream().write(new byte[]{(byte)0x81,5,'h','e','l','l','o'});socket.getOutputStream().flush();
    WebSocketWire.Frame ping=WebSocketWire.read(socket.getInputStream(),true);assertEquals(9,ping.opcode());socket.getOutputStream().write(new byte[]{(byte)0x8a,0});socket.getOutputStream().flush();
    assertEquals(8,WebSocketWire.read(socket.getInputStream(),true).opcode());socket.getOutputStream().write(new byte[]{(byte)0x88,2,3,(byte)0xe8});socket.getOutputStream().flush();
   }catch(Exception e){throw new RuntimeException(e);}});
   int port=proxy.start(0);
   try(SSLSocket socket=client(port,server.getLocalPort(),"http/1.1")){
    socket.getOutputStream().write("GET /ws HTTP/1.1\r\nHost: lab\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Version: 13\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Extensions: permessage-deflate\r\n\r\n".getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();assertTrue(head(socket.getInputStream()).startsWith("HTTP/1.1 101"));
    socket.getOutputStream().write(WebSocketFrameCodec.encodeClientText("hello"));socket.getOutputStream().flush();assertEquals("hello",new String(WebSocketWire.read(socket.getInputStream(),false).payload(),StandardCharsets.UTF_8));
    socket.getOutputStream().write(new byte[]{(byte)0x89,(byte)0x80,1,2,3,4});socket.getOutputStream().flush();assertEquals(10,WebSocketWire.read(socket.getInputStream(),false).opcode());
    socket.getOutputStream().write(new byte[]{(byte)0x88,(byte)0x82,0,0,0,0,3,(byte)0xe8});socket.getOutputStream().flush();assertEquals(8,WebSocketWire.read(socket.getInputStream(),false).opcode());
   }
   upstream.get(10,TimeUnit.SECONDS);long until=System.currentTimeMillis()+5000;while(!proxy.flows().get(0).status.equals("COMPLETE")&&System.currentTimeMillis()<until)Thread.sleep(20);
   EmbeddedTlsProxy.Flow flow=proxy.flows().get(0);assertEquals(flow.error,"COMPLETE",flow.status);assertEquals("WebSocket",flow.protocol);String preview=String.join("\n",flow.messages());assertTrue(preview,preview.contains("TEXT hello")&&preview.contains("PING")&&preview.contains("PONG"));
  }finally{pool.shutdownNow();}
 }
 private static void frame(OutputStream out,int type,int flags,int stream,byte[] data)throws Exception{out.write(new byte[]{(byte)(data.length>>>16),(byte)(data.length>>>8),(byte)data.length,(byte)type,(byte)flags,(byte)(stream>>>24),(byte)(stream>>>16),(byte)(stream>>>8),(byte)stream});out.write(data);out.flush();}
 private static void request(OutputStream out,DefaultHttp2HeadersEncoder encoder,int id,int port,String body)throws Exception{
  Http2Headers headers=new DefaultHttp2Headers().method("POST").scheme("https").authority("127.0.0.1:"+port).path("/h2/"+id);headers.add("content-type","text/plain");headers.addInt("content-length",body.length());ByteBuf block=Unpooled.buffer();try{encoder.encodeHeaders(id,headers,block);byte[] bytes=new byte[block.readableBytes()];block.readBytes(bytes);frame(out,1,4,id,bytes);}finally{block.release();}frame(out,0,1,id,body.getBytes(StandardCharsets.US_ASCII));
 }
 public void testRealHttp2AlpnMultiplexEditAndResponseFlowControl()throws Exception{
  ExecutorService pool=Executors.newFixedThreadPool(4);
  try(SSLServerSocket server=server();EmbeddedTlsProxy proxy=new EmbeddedTlsProxy(ca,"127.0.0.1",server.getLocalPort(),true,trust)){
   Future<?> upstream=pool.submit(()->{try{for(int n=0;n<2;n++){SSLSocket socket=(SSLSocket)server.accept();pool.submit(()->{try(socket){socket.setSoTimeout(10000);String request=head(socket.getInputStream());byte[] body=Http1Body.read(request,socket.getInputStream(),1024);byte[] response=new byte[80000];Arrays.fill(response,(byte)'x');byte[] prefix=("accepted:"+new String(body,StandardCharsets.UTF_8)+"\n").getBytes(StandardCharsets.UTF_8);System.arraycopy(prefix,0,response,0,prefix.length);socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "+response.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().write(response);socket.getOutputStream().flush();}catch(Exception e){throw new RuntimeException(e);}});}}catch(Exception e){throw new RuntimeException(e);}});
   int port=proxy.start(0);Future<Map<Integer,String>> response=pool.submit(()->{
    try(SSLSocket socket=client(port,server.getLocalPort(),"h2")){
     OutputStream out=socket.getOutputStream();out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII));frame(out,4,0,0,new byte[0]);DefaultHttp2HeadersEncoder encoder=new DefaultHttp2HeadersEncoder();request(out,encoder,1,server.getLocalPort(),"price=100");request(out,encoder,3,server.getLocalPort(),"price=2");
     Map<Integer,ByteArrayOutputStream> bodies=new HashMap<>();Set<Integer> ended=new HashSet<>();DefaultHttp2HeadersDecoder decoder=new DefaultHttp2HeadersDecoder();DataInputStream in=new DataInputStream(socket.getInputStream());
     while(ended.size()<2){int length=(in.readUnsignedByte()<<16)|(in.readUnsignedByte()<<8)|in.readUnsignedByte(),type=in.readUnsignedByte(),flags=in.readUnsignedByte(),stream=in.readInt()&0x7fffffff;assertTrue(length<=16384);byte[] payload=new byte[length];in.readFully(payload);
      if(type==4&&(flags&1)==0)frame(out,4,1,0,new byte[0]);
      if(type==1){ByteBuf block=Unpooled.wrappedBuffer(payload);try{assertEquals("200",decoder.decodeHeaders(stream,block).status().toString());}finally{block.release();}}
      if(type==0){bodies.computeIfAbsent(stream,k->new ByteArrayOutputStream()).write(payload);byte[] update=java.nio.ByteBuffer.allocate(4).putInt(length).array();frame(out,8,0,stream,update);frame(out,8,0,0,update);}
      if((type==0||type==1)&&(flags&1)!=0)ended.add(stream);if(type==3||type==7)throw new AssertionError("HTTP/2 reset/GOAWAY "+Arrays.toString(payload));
     }
     Map<Integer,String> result=new HashMap<>();for(Map.Entry<Integer,ByteArrayOutputStream> entry:bodies.entrySet()){assertEquals(80000,entry.getValue().size());result.put(entry.getKey(),entry.getValue().toString("UTF-8"));}return result;
    }
   });
   Set<Long> handled=new HashSet<>();long until=System.currentTimeMillis()+10000;
   while(handled.size()<2&&System.currentTimeMillis()<until){for(EmbeddedTlsProxy.Flow flow:proxy.flows())if(flow.status.equals("PAUSED")&&handled.add(flow.id)){assertEquals("HTTP/2",flow.protocol);proxy.decide(flow.id,flow.request.replace("price=100","price=1"),false);}Thread.sleep(10);}
   assertEquals("Two HTTP/2 streams did not pause: "+proxy.flows().get(0).error,2,handled.size());Map<Integer,String> replies=response.get(20,TimeUnit.SECONDS);assertTrue(replies.get(1).startsWith("accepted:price=1\n"));assertTrue(replies.get(3).startsWith("accepted:price=2\n"));upstream.get(5,TimeUnit.SECONDS);
  }finally{pool.shutdownNow();}
 }
}
