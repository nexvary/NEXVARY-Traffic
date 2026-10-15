package com.nexvary.trafficlab.core;
import java.util.*;
public final class RequestWaterfall {
  public record Span(long historyId,String route,long startMs,long durationMs,int status){}
  private RequestWaterfall(){}
  public static List<Span> build(List<HistoryEntry> entries){
    if(entries==null||entries.isEmpty())return List.of(); long t0=entries.stream().mapToLong(e->e.timestamp).min().orElse(0); List<Span> out=new ArrayList<>();
    for(HistoryEntry e:entries) out.add(new Span(e.id,e.request.method+" "+e.request.path,e.timestamp-t0,e.response==null?0:e.response.elapsedMs,e.response==null?0:e.response.statusCode));
    out.sort(Comparator.comparingLong(Span::startMs)); return out;
  }
  public static String ascii(List<Span> spans){StringBuilder b=new StringBuilder();for(Span s:spans){int n=(int)Math.min(40,Math.max(1,s.startMs()/25));b.append(String.format("#%d %6dms %s%s %dms → %d\n",s.historyId,s.startMs," ".repeat(n),"▰",s.durationMs,s.status)).append("   ").append(s.route).append('\n');}return b.toString();}
}
