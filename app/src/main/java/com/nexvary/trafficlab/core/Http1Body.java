package com.nexvary.trafficlab.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Strict, bounded HTTP/1.1 request framing, including chunked uploads. */
public final class Http1Body {
    private Http1Body(){}
    public static byte[] read(String head,InputStream in,int maximum)throws IOException{
        Integer length=null;String transfer=null;
        for(String line:head.split("\r\n")){
            int colon=line.indexOf(':');if(colon<1)continue;
            String name=line.substring(0,colon),value=line.substring(colon+1).trim();
            if(name.equalsIgnoreCase("Content-Length")){
                if(length!=null||!value.matches("[0-9]+"))throw new IOException("Invalid or duplicate Content-Length");
                try{length=Integer.parseInt(value);}catch(NumberFormatException error){throw new IOException("Invalid Content-Length");}
                if(length>maximum)throw new IOException("Request body exceeds limit");
            }
            if(name.equalsIgnoreCase("Transfer-Encoding")){
                if(transfer!=null)throw new IOException("Duplicate Transfer-Encoding");transfer=value.toLowerCase(Locale.ROOT);
            }
        }
        if(transfer!=null){
            if(length!=null)throw new IOException("Ambiguous request framing");
            if(!transfer.equals("chunked"))throw new IOException("Unsupported transfer coding");
            return chunks(in,maximum);
        }
        byte[] body=new byte[length==null?0:length];new DataInputStream(in).readFully(body);return body;
    }
    private static String line(InputStream in,int maximum)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        while(out.size()<maximum){int b=in.read();if(b<0)throw new EOFException("Incomplete chunk framing");
            if(b==13){if(in.read()!=10)throw new IOException("Invalid chunk CRLF");return out.toString("US-ASCII");}
            if(b==10||b<32||b>126)throw new IOException("Invalid chunk line");out.write(b);
        }throw new IOException("Chunk line exceeds limit");
    }
    private static byte[] chunks(InputStream in,int maximum)throws IOException{
        ByteArrayOutputStream body=new ByteArrayOutputStream();int count=0;
        while(++count<=65536){
            String line=line(in,1024),size=line.split(";",2)[0];
            if(!size.matches("[0-9A-Fa-f]{1,8}"))throw new IOException("Invalid chunk size");
            long length=Long.parseLong(size,16);if(length>maximum-body.size())throw new IOException("Chunked body exceeds limit");
            if(length==0){
                int trailers=0;while(true){String trailer=line(in,8192);trailers+=trailer.length()+2;if(trailers>32768)throw new IOException("Trailers exceed limit");if(trailer.isEmpty())return body.toByteArray();
                    int colon=trailer.indexOf(':');if(colon<1)throw new IOException("Malformed trailer");String name=trailer.substring(0,colon);
                    if(name.equalsIgnoreCase("Content-Length")||name.equalsIgnoreCase("Transfer-Encoding")||name.equalsIgnoreCase("Host")||name.equalsIgnoreCase("Authorization"))throw new IOException("Forbidden framing or authentication trailer");
                }
            }
            byte[] data=new byte[(int)length];new DataInputStream(in).readFully(data);body.write(data);
            if(in.read()!=13||in.read()!=10)throw new IOException("Missing chunk CRLF");
        }throw new IOException("Too many chunks");
    }
}
