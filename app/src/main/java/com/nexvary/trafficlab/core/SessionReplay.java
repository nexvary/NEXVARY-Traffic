package com.nexvary.trafficlab.core;
import java.util.*;
public final class SessionReplay {
 public record Step(int index,long offsetMs,String direction,String summary,String payload){}
 public static List<Step> from(List<HistoryEntry> entries){if(entries.isEmpty())return List.of(); long t0=entries.get(0).timestamp; List<Step>o=new ArrayList<>(); int i=1; for(HistoryEntry e:entries){o.add(new Step(i++,Math.max(0,e.timestamp-t0),"REQUEST",e.request.method+" "+e.request.path,e.request.toRaw("127.0.0.1"))); if(e.response!=null)o.add(new Step(i++,Math.max(0,e.timestamp-t0+e.response.elapsedMs),"RESPONSE","HTTP "+e.response.statusCode,e.response.raw));} return List.copyOf(o);}
 public static String script(List<Step>s){StringBuilder b=new StringBuilder();for(Step x:s)b.append('+').append(x.offsetMs).append("ms ").append(x.direction).append(" • ").append(x.summary).append('\n');return b.toString();}
}
