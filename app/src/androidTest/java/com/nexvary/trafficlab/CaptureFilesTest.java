package com.nexvary.trafficlab;
import android.content.Context;
import android.test.InstrumentationTestCase;
import com.nexvary.trafficlab.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;

public final class CaptureFilesTest extends InstrumentationTestCase {
    public void testHarRoundTripEncryptionAuthenticationAndDelete()throws Exception{
        Context context=getInstrumentation().getTargetContext();CapturedTraffic.openSession();CapturedTraffic.clear();
        String raw="POST /archive?token=QUERY_SECRET HTTP/1.1\r\nHost: lab.example\r\nAuthorization: AUTH_SECRET\r\n\r\nBODY_SECRET";
        CapturedTraffic.completed(raw,raw,"HTTP/1.1 200 OK\r\n\r\nRESPONSE_SECRET",12,false);
        try{
            String har=HarExport.create(CapturedTraffic.all(),true);new JSONObject(har);
            List<String[]> rows=CaptureFiles.parse(har);assertEquals(1,rows.size());assertEquals(raw,rows.get(0)[0]);
            String safe=HarExport.create(CapturedTraffic.all(),false);assertFalse(safe.contains("SECRET"));assertEquals(1,CaptureFiles.parse(safe).size());
            CaptureFiles.save(context,CapturedTraffic.all());File file=new File(context.getFilesDir(),"capture-session.enc");
            byte[] encrypted;try(InputStream in=new FileInputStream(file)){encrypted=CaptureFiles.bounded(in);}
            assertFalse(new String(encrypted,StandardCharsets.ISO_8859_1).contains("BODY_SECRET"));assertEquals(har,CaptureFiles.load(context));
            encrypted[encrypted.length-1]^=1;try(FileOutputStream out=new FileOutputStream(file)){out.write(encrypted);}
            try{CaptureFiles.load(context);fail("Tampered encrypted archive accepted");}catch(javax.crypto.AEADBadTagException expected){}
            try{CaptureFiles.parse("{\"log\":{\"entries\":[{\"request\":{\"method\":\"GET\",\"url\":\"file:///private\"},\"response\":{\"status\":200}}]}}");fail("Non-HTTP URL accepted");}catch(IOException expected){}
            CaptureFiles.delete(context);assertFalse(file.exists());
        }finally{CaptureFiles.delete(context);CapturedTraffic.clear();}
    }
}
