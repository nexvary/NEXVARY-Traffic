package com.nexvary.trafficlab.core;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/** RFC 6455 framing, exact wire forwarding and bounded decoded previews. No extensions. */
public final class WebSocketWire {
    public record Frame(boolean fin,int opcode,byte[] payload,byte[] wire){}
    public static String value(String head,String name)throws IOException{
        String value=null;for(String line:head.split("\r\n")){int colon=line.indexOf(':');if(colon>0&&line.substring(0,colon).equalsIgnoreCase(name)){if(value!=null)throw new IOException("Duplicate "+name);value=line.substring(colon+1).trim();}}return value;
    }
    public static boolean isUpgrade(String head)throws IOException{return "websocket".equalsIgnoreCase(value(head,"Upgrade"));}
    public static String accept(String key)throws IOException{try{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.US_ASCII)));}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
    public static void request(String head,byte[] body)throws IOException{
        String key=value(head,"Sec-WebSocket-Key"),connection=value(head,"Connection");
        try{if(!head.startsWith("GET ")||body.length!=0||!"13".equals(value(head,"Sec-WebSocket-Version"))||key==null||Base64.getDecoder().decode(key).length!=16||connection==null||!Arrays.asList(connection.toLowerCase(Locale.ROOT).split("\\s*,\\s*")).contains("upgrade"))throw new IllegalArgumentException();}
        catch(IllegalArgumentException e){throw new IOException("Invalid WebSocket handshake");}
    }
    public static void response(String head,String key)throws IOException{
        String connection=value(head,"Connection");
        if(!head.startsWith("HTTP/1.1 101 ")||!isUpgrade(head)||!accept(key).equals(value(head,"Sec-WebSocket-Accept"))||value(head,"Sec-WebSocket-Extensions")!=null||connection==null||!Arrays.asList(connection.toLowerCase(Locale.ROOT).split("\\s*,\\s*")).contains("upgrade"))throw new IOException("Invalid WebSocket upgrade response");
    }
    private static int next(InputStream in)throws IOException{int n=in.read();if(n<0)throw new EOFException("Incomplete WebSocket frame");return n;}
    public static Frame read(InputStream in,boolean client)throws IOException{
        int a=in.read();if(a<0)return null;int b=next(in);ByteArrayOutputStream wire=new ByteArrayOutputStream();wire.write(a);wire.write(b);
        int opcode=a&15;boolean fin=(a&128)!=0,masked=(b&128)!=0;long length=b&127;
        if((a&112)!=0||masked!=client||!(opcode==0||opcode==1||opcode==2||opcode==8||opcode==9||opcode==10))throw new IOException("Invalid WebSocket frame flags/opcode/mask");
        if(length==126){int hi=next(in),lo=next(in);wire.write(hi);wire.write(lo);length=(hi<<8)|lo;if(length<126)throw new IOException("Noncanonical WebSocket length");}
        else if(length==127){length=0;for(int i=0;i<8;i++){int n=next(in);wire.write(n);if(i==0&&n!=0)throw new IOException("WebSocket frame limit");length=(length<<8)|n;}if(length<65536)throw new IOException("Noncanonical WebSocket length");}
        if(length>1024*1024||(opcode>=8&&(!fin||length>125)))throw new IOException("WebSocket frame limit or fragmented control frame");
        byte[] mask=new byte[4];if(masked)for(int i=0;i<4;i++){mask[i]=(byte)next(in);wire.write(mask[i]);}
        byte[] payload=new byte[(int)length];int offset=0;while(offset<payload.length){int n=in.read(payload,offset,payload.length-offset);if(n<0)throw new EOFException("Truncated WebSocket payload");offset+=n;}
        wire.write(payload);if(masked)for(int i=0;i<payload.length;i++)payload[i]^=mask[i%4];
        if(opcode==8){if(payload.length==1)throw new IOException("Invalid close payload");if(payload.length>=2){int code=((payload[0]&255)<<8)|(payload[1]&255);if(code<1000||code>=5000||code==1004||code==1005||code==1006||code==1015||(code>=1016&&code<=2999))throw new IOException("Invalid close status");utf8(Arrays.copyOfRange(payload,2,payload.length));}}
        return new Frame(fin,opcode,payload,wire.toByteArray());
    }
    public static String utf8(byte[] value)throws IOException{try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(value)).toString();}catch(CharacterCodingException e){throw new IOException("Invalid WebSocket UTF-8",e);}}
    public static final class Messages {
        private int opcode=-1;private final ByteArrayOutputStream body=new ByteArrayOutputStream();
        public String accept(Frame frame)throws IOException{
            if(frame.opcode()>=8)return frame.opcode()==8?"CLOSE":frame.opcode()==9?"PING":"PONG";
            if(frame.opcode()==0){if(opcode<0)throw new IOException("Unexpected continuation");}
            else{if(opcode>=0)throw new IOException("Interleaved fragmented messages");opcode=frame.opcode();}
            if(body.size()+frame.payload().length>1024*1024)throw new IOException("WebSocket message exceeds 1 MiB");body.write(frame.payload());
            if(!frame.fin())return null;
            byte[] bytes=body.toByteArray();String preview;
            if(opcode==1){String text=utf8(bytes);preview="TEXT "+text.substring(0,Math.min(4096,text.length()))+(text.length()>4096?" [PREVIEW]":"");}
            else preview="BINARY "+bytes.length+" B";
            body.reset();opcode=-1;return preview;
        }
    }
}
