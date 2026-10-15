import com.nexvary.trafficlab.core.*; import java.util.*;
public class Stage21To30Test { public static void main(String[] z){
 LabRequest q=new LabRequest(); q.method="POST"; q.path="/api/orders/42"; q.headers.put("Content-Type","application/json"); q.body="{\"item\":\"TE70\",\"price\":100,\"qty\":1}";
 LabResponse r1=new LabResponse(); r1.statusCode=200;r1.elapsedMs=40;r1.raw="HTTP/1.1 200 OK\r\n\r\n{\"ok\":true}";r1.body="{\"ok\":true}";
 LabRequest q2=q.copy();q2.body=q2.body.replace("100","1"); LabResponse r2=new LabResponse();r2.statusCode=201;r2.elapsedMs=55;r2.raw="HTTP/1.1 201 Created\r\n\r\n{\"ok\":true}";r2.body="{\"ok\":true}";
 Finding f=new Finding("HIGH","Client-controlled price","Demo finding");
 List<HistoryEntry> es=List.of(new HistoryEntry(1,1000,q,r1,List.of()),new HistoryEntry(2,1100,q2,r2,List.of(f)));
 if(ApiGraphBuilder.build(es).edges().isEmpty())throw new Error("graph");
 if(LiveSecurityTimeline.build(es).size()<3)throw new Error("timeline");
 if(RequestMutationMatrix.safeLectureVariants(q).size()<4)throw new Error("matrix");
 if(SessionReplay.from(es).size()!=4)throw new Error("replay");
 if(PresenterDashboardModel.summarize(es).sessions()!=2)throw new Error("dashboard");
 if(!InteractiveReportBuilder.html("Demo",es).contains("NEXVARY Traffic Lab"))throw new Error("html");
 if(LectureCueEngine.standard().size()<6)throw new Error("cues");
 if(DataFlowFingerprint.fingerprint(q,r1).length()!=64)throw new Error("fp");
 if(AnomalyExplainer.explain(es).isEmpty())throw new Error("anomaly");
 var bundle=LabExportBundle.build("Demo",es); if(bundle.size()!=4||!LabExportBundle.verify(bundle))throw new Error("bundle");
 System.out.println("STAGES_21_TO_30_OK");
}}
