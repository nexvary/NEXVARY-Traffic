package com.nexvary.trafficlab.core;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;

public final class JwtAnalyzer {
    public record Analysis(String headerJson,String payloadJson,String algorithm,Long expiresAt,boolean expired,List<String> notes) {}
    private JwtAnalyzer() {}

    public static Analysis analyze(String token,long nowEpochSeconds){
        if(token==null)throw new IllegalArgumentException("token is null"); String[] p=token.trim().split("\\.",-1);if(p.length!=3)throw new IllegalArgumentException("JWT must contain 3 segments");
        String h=decode(p[0]), body=decode(p[1]); String alg=findString(h,"alg"); Long exp=findLong(body,"exp");
        List<String> notes=new ArrayList<>(); if("none".equalsIgnoreCase(alg))notes.add("Unsigned alg=none token should not be trusted by a server."); if(alg==null||alg.isBlank())notes.add("No alg claim found in header."); if(exp==null)notes.add("No exp claim found.");
        boolean expired=exp!=null&&exp<=nowEpochSeconds; if(expired)notes.add("Token is expired at "+Instant.ofEpochSecond(exp)+".");
        notes.add("This analyzer decodes claims only; it does not crack keys or bypass signature verification.");
        return new Analysis(h,body,alg==null?"unknown":alg,exp,expired,List.copyOf(notes));
    }
    private static String decode(String s){try{return new String(Base64.getUrlDecoder().decode(pad(s)), StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalArgumentException("invalid base64url JWT segment");}}
    private static String pad(String s){int m=s.length()%4;return m==0?s:s+"====".substring(m);}
    private static String findString(String j,String k){Matcher m=Pattern.compile("\\\""+Pattern.quote(k)+"\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(j);return m.find()?m.group(1):null;}
    private static Long findLong(String j,String k){Matcher m=Pattern.compile("\\\""+Pattern.quote(k)+"\\\"\\s*:\\s*(\\d+)").matcher(j);return m.find()?Long.parseLong(m.group(1)):null;}
}
