package com.nexvary.trafficlab.tls;
import com.nexvary.trafficlab.core.LabTarget;
import io.netty.buffer.*;
import io.netty.handler.codec.http2.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;

/** One bounded HTTP/2 GET for the guided test, not a crawler. */
final class Http2LabClient {
    private static void frame(OutputStream out,int type,int flags,int id,byte[] bytes)throws IOException{
        out.write(new byte[]{(byte)(bytes.length>>>16),(byte)(bytes.length>>>8),(byte)bytes.length,(byte)type,(byte)flags,(byte)(id>>>24),(byte)(id>>>16),(byte)(id>>>8),(byte)id});out.write(bytes);out.flush();
    }
    static String get(InputStream input,OutputStream out,LabTarget target)throws Exception{
        out.write("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII));frame(out,4,0,0,new byte[0]);
        ByteBuf block=Unpooled.buffer();try{
            new DefaultHttp2HeadersEncoder().encodeHeaders(1,new DefaultHttp2Headers().method("GET").scheme("https").authority(target.host()+":"+target.port()).path(target.path()).add("accept","*/*"),block);
            byte[] bytes=new byte[block.readableBytes()];block.readBytes(bytes);frame(out,1,5,1,bytes);
        }finally{block.release();}
        DataInputStream in=new DataInputStream(input);DefaultHttp2HeadersDecoder decoder=new DefaultHttp2HeadersDecoder();
        ByteArrayOutputStream headers=new ByteArrayOutputStream(),preview=new ByteArrayOutputStream();String summary="";long total=0;boolean continuing=false,end=false;
        for(int frames=0;frames<4096;frames++){
            int length=(in.readUnsignedByte()<<16)|(in.readUnsignedByte()<<8)|in.readUnsignedByte(),type=in.readUnsignedByte(),flags=in.readUnsignedByte(),id=in.readInt()&0x7fffffff;
            if(length>16384)throw new IOException("HTTP/2 frame exceeds negotiated limit");byte[] bytes=new byte[length];in.readFully(bytes);
            if(continuing&&(type!=9||id!=1))throw new IOException("Missing HTTP/2 CONTINUATION");
            if(type==4){if(id!=0||length%6!=0||((flags&1)!=0&&length!=0))throw new IOException("Invalid HTTP/2 SETTINGS");if((flags&1)==0)frame(out,4,1,0,new byte[0]);}
            else if(type==6){if(id!=0||length!=8)throw new IOException("Invalid HTTP/2 PING");if((flags&1)==0)frame(out,6,1,0,bytes);}
            else if(type==1||type==9){
                if(id!=1||(type==9&&!continuing))throw new IOException("Unexpected HTTP/2 header stream");
                if(type==1){if((flags&40)!=0)throw new IOException("Padded/priority response HEADERS are unsupported");end=(flags&1)!=0;}
                if(headers.size()+length>32768)throw new IOException("HTTP/2 response header limit");headers.write(bytes);continuing=(flags&4)==0;
                if(!continuing){ByteBuf decoded=Unpooled.wrappedBuffer(headers.toByteArray());try{Http2Headers values=decoder.decodeHeaders(1,decoded);if(values.status()!=null){summary="HTTP/2 "+values.status()+"\n"+values.toString()+"\n\n";}}finally{decoded.release();}headers.reset();if(end)return summary+preview.toString("UTF-8");}
            }else if(type==0){
                if(id!=1||summary.isEmpty()||continuing)throw new IOException("Unexpected HTTP/2 DATA");int pad=(flags&8)!=0?(bytes[0]&255)+1:0;if(pad>length)throw new IOException("Invalid HTTP/2 padding");int start=(flags&8)!=0?1:0,count=length-pad;
                total+=count;if(total>16L*1024*1024)throw new IOException("HTTP/2 response limit");if(preview.size()<32768)preview.write(bytes,start,Math.min(count,32768-preview.size()));
                if(length>0){byte[] increment=ByteBuffer.allocate(4).putInt(length).array();frame(out,8,0,0,increment);if((flags&1)==0)frame(out,8,0,1,increment);}
                if((flags&1)!=0)return summary+preview.toString("UTF-8")+(total>32768?"\n[PREVIEW ONLY]":"");
            }else if(type==3||type==7)throw new IOException("HTTP/2 peer reset/GOAWAY");
        }throw new IOException("HTTP/2 frame count limit");
    }
}
