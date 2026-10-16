package com.nexvary.trafficlab.core;
import java.util.*;
public final class ApiBehaviorBaseline {
  public record RouteBaseline(String route,int samples,double avgMs,Set<Integer> statuses,double avgRisk){}
  private ApiBehaviorBaseline(){}
  public static List<RouteBaseline> learn(List<HistoryEntry> entries){Map<String,List<HistoryEntry>> m=new LinkedHashMap<>();if(entries!=null)for(HistoryEntry e:entries)m.computeIfAbsent(normalize(e.request.method+" "+e.request.path),k->new ArrayList<>()).add(e);List<RouteBaseline> out=new ArrayList<>();for(var x:m.entrySet()){double ms=x.getValue().stream().mapToLong(e->e.response==null?0:e.response.elapsedMs).average().orElse(0);double risk=x.getValue().stream().mapToInt(e->RiskEngine.score(e.findings)).average().orElse(0);Set<Integer> st=new LinkedHashSet<>();for(var e:x.getValue())if(e.response!=null)st.add(e.response.statusCode);out.add(new RouteBaseline(x.getKey(),x.getValue().size(),ms,st,risk));}return out;}
  private static String normalize(String s){return s.replaceAll("/\\d+(?=/|$)","/{id}");}
  public static String pretty(List<RouteBaseline> xs){StringBuilder b=new StringBuilder();for(var x:xs)b.append(x.route).append(" • n=").append(x.samples).append(" • avg=").append(String.format(java.util.Locale.US,"%.1f",x.avgMs)).append("ms • status=").append(x.statuses).append(" • risk=").append(String.format(java.util.Locale.US,"%.1f",x.avgRisk)).append('\n');return b.toString();}
}
