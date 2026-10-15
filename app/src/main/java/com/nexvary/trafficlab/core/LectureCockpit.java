package com.nexvary.trafficlab.core;
import java.util.*;
public final class LectureCockpit {
  public record Snapshot(int sessions,int findings,int maxRisk,long avgMs,int sensitiveHops,int apiRoutes,String nextCue,String status){}
  private LectureCockpit(){}
  public static Snapshot snapshot(List<HistoryEntry> entries){int sessions=entries==null?0:entries.size(),find=0,max=0;long sum=0;Set<String> routes=new HashSet<>();if(entries!=null)for(HistoryEntry e:entries){find+=e.findings.size();max=Math.max(max,RiskEngine.score(e.findings));sum+=e.response==null?0:e.response.elapsedMs;routes.add(e.request.method+" "+e.request.path.replaceAll("/\\d+(?=/|$)","/{id}"));}int hops=SensitiveDataJourney.trace(entries).size();String cue=sessions==0?"Run baseline request":find==0?"Show intercept and diff":"Open remediation graph";String st=max>=70?"ATTENTION":max>=30?"REVIEW":"READY";return new Snapshot(sessions,find,max,sessions==0?0:sum/sessions,hops,routes.size(),cue,st);}
  public static String pretty(Snapshot s){return "NEXVARY LECTURE COCKPIT\nStatus: "+s.status+"\nSessions: "+s.sessions+" • Findings: "+s.findings+" • Max risk: "+s.maxRisk+"/100\nAvg response: "+s.avgMs+" ms • API routes: "+s.apiRoutes+" • Sensitive journeys: "+s.sensitiveHops+"\nNext cue: "+s.nextCue;}
}
