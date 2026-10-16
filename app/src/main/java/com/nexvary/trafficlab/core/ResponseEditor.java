package com.nexvary.trafficlab.core;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/** UTF-8 HTTP/1.1 response editing; compressed and binary payloads pass through unchanged. */
public final class ResponseEditor {
    private ResponseEditor(){}
    public static String template(byte[] response)throws IOException{
        int boundary=-1;for(int i=0;i<response.length-3&&i<32768;i++)if(response[i]==13&&response[i+1]==10&&response[i+2]==13&&response[i+3]==10){boundary=i;break;}
        if(boundary<0)throw new IOException("Incomplete or oversized response headers");
        String head=new String(response,0,boundary+4,StandardCharsets.ISO_8859_1);
        for(String line:head.split("\r\n")){
            int colon=line.indexOf(':');if(colon<1)continue;String name=line.substring(0,colon),value=line.substring(colon+1).trim().toLowerCase(Locale.ROOT);
            if(name.equalsIgnoreCase("Content-Encoding")&&!value.equals("identity"))throw new IOException("Compressed response: forward original or drop");
            if(name.equalsIgnoreCase("Content-Type")&&!(value.startsWith("text/")||value.contains("json")||value.contains("xml")||value.contains("javascript")||value.contains("x-www-form-urlencoded")))throw new IOException("Binary response: forward original or drop");
            if(name.equalsIgnoreCase("Content-Type")&&value.contains("charset=")&&!value.contains("charset=utf-8")&&!value.contains("charset=\"utf-8\""))throw new IOException("Response editor requires UTF-8");
        }
        byte[] body=Arrays.copyOfRange(response,boundary+4,response.length);
        if(head.toLowerCase(Locale.ROOT).contains("transfer-encoding:")||head.toLowerCase(Locale.ROOT).contains("content-length:"))body=Http1Body.read(head,new ByteArrayInputStream(body),1024*1024);
        String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body)).toString();
        return new String(normalize(head+text),StandardCharsets.UTF_8);
    }
    public static byte[] normalize(String edited)throws IOException{
        int boundary=edited.indexOf("\r\n\r\n");if(boundary<0||boundary>32768)throw new IOException("Expected HTTP response headers");
        String[] lines=edited.substring(0,boundary).split("\r\n");String[] status=lines[0].split(" ",3);
        if(status.length<2||!status[0].equals("HTTP/1.1")||!status[1].matches("[2-5][0-9][0-9]"))throw new IOException("Final HTTP/1.1 response required");
        int code=Integer.parseInt(status[1]);byte[] body=edited.substring(boundary+4).getBytes(StandardCharsets.UTF_8);
        if(body.length>1024*1024)throw new IOException("Edited response exceeds 1 MiB");
        if(code==204||code==304)throw new IOException("Bodyless status: forward original or drop");
        StringBuilder out=new StringBuilder(lines[0]).append("\r\n");
        for(int i=1;i<lines.length;i++){
            int colon=lines[i].indexOf(':');if(colon<1)throw new IOException("Malformed response header");String name=lines[i].substring(0,colon);
            if(!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+"))throw new IOException("Invalid response header");
            String value=lines[i].substring(colon+1);for(int j=0;j<value.length();j++){char c=value.charAt(j);if(c<32&&c!=9||c==127)throw new IOException("Invalid response header value");}
            if(name.equalsIgnoreCase("Upgrade"))throw new IOException("Response upgrades cannot be edited");
            if(name.equalsIgnoreCase("Content-Length")||name.equalsIgnoreCase("Transfer-Encoding")||name.equalsIgnoreCase("Trailer")||name.equalsIgnoreCase("Connection")||name.equalsIgnoreCase("Content-Encoding"))continue;
            out.append(lines[i]).append("\r\n");
        }
        out.append("Content-Length: ").append(body.length).append("\r\nConnection: close\r\n\r\n");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();bytes.write(out.toString().getBytes(StandardCharsets.UTF_8));bytes.write(body);return bytes.toByteArray();
    }
}
