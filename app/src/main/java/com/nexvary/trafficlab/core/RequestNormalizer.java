package com.nexvary.trafficlab.core;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Normalizes mutable lab requests before local transmission. */
public final class RequestNormalizer {
    private RequestNormalizer() {}

    public static LabRequest normalize(LabRequest input) {
        LabRequest r = input == null ? new LabRequest() : input.copy();
        r.body = r.body == null ? "" : r.body;
        // HTTP header names are case-insensitive. Remove every stale length,
        // including differently-cased duplicates, before calculating UTF-8 bytes.
        r.headers.keySet().removeIf(k -> "Content-Length".equalsIgnoreCase(k));
        r.headers.put("Content-Length", Integer.toString(utf8Length(r.body)));
        return r;
    }

    public static int utf8Length(String body) {
        return body == null ? 0 : body.getBytes(StandardCharsets.UTF_8).length;
    }
}
