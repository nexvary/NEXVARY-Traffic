import com.nexvary.trafficlab.core.*;
import java.util.*;
import java.util.concurrent.*;

public class V6RobustnessTest {
  public static void main(String[] args) throws Exception {
    int port=18118;
    DemoLabServer s=new DemoLabServer(port); s.start();
    try {
      LabRequest r=new LabRequest(); r.method="POST"; r.path="/api/order"; r.headers.put("Content-Type","application/json; charset=utf-8");
      r.body="{\"item\":\"مثقاب تجريبي\",\"price\" : 1.5,\"qty\":1}";
      LabResponse res=RawHttpClient.sendLocal(port,r);
      if(res.statusCode!=200 || !res.body.contains("\"accepted_price\":1.5")) throw new AssertionError("UTF-8/price parsing failed: "+res.raw);
      ExecutorService ex=Executors.newFixedThreadPool(8); List<Future<Integer>> fs=new ArrayList<>();
      for(int i=0;i<24;i++){ final int n=i; fs.add(ex.submit(()->{LabRequest q=new LabRequest();q.method="POST";q.path="/api/order";q.body="{\"price\":"+(n+1)+"}";return RawHttpClient.sendLocal(port,q).statusCode;})); }
      for(Future<Integer> f:fs) if(f.get(5,TimeUnit.SECONDS)!=200) throw new AssertionError("concurrency response failed");
      ex.shutdownNow();
      System.out.println("V6_ROBUSTNESS_OK utf8=true concurrent=24");
    } finally { s.stop(); }
  }
}
