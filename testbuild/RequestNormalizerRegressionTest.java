import com.nexvary.trafficlab.core.*;

public class RequestNormalizerRegressionTest {
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        LabRequest input = new LabRequest();
        input.body = "مرحبا 🌍";
        input.headers.put("Content-Length", "999");
        input.headers.put("content-length", "1");
        input.headers.put("CONTENT-LENGTH", "2");
        input.headers.put("Content-Type", "application/json");
        LabRequest result = RequestNormalizer.normalize(input);
        check(result.headers.keySet().stream().filter(k -> k.equalsIgnoreCase("Content-Length")).count() == 1,
                "Duplicate length headers survived");
        check(result.headers.get("Content-Length").equals("15"), "Incorrect UTF-8 byte count");
        check(input.headers.size() == 4 && input.headers.get("Content-Length").equals("999"),
                "Normalization mutated original evidence");
        check(result.headers.get("Content-Type").equals("application/json"), "Unrelated header changed");
        input.body = "";
        check(RequestNormalizer.normalize(input).headers.get("Content-Length").equals("0"), "Empty body length");
        input.body = null;
        check(RequestNormalizer.normalize(input).body.equals(""), "Null body not normalized");
        check(RequestNormalizer.normalize(null).headers.get("Content-Length").equals("0"), "Null input");
        check(RequestNormalizer.normalize(result).headers.equals(result.headers), "Not idempotent");
        System.out.println("REQUEST_NORMALIZER_REGRESSION_OK");
    }
}
