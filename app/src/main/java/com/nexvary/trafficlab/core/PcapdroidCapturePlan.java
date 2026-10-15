package com.nexvary.trafficlab.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Settings for the documented external CaptureCtrl API; no upstream code bundled. */
public final class PcapdroidCapturePlan {
    public static final int MIN_VERSION_CODE = 89;
    public static final String PACKAGE = "com.emanuelef.remote_capture";
    public static final String CONTROL = PACKAGE + ".activities.CaptureCtrl";
    private PcapdroidCapturePlan() { }

    public static Map<String, Object> start(String target, long sessionTime) {
        if (target == null || !target.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))
            throw new IllegalArgumentException("Select one application package");
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("action", "start");
        extras.put("app_filter", target);
        extras.put("tls_decryption", true);
        extras.put("decryption_rules", "[{\"type\":\"APP\",\"value\":\"" + target + "\"}]");
        extras.put("root_capture", false);
        extras.put("ip_mode", "both");
        extras.put("pcap_dump_mode", "pcap_file");
        extras.put("pcap_name", "nexvary-lab-" + sessionTime + ".pcap");
        extras.put("max_dump_size", 64 * 1024 * 1024);
        return extras;
    }
}
