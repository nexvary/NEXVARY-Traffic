package com.nexvary.trafficlab.core;

public class InterceptEngine {
    private LabRequest pending;
    private LabRequest original;
    public synchronized void intercept(LabRequest req) { original = req.copy(); pending = req.copy(); }
    public synchronized LabRequest pending() { return pending == null ? null : pending.copy(); }
    public synchronized LabRequest original() { return original == null ? null : original.copy(); }
    public synchronized void replace(String method, String path, String rawHeaders, String body) {
        if (pending == null) return;
        pending.method = method.trim().toUpperCase(); pending.path = path.trim(); pending.body = body;
        pending.headers.clear();
        for (String line: rawHeaders.split("\\r?\\n")) {
            int p = line.indexOf(':'); if (p > 0) pending.headers.put(line.substring(0,p).trim(), line.substring(p+1).trim());
        }
    }
    public synchronized LabRequest forward() { LabRequest r = pending == null ? null : pending.copy(); pending = null; return r; }
    public synchronized void drop() { pending = null; }
}
