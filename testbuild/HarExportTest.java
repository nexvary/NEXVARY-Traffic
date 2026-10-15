import com.nexvary.trafficlab.core.*;
public final class HarExportTest {
    public static void main(String[] args){
        CapturedTraffic.openSession();CapturedTraffic.clear();
        String req="POST /lesson?token=SECRET_QUERY HTTP/1.1\r\nHost: lab.example\r\nAuthorization: SECRET_AUTH\r\nCookie: SECRET_COOKIE\r\n\r\nSECRET_BODY";
        CapturedTraffic.completed(req,req,"HTTP/1.1 200 OK\r\nSet-Cookie: SECRET_RESPONSE\r\n\r\nSECRET_RESULT",10,false);
        String redacted=HarExport.create(CapturedTraffic.all(),false),full=HarExport.create(CapturedTraffic.all(),true);
        if(redacted.contains("SECRET_")||!redacted.contains("https://lab.example/lesson")||!full.contains("SECRET_BODY"))throw new AssertionError("HAR privacy");
        if(!HarExport.quote("\"\\\n\u0001").equals("\"\\\"\\\\\\n\\u0001\""))throw new AssertionError("JSON escaping");
        long session=CapturedTraffic.archiveSession();CapturedTraffic.endSession();CapturedTraffic.imported(session,req,req,"late",0,0,"HAR_IMPORT");
        if(!CapturedTraffic.all().isEmpty())throw new AssertionError("Late archive import after Exit");
        System.out.println("HAR_EXPORT_OK redaction, text previews, escaping, stale session guard");
    }
}
