import com.nexvary.trafficlab.core.*;
import java.util.*;

public class V9ExperienceTest {
  public static void main(String[] args) {
    LabRequest a=new LabRequest(); a.method="POST"; a.path="/api/order"; a.headers.put("Content-Type","application/json"); a.headers.put("Content-Length","999"); a.body="{\"item\":\"TE70\",\"price\":100,\"qty\":1,\"active\":true}";
    LabRequest b=a.copy(); b.body="{\"item\":\"demo\",\"price\":1,\"qty\":2,\"active\":false}";
    LabRequest n=RequestNormalizer.normalize(b);
    if(!Integer.toString(RequestNormalizer.utf8Length(b.body)).equals(n.headers.get("Content-Length"))) throw new AssertionError("content-length");
    var fields=StructuredBodyEditor.parseFlatJson(a.body); var hints=SchemaAwareEditor.infer(fields);
    if(hints.size()!=4 || hints.get(1).control()!=SchemaAwareEditor.Control.STEPPER || hints.get(3).control()!=SchemaAwareEditor.Control.TOGGLE) throw new AssertionError("schema hints");
    var changes=RequestCompare.compare(a,b); if(changes.size()<4) throw new AssertionError("compare");
    LabResponse res=new LabResponse(); res.statusCode=200; res.elapsedMs=12; res.raw="HTTP/1.1 200 OK";
    HistoryEntry e=new HistoryEntry(1,System.currentTimeMillis(),b,res,List.of());
    var story=SessionStoryEngine.build(List.of(e)); if(story.size()!=1 || !story.get(0).title().contains("Order")) throw new AssertionError("story");
    System.out.println("V9_EXPERIENCE_OK");
    System.out.println("contentLength="+n.headers.get("Content-Length"));
    System.out.println("changes="+changes.size());
    System.out.println("story="+story.get(0).title());
  }
}
