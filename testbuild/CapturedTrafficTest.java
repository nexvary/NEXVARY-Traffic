import com.nexvary.trafficlab.core.*;
public class CapturedTrafficTest {
    public static void main(String[] args){
        CapturedTraffic.clear();
        String original="POST /lesson HTTP/1.1\r\nHost: lab.example\r\n\r\n{\"price\":100}";
        String forwarded=original.replace("100","1");
        CapturedTraffic.completed(original,forwarded,"HTTP/1.1 201 Created\r\nContent-Length: 2\r\n\r\nok",42,false);
        CapturedTraffic.Exchange exchange=CapturedTraffic.all().get(0);
        LabRequest request=CapturedTraffic.request(exchange);LabResponse response=CapturedTraffic.response(exchange);
        if(!request.path.equals("/lesson")||!request.body.equals("{\"price\":1}")||!request.headers.get("Host").equals("lab.example"))throw new AssertionError("Real forwarded request lost");
        if(response.statusCode!=201||!response.body.equals("ok")||response.elapsedMs!=42||exchange.prefixOnly())throw new AssertionError("Response handoff");
        for(int i=0;i<40;i++)CapturedTraffic.completed(original,forwarded,"x".repeat(40000),i,false);
        if(CapturedTraffic.all().size()!=32||!CapturedTraffic.all().get(0).prefixOnly()||CapturedTraffic.all().get(0).response().length()!=32768)throw new AssertionError("Retention/prefix budget");
        CapturedTraffic.clear();if(!CapturedTraffic.all().isEmpty())throw new AssertionError("Clear");
        System.out.println("CAPTURED_TRAFFIC_OK");
    }
}
