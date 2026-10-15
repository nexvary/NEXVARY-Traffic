import com.nexvary.trafficlab.core.PcapdroidCapturePlan;
import java.util.Map;
public class PcapdroidCapturePlanTest {
    public static void main(String[] args) {
        Map<String,Object> plan = PcapdroidCapturePlan.start("com.nexvary.training", 123);
        if (!plan.get("app_filter").equals("com.nexvary.training")) throw new AssertionError("scope");
        if (!plan.get("decryption_rules").equals("[{\"type\":\"APP\",\"value\":\"com.nexvary.training\"}]")) throw new AssertionError("TLS scope");
        if (!Boolean.TRUE.equals(plan.get("tls_decryption")) || !Boolean.FALSE.equals(plan.get("root_capture"))) throw new AssertionError("mode");
        if (!plan.get("max_dump_size").equals(67108864)) throw new AssertionError("dump limit");
        if (!plan.get("pcap_name").equals("nexvary-lab-123.pcap")) throw new AssertionError("session file");
        for (String bad : new String[]{"", "*", "com.a,com.b", "com.a\"", "com.a\n", "com..a"}) {
            try { PcapdroidCapturePlan.start(bad, 0); throw new AssertionError("Accepted " + bad); }
            catch (IllegalArgumentException expected) { }
        }
        System.out.println("PCAPDROID_CAPTURE_PLAN_OK");
    }
}
