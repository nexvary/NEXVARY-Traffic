package com.nexvary.trafficlab.core;
import java.nio.charset.StandardCharsets; import java.security.*; import java.util.*;
public final class IntegrityManifest {
 public static String sha256(String s){try{byte[]d=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder(d.length*2);for(byte b:d){hex.append(Character.forDigit((b & 0xff) >>> 4,16));hex.append(Character.forDigit(b & 0x0f,16));}return hex.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 public static Map<String,String> build(Map<String,String> artifacts){Map<String,String>m=new LinkedHashMap<>();artifacts.forEach((k,v)->m.put(k,sha256(v)));return m;}
}