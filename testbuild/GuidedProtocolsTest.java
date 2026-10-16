import com.nexvary.trafficlab.core.*;
import java.io.*;
import java.nio.*;
import java.util.*;
public class GuidedProtocolsTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static void bad(byte[] bytes,boolean client)throws Exception{try{WebSocketWire.read(new ByteArrayInputStream(bytes),client);throw new AssertionError();}catch(IOException expected){}}
 public static void main(String[] args)throws Exception{
  LabTarget target=LabTarget.parse("example.com:8443/path?q=1");check(target.host().equals("example.com")&&target.port()==8443&&target.path().equals("/path?q=1"));
  for(String value:new String[]{"http://example.com","https://a:b@example.com","https://example.com/#x","https://example.com:99999","https://"})try{LabTarget.parse(value);throw new AssertionError(value);}catch(IllegalArgumentException expected){}
  check(WebSocketWire.accept("dGhlIHNhbXBsZSBub25jZQ==").equals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo="));
  WebSocketWire.Frame frame=WebSocketWire.read(new ByteArrayInputStream(WebSocketFrameCodec.encodeClientText("hello")),true);check(new String(frame.payload(),"UTF-8").equals("hello"));
  byte[] large=new byte[130];large[0]=(byte)0x82;large[1]=126;large[2]=0;large[3]=126;check(WebSocketWire.read(new ByteArrayInputStream(large),false).payload().length==126);
  bad(new byte[]{(byte)0x81,1,1},true);bad(new byte[]{(byte)0xc1,0},false);bad(new byte[]{9,0},false);bad(new byte[]{(byte)0x88,1,0},false);bad(new byte[]{(byte)0x82,126,0,1,1},false);
  WebSocketWire.Messages messages=new WebSocketWire.Messages();check(messages.accept(new WebSocketWire.Frame(false,1,new byte[]{(byte)0xe2},null))==null);check(messages.accept(new WebSocketWire.Frame(true,9,new byte[0],null)).equals("PING"));check(messages.accept(new WebSocketWire.Frame(true,0,new byte[]{(byte)0x82,(byte)0xac},null)).contains("€"));
  byte[] quic=new byte[26];ByteBuffer b=ByteBuffer.wrap(quic);b.put((byte)0xc0).putInt(1).put((byte)0).put((byte)0).put((byte)0).put((byte)17);QuicMetadata.Header h=QuicMetadata.parse(quic);check(h!=null&&h.type().equals("INITIAL")&&h.version().equals("QUIC v1"));
  quic[0]=(byte)0xd0;ByteBuffer.wrap(quic,1,4).putInt(0x6b3343cf);check(QuicMetadata.parse(quic).type().equals("INITIAL"));check(QuicMetadata.parse(new byte[]{0})==null);check(QuicMetadata.parse(Arrays.copyOf(quic,10))==null);
  System.out.println("GUIDED_PROTOCOLS_OK HTTPS target validation, RFC6455 framing, fragmentation, QUIC v1/v2 metadata");
 }
}
