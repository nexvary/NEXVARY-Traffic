import com.nexvary.trafficlab.core.ResponseEditor;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
public final class ResponseEditorTest {
    public static void main(String[] args)throws Exception{
        String raw="HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nTransfer-Encoding: chunked\r\n\r\n2\r\n{}\r\n0\r\n\r\n";
        String template=ResponseEditor.template(raw.getBytes(StandardCharsets.UTF_8));
        if(template.contains("Transfer-Encoding")||!template.contains("Content-Length: 2")||!template.endsWith("{}"))throw new AssertionError("Response dechunk");
        String changed=new String(ResponseEditor.normalize(template.replace("{}","{\"lesson\":1}")),StandardCharsets.UTF_8);
        if(!changed.contains("Content-Length: 12")||!changed.endsWith("{\"lesson\":1}"))throw new AssertionError("Edited framing");
        try{ResponseEditor.template(raw.replace("Transfer-Encoding: chunked","Content-Encoding: gzip").getBytes(StandardCharsets.UTF_8));throw new AssertionError("Compressed response accepted");}catch(IOException expected){}
        try{ResponseEditor.normalize("HTTP/1.1 204 No Content\r\n\r\nbody");throw new AssertionError("Bodyless response accepted");}catch(IOException expected){}
        System.out.println("RESPONSE_EDITOR_OK dechunk, UTF-8 framing, compressed/bodyless rejection");
    }
}
