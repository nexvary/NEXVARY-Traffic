package com.nexvary.trafficlab.evidence;

import java.nio.charset.StandardCharsets; import java.security.*; import java.util.*;

public class EvidenceWorkspace {
    public record Evidence(long timestamp,String label,String sha256,String previousHash){}
    private final List<Evidence> items=new ArrayList<>();
    public synchronized Evidence add(String label,String content){ String prev=items.isEmpty()?"GENESIS":items.get(items.size()-1).sha256(); String hash=sha256(prev+"\n"+label+"\n"+content); Evidence e=new Evidence(System.currentTimeMillis(),label,hash,prev); items.add(e); return e; }
    public synchronized List<Evidence> all(){return List.copyOf(items);} public synchronized boolean verifyChain(){String prev="GENESIS"; for(Evidence e:items){ if(!e.previousHash().equals(prev))return false; prev=e.sha256(); } return true; }
    public static String sha256(String s){try{byte[] d=MessageDigest.getInstance("SHA-256").digest((s==null?"":s).getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
