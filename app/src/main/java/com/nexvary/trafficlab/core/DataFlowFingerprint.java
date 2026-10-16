package com.nexvary.trafficlab.core;
import java.nio.charset.StandardCharsets;import java.security.*;
public final class DataFlowFingerprint {
 public static String fingerprint(LabRequest q,LabResponse r){String raw=q.method+"\n"+ApiSchemaLearner.normalize(q.path)+"\n"+q.headers.keySet()+"\n"+shape(q.body)+"\n"+(r==null?0:r.statusCode);return sha(raw);}
 static String shape(String s){return s==null?"":s.replaceAll("\"[^\"]*\"","\"S\"").replaceAll("\\b\\d+\\b","N");}
 private static String sha(String s){try{byte[]d=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
