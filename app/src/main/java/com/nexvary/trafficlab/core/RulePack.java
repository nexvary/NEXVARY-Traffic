package com.nexvary.trafficlab.core;

import java.nio.charset.StandardCharsets; import java.security.*;

public class RulePack {
    public final String name; public final String version; public final String payload; public final String sha256;
    public RulePack(String name,String version,String payload){ this.name=name; this.version=version; this.payload=payload; this.sha256=sha256(payload); }
    public boolean verifyIntegrity(){ return sha256.equals(sha256(payload)); }
    public static String sha256(String s){ try{ byte[] d=MessageDigest.getInstance("SHA-256").digest((s==null?"":s).getBytes(StandardCharsets.UTF_8)); StringBuilder b=new StringBuilder(); for(byte x:d)b.append(String.format("%02x",x)); return b.toString(); }catch(Exception e){throw new IllegalStateException(e);} }
    public static RulePack builtIn(){ return new RulePack("NEXVARY Core Rules","1.0","plaintext-http;credential-field;missing-security-headers;client-authoritative-price"); }
}
