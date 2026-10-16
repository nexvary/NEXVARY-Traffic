package com.nexvary.trafficlab.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public final class LectureRecordingMetadata {
    public record Marker(long offsetMs,String title,String note) {}
    public record Recording(String lectureId,long startedAt,List<Marker> markers,String integritySha256) {}
    private LectureRecordingMetadata() {}

    public static Recording build(String lectureId,long startedAt,List<Marker> markers){StringBuilder canonical=new StringBuilder(lectureId).append('|').append(startedAt);for(Marker m:markers)canonical.append('|').append(m.offsetMs()).append(':').append(m.title()).append(':').append(m.note());return new Recording(lectureId,startedAt,List.copyOf(markers),sha(canonical.toString()));}
    public static String chapterText(Recording r){StringBuilder b=new StringBuilder("Lecture "+r.lectureId()+" • "+Instant.ofEpochMilli(r.startedAt())+"\n");for(Marker m:r.markers())b.append(String.format(Locale.ROOT,"%02d:%02d.%03d",m.offsetMs()/60000,(m.offsetMs()/1000)%60,m.offsetMs()%1000)).append("  ").append(m.title()).append(" — ").append(m.note()).append('\n');b.append("SHA-256 ").append(r.integritySha256());return b.toString();}
    private static String sha(String s){try{byte[] d=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format("%02x",x));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
