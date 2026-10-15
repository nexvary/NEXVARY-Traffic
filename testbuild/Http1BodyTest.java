import com.nexvary.trafficlab.core.Http1Body;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
public final class Http1BodyTest {
    private static byte[] read(String h,String b,int max)throws Exception{return Http1Body.read(h,new ByteArrayInputStream(b.getBytes(StandardCharsets.UTF_8)),max);}
    private static void bad(String h,String b,int max)throws Exception{try{read(h,b,max);throw new AssertionError("Malformed framing accepted");}catch(IOException expected){}}
    public static void main(String[] args)throws Exception{
        String head="POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n";
        byte[] decoded=read(head,"3;lesson=yes\r\nabc\r\n2\r\nde\r\n0\r\nX-Lesson: complete\r\n\r\n",100);
        if(!Arrays.equals(decoded,"abcde".getBytes(StandardCharsets.US_ASCII)))throw new AssertionError("Chunked body");
        bad(head,"3\r\nabc\r\n0\r\n\r\n",2);bad(head,"-1\r\n",100);bad(head,"1\r\naX\n0\r\n\r\n",100);
        bad(head,"0\r\nHost: changed\r\n\r\n",100);bad(head,"10\r\na",100);
        bad(head.replace("\r\n\r\n","\r\nContent-Length: 5\r\n\r\n"),"",100);
        bad("POST / HTTP/1.1\r\nContent-Length: 1\r\nContent-Length: 1\r\n\r\n","a",100);
        bad(head.replace("chunked","gzip, chunked"),"",100);
        if(read("POST / HTTP/1.1\r\nContent-Length: 2\r\n\r\n","ok",100).length!=2)throw new AssertionError("Content length");
        System.out.println("HTTP1_BODY_OK chunked, limits, framing ambiguity, malformed input");
    }
}
