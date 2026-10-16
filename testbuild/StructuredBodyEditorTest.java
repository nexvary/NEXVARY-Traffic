import com.nexvary.trafficlab.core.StructuredBodyEditor;
import java.util.*;
public class StructuredBodyEditorTest {
  public static void main(String[] args) {
    var fs=StructuredBodyEditor.parseFlatJson("{\"item\":\"TE70\",\"price\":100,\"qty\":1,\"ok\":true}");
    if(fs.size()!=4) throw new AssertionError("parse size");
    fs=StructuredBodyEditor.replace(fs,"price","1");
    String out=StructuredBodyEditor.toJson(fs);
    if(!out.contains("\"price\":1")) throw new AssertionError(out);
    if(!StructuredBodyEditor.incrementNumber("100",-1).equals("99")) throw new AssertionError("increment");
    if(!StructuredBodyEditor.parseFlatJson("not json").isEmpty()) throw new AssertionError("non-json");
    System.out.println("STRUCTURED_BODY_EDITOR_OK\n"+out);
  }
}
