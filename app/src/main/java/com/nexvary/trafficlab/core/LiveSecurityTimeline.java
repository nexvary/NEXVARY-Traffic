package com.nexvary.trafficlab.core;
import java.util.*;
public final class LiveSecurityTimeline {
 public record Event(long time,String lane,String severity,String text){}
 public static List<Event> build(List<HistoryEntry> es){List<Event> out=new ArrayList<>(); for(HistoryEntry e:es){out.add(new Event(e.timestamp,"HTTP","INFO",e.request.method+" "+e.request.path+" → "+(e.response==null?"-":e.response.statusCode))); for(Finding f:e.findings) out.add(new Event(e.timestamp,"RISK",f.severity(),f.title()));} out.sort(Comparator.comparingLong(Event::time)); return out;}
 public static String compact(List<Event> es){StringBuilder b=new StringBuilder();for(Event e:es)b.append(e.lane()).append(" | ").append(e.severity()).append(" | ").append(e.text()).append('\n');return b.toString();}
}
