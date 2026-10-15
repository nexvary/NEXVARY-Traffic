package com.nexvary.trafficlab.core;
import java.util.*;
public final class AnomalyExplainer {
 public static List<String> explain(List<HistoryEntry> es){List<String>o=new ArrayList<>();if(es.size()<2)return List.of("Collect at least two lab sessions to establish a comparison baseline.");HistoryEntry base=es.get(0);for(int i=1;i<es.size();i++){HistoryEntry e=es.get(i);if(e.response!=null&&base.response!=null&&e.response.statusCode!=base.response.statusCode)o.add("Status changed "+base.response.statusCode+" → "+e.response.statusCode+" for "+e.request.path);if(!Objects.equals(e.request.body,base.request.body))o.add("Request body changed for "+e.request.path+"; inspect Diff before attributing cause.");if(RiskEngine.score(e.findings)>RiskEngine.score(base.findings))o.add("Risk score increased for "+e.request.path+" due to newly detected passive findings.");}return o.isEmpty()?List.of("No material anomaly detected against the first captured lab session."):List.copyOf(o);}
}
