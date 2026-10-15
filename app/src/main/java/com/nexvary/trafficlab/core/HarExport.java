package com.nexvary.trafficlab.core;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** HAR 1.2 metadata plus clearly labeled text previews; redaction is the default. */
public final class HarExport {
    private HarExport(){}
    public static String quote(String value){
        StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '"':out.append("\\\"");break;case '\\':out.append("\\\\");break;case '\n':out.append("\\n");break;case '\r':out.append("\\r");break;case '\t':out.append("\\t");break;default:if(c<32||Character.isSurrogate(c))out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}}
        return out.append('"').toString();
    }
    private static boolean sensitive(String name){String n=name.toLowerCase(Locale.ROOT);return n.contains("authorization")||n.contains("cookie")||n.contains("token")||n.contains("secret")||n.contains("api-key")||n.contains("apikey");}
    private static String headers(String raw,boolean secrets){
        StringBuilder out=new StringBuilder("[");String[] lines=raw.split("\\r\\n");boolean first=true;
        for(int i=1;i<lines.length;i++){if(lines[i].isEmpty())break;int colon=lines[i].indexOf(':');if(colon<1)continue;String name=lines[i].substring(0,colon);if(!secrets&&sensitive(name))continue;
            if(!first)out.append(',');first=false;out.append("{\"name\":").append(quote(name)).append(",\"value\":").append(quote(lines[i].substring(colon+1).trim())).append('}');}
        return out.append(']').toString();
    }
    public static String create(List<CapturedTraffic.Exchange> exchanges,boolean includeSecrets){
        StringBuilder out=new StringBuilder("{\"log\":{\"version\":\"1.2\",\"creator\":{\"name\":\"NEXVARY Traffic Lab\",\"version\":\"10.2.0\"},\"entries\":[");boolean first=true;
        for(CapturedTraffic.Exchange exchange:exchanges){
            LabRequest request=CapturedTraffic.request(exchange);LabResponse response=CapturedTraffic.response(exchange);String host="lab.invalid";
            for(Map.Entry<String,String> h:request.headers.entrySet())if(h.getKey().equalsIgnoreCase("Host"))host=h.getValue();
            String path=request.path;if(!includeSecrets){int query=path.indexOf('?');if(query>=0)path=path.substring(0,query);}
            String url="https://"+host+path;if(!first)out.append(',');first=false;
            out.append("{\"startedDateTime\":").append(quote(Instant.ofEpochMilli(exchange.timestamp()).toString())).append(",\"time\":").append(exchange.elapsedMs())
                .append(",\"request\":{\"method\":").append(quote(request.method)).append(",\"url\":").append(quote(url)).append(",\"httpVersion\":\"HTTP/1.1\",\"cookies\":[],\"queryString\":[],\"headers\":").append(headers(exchange.forwarded(),includeSecrets))
                .append(",\"headersSize\":-1,\"bodySize\":-1},\"response\":{\"status\":").append(response.statusCode).append(",\"statusText\":").append(quote(response.statusLine)).append(",\"httpVersion\":\"HTTP/1.1\",\"cookies\":[],\"headers\":").append(headers(exchange.response(),includeSecrets))
                .append(",\"content\":{\"size\":-1,\"mimeType\":\"application/octet-stream\",\"comment\":\"Body is available only as an optional NEXVARY text preview\"},\"redirectURL\":\"\",\"headersSize\":-1,\"bodySize\":-1},\"cache\":{},\"timings\":{\"send\":0,\"wait\":").append(exchange.elapsedMs()).append(",\"receive\":0},\"_nexvaryPrefixOnly\":").append(exchange.prefixOnly()).append(",\"_nexvarySource\":").append(quote(exchange.source()));
            if(includeSecrets)out.append(",\"_nexvaryOriginalTextPreview\":").append(quote(exchange.original())).append(",\"_nexvaryForwardedTextPreview\":").append(quote(exchange.forwarded())).append(",\"_nexvaryResponseTextPreview\":").append(quote(exchange.response()));
            else out.append(",\"comment\":\"Redacted export: query values, bodies and sensitive headers omitted\"");
            out.append('}');
        }
        return out.append("]}}").toString();
    }
}
