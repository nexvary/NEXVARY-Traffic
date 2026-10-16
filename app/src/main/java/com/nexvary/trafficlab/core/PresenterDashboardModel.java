package com.nexvary.trafficlab.core;
import java.util.*;
public final class PresenterDashboardModel {
 public record Snapshot(int sessions,int findings,int maxRisk,long avgMs,String topRoute,String headline){}
 public static Snapshot summarize(List<HistoryEntry> es){int f=0,max=0;long total=0;Map<String,Integer>routes=new LinkedHashMap<>();for(HistoryEntry e:es){f+=e.findings.size();max=Math.max(max,RiskEngine.score(e.findings));if(e.response!=null)total+=e.response.elapsedMs;routes.merge(e.request.method+" "+ApiSchemaLearner.normalize(e.request.path),1,Integer::sum);}String top=routes.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("—");long avg=es.isEmpty()?0:total/es.size();String h=max>=70?"High-risk demo condition visible":max>=30?"Moderate findings available for discussion":"Lab baseline looks controlled";return new Snapshot(es.size(),f,max,avg,top,h);}
}
