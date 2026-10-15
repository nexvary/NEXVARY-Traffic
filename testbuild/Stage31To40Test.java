import com.nexvary.trafficlab.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Stage31To40Test {
    static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args) throws Exception {
        LabRequest r=new LabRequest();r.method="POST";r.path="/api/order/42";r.headers.put("Content-Type","application/json");r.body="{\"price\":1}";
        LabResponse p=new LabResponse();p.statusCode=200;p.body="{\"ok\":true}";p.elapsedMs=12;
        HistoryEntry e=new HistoryEntry(1,System.currentTimeMillis(),r,p,List.of(new Finding("HIGH","Client price trusted","demo")));
        var map=VisualPacketMap.fromHistory(List.of(e));ok(map.links().size()==1,"map");
        var radar=LiveEndpointRadar.summarize(List.of(e));ok(radar.get(0).route().contains("/{id}"),"radar normalize");
        byte[] h2=Http2FrameLab.encode(0,1,3,"abc".getBytes(StandardCharsets.UTF_8));ok(Http2FrameLab.decode(h2).streamId()==3,"h2");
        byte[] gm=GrpcInspector.frame("xyz".getBytes(StandardCharsets.UTF_8),false);ok(GrpcInspector.inspect(gm).length()==3,"grpc");
        String jwt="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhIiwiZXhwIjo0MTAyNDQ0ODAwfQ.ZGVtbw";ok("HS256".equals(JwtAnalyzer.analyze(jwt,1000).algorithm()),"jwt");
        ok(OAuthFlowVisualizer.authorizationCodePkce().size()==7,"oauth");
        Map<String,String> a=Map.of("status","200"),b=Map.of("status","201","field","x");ok(ApiContractDiff.compare(a,b).size()==2,"contract");
        ok(SmartFindingCorrelator.correlate(List.of(e)).size()==1,"correlator");
        var rec=LectureRecordingMetadata.build("L1",1000,List.of(new LectureRecordingMetadata.Marker(0,"A","B")));ok(rec.integritySha256().length()==64,"recording");
        CaseWorkspace cw=new CaseWorkspace();String id=cw.create("Demo");cw.attach(id,1);cw.note(id,"ok");ok(cw.all().get(0).historyIds().contains(1L),"case");
        System.out.println("STAGES_31_TO_40_OK");
        System.out.println("packetLinks="+map.links().size()+" radar="+radar.get(0).route()+" h2="+Http2FrameLab.decode(h2).typeName()+" grpc="+GrpcInspector.inspect(gm).length()+" cases="+cw.all().size());
    }
}
