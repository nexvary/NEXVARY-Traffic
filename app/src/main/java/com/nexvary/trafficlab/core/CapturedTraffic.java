package com.nexvary.trafficlab.core;
import java.util.*;

/** Bounded process-local handoff from real capture to the existing analysis pages. */
public final class CapturedTraffic {
    public record Exchange(String id,String original,String forwarded,String response,long elapsedMs,boolean prefixOnly,long timestamp,String source) { }
    private static final Deque<Exchange> exchanges=new ArrayDeque<>();
    private static final int LIMIT=32768;
    private static long generation,clearVersion;
    private static boolean accepting=true;
    public static synchronized long openSession(){accepting=true;return ++generation;}
    public static synchronized void endSession(){accepting=false;generation++;clearVersion++;exchanges.clear();}
    private CapturedTraffic() { }
    private static String prefix(String value){return value.substring(0,Math.min(value.length(),LIMIT));}
    public static synchronized void completed(String original,String forwarded,String response,long elapsedMs,boolean truncated){
        completed(generation,original,forwarded,response,elapsedMs,truncated);
    }
    public static synchronized void completed(long session,String original,String forwarded,String response,long elapsedMs,boolean truncated){
        if(!accepting||session!=generation)return;
        boolean prefixOnly=truncated||original.length()>LIMIT||forwarded.length()>LIMIT||response.length()>LIMIT;
        exchanges.addLast(new Exchange(UUID.randomUUID().toString(),prefix(original),prefix(forwarded),prefix(response),elapsedMs,prefixOnly,System.currentTimeMillis(),"REAL_HTTPS"));
        while(exchanges.size()>32)exchanges.removeFirst();
    }
    public static synchronized long archiveSession(){if(!accepting){accepting=true;generation++;}return generation;}
    public static synchronized void imported(long session,String original,String forwarded,String response,long elapsedMs,long timestamp,String source){
        if(!accepting||session!=generation)return;
        exchanges.addLast(new Exchange(UUID.randomUUID().toString(),prefix(original),prefix(forwarded),prefix(response),Math.max(0,elapsedMs),true,Math.max(0,timestamp),source.equals("RESTORED_CAPTURE")?source:"HAR_IMPORT"));
        while(exchanges.size()>32)exchanges.removeFirst();
    }
    public static synchronized List<Exchange> all(){return new ArrayList<>(exchanges);}
    public static synchronized long clearVersion(){return clearVersion;}
    public static synchronized void clear(){clearVersion++;exchanges.clear();}
    public static LabRequest request(Exchange exchange){
        String raw=exchange.forwarded();int boundary=raw.indexOf("\r\n\r\n");String head=boundary<0?raw:raw.substring(0,boundary);
        String[] lines=head.split("\r\n");String[] first=lines[0].split(" ",3);LabRequest request=new LabRequest();
        if(first.length>=2){request.method=first[0];request.path=first[1];}
        for(int i=1;i<lines.length;i++){int colon=lines[i].indexOf(':');if(colon>0)request.headers.put(lines[i].substring(0,colon),lines[i].substring(colon+1).trim());}
        request.body=boundary<0?"":raw.substring(boundary+4);return request;
    }
    public static LabResponse response(Exchange exchange){
        LabResponse response=new LabResponse();response.raw=exchange.response();response.elapsedMs=exchange.elapsedMs();
        int line=response.raw.indexOf("\r\n");response.statusLine=line<0?response.raw:response.raw.substring(0,line);
        String[] first=response.statusLine.split(" ");if(first.length>1)try{response.statusCode=Integer.parseInt(first[1]);}catch(NumberFormatException ignored){}
        int body=response.raw.indexOf("\r\n\r\n");response.body=body<0?"":response.raw.substring(body+4);return response;
    }
}
