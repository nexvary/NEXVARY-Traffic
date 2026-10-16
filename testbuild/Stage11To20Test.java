import com.nexvary.trafficlab.core.*; import java.util.*;
public class Stage11To20Test { public static void main(String[]z){
 LabRequest q=new LabRequest();q.method="POST";q.path="/api/users/42";q.body="{\"email\":\"demo@nexvary.com\",\"price\":100}";q.headers.put("Authorization","token=DEMO");
 LabResponse a=new LabResponse(); a.statusCode=200; a.body="{\"ok\":true}"; LabResponse b=new LabResponse(); b.statusCode=201; b.body="{\"ok\":true,\"v\":2}";
 HistoryEntry e=new HistoryEntry(1,System.currentTimeMillis(),q,a,List.of()); List<HistoryEntry> es=List.of(e);
 if(SessionCorrelator.group(es).size()!=1)throw new Error(); if(PrivacyLeakDetector.scan(q).size()<2)throw new Error(); if(!ApiSchemaLearner.learn(es).containsKey("POST /api/users/{id}"))throw new Error();
 if(RegressionComparator.compare(a,b).size()<2)throw new Error(); if(!PerformanceProfiler.profile(q,a,50).grade().equals("A"))throw new Error(); if(DemoScenarioEngine.builtIns().size()<2)throw new Error();
 if(FindingTimeline.build(es).size()!=1)throw new Error(); if(CommandPalette.search("pcap").isEmpty())throw new Error(); if(!LabReportBuilder.markdown("Demo",es).contains("NEXVARY"))throw new Error();
 if(IntegrityManifest.sha256("x").length()!=64)throw new Error(); System.out.println("STAGES_11_TO_20_OK"); }}
