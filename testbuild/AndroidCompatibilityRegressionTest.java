import com.nexvary.trafficlab.core.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AndroidCompatibilityRegressionTest {
    public static void main(String[] args) throws Exception {
        if (!IntegrityManifest.sha256("abc").equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))
            throw new AssertionError("SHA-256 hex changed");
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            if (!CommandPalette.search("INTERCEPT").contains("Intercept request"))
                throw new AssertionError("Search depends on device locale");
            try {
                CommandPalette.search(null).add("unexpected");
                throw new AssertionError("Search result should stay immutable");
            } catch (UnsupportedOperationException expected) { }
        } finally { Locale.setDefault(original); }
        int port = 18128;
        DemoLabServer server = new DemoLabServer(port);
        server.start();
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write("POST /api/order HTTP/1.1\r\nHost: localhost\r\nContent-Length: 10\r\n\r\nx".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            socket.shutdownOutput();
            Scanner response = new Scanner(socket.getInputStream(), "UTF-8").useDelimiter("\\A");
            String raw = response.hasNext() ? response.next() : "";
            if (!raw.startsWith("HTTP/1.1 400")) throw new AssertionError("Truncated body accepted: " + raw);
        } finally { server.stop(); }
        System.out.println("ANDROID_COMPATIBILITY_REGRESSION_OK");
    }
}
