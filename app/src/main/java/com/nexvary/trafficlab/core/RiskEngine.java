package com.nexvary.trafficlab.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RiskEngine {
    private RiskEngine() {}
    public static List<Finding> scan(LabRequest req, LabResponse res) {
        List<Finding> out = new ArrayList<>();
        String raw = (req.toRaw("lab") + "\n" + (res == null ? "" : res.raw)).toLowerCase(Locale.ROOT);
        if (req.path.startsWith("http://")) out.add(new Finding("HIGH", "Cleartext HTTP", "Request is not protected by TLS."));
        if (raw.contains("authorization: bearer ")) out.add(new Finding("MEDIUM", "Bearer token observed", "Authorization token is present in the captured request."));
        if (raw.contains("password=") || raw.contains("\"password\"")) out.add(new Finding("MEDIUM", "Credential field observed", "A password-like field appears in the request body."));
        if (res != null && !res.raw.toLowerCase(Locale.ROOT).contains("x-content-type-options")) out.add(new Finding("LOW", "Security header absent", "X-Content-Type-Options was not observed in the response."));
        return out;
    }
    public static int score(List<Finding> findings) {
        int s = 0;
        for (Finding f: findings) s += switch (f.severity()) { case "HIGH" -> 35; case "MEDIUM" -> 20; default -> 8; };
        return Math.min(100, s);
    }
}
