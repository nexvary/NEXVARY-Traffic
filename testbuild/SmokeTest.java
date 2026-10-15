import com.nexvary.trafficlab.core.*;
public class SmokeTest {
  public static void main(String[] a) throws Exception {
    DemoLabServer s=new DemoLabServer(18088); s.start(); Thread.sleep(100);
    LabRequest r=new LabRequest();r.method="POST";r.path="/api/order";r.headers.put("Content-Type","application/json");r.body="{\"price\":1,\"password\":\"demo\"}";
    LabResponse x=RawHttpClient.sendLocal(18088,r);
    if(x.statusCode!=200 || !x.body.contains("accepted_price\":1")) throw new RuntimeException("bad response "+x.raw);
    if(RiskEngine.scan(r,x).isEmpty()) throw new RuntimeException("risk engine empty");
    s.stop(); System.out.println("SMOKE_OK status="+x.statusCode+" body="+x.body);
  }
}
