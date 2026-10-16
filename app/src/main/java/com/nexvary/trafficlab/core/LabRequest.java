package com.nexvary.trafficlab.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class LabRequest {
    public String method = "GET";
    public String path = "/";
    public final Map<String,String> headers = new LinkedHashMap<>();
    public String body = "";
    public long createdAt = System.currentTimeMillis();

    public LabRequest copy() {
        LabRequest r = new LabRequest();
        r.method = method; r.path = path; r.body = body; r.createdAt = createdAt;
        r.headers.putAll(headers);
        return r;
    }

    public String toRaw(String host) {
        StringBuilder b = new StringBuilder();
        b.append(method).append(' ').append(path).append(" HTTP/1.1\r\n");
        if (!headers.containsKey("Host")) b.append("Host: ").append(host).append("\r\n");
        for (Map.Entry<String,String> e: headers.entrySet()) b.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        if (!body.isEmpty() && !headers.containsKey("Content-Length")) b.append("Content-Length: ").append(body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).append("\r\n");
        b.append("Connection: close\r\n\r\n").append(body);
        return b.toString();
    }
}
