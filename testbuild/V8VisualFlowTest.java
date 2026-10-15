import com.nexvary.trafficlab.core.*;
public class V8VisualFlowTest {
  public static void main(String[] args) throws Exception {
    DemoLabServer server=new DemoLabServer(18118); server.start();
    try {
      LabRequest r=new LabRequest(); r.method="POST"; r.path="/api/order"; r.headers.put("Content-Type","application/json"); r.body="{\"item\":\"TE70\",\"price\":100,\"qty\":1}";
      var fields=StructuredBodyEditor.parseFlatJson(r.body);
      fields=StructuredBodyEditor.replace(fields,"price","1");
      fields=StructuredBodyEditor.replace(fields,"qty",StructuredBodyEditor.incrementNumber("1",1));
      r.body=StructuredBodyEditor.toJson(fields);
      LabResponse res=RawHttpClient.sendLocal(18118,r);
      if(res.statusCode!=200 || !res.raw.contains("\"accepted_price\":1")) throw new AssertionError(res.raw);
      if(!r.body.contains("\"qty\":2")) throw new AssertionError(r.body);
      System.out.println("V8_VISUAL_FLOW_OK status="+res.statusCode+" body="+r.body);
    } finally { server.stop(); }
  }
}
