package com.nexvary.trafficlab.core;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class RawHttpClient {
    private RawHttpClient() {}
    public static LabResponse sendLocal(int port, LabRequest request) throws IOException {
        long start = System.nanoTime();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 3000);
            s.setSoTimeout(5000);
            OutputStream out = s.getOutputStream();
            String rawReq = request.toRaw("127.0.0.1:" + port);
            out.write(rawReq.getBytes(StandardCharsets.UTF_8)); out.flush();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            InputStream in = s.getInputStream(); byte[] chunk = new byte[4096]; int n;
            while ((n = in.read(chunk)) >= 0) buf.write(chunk,0,n);
            String raw = buf.toString(StandardCharsets.UTF_8);
            LabResponse res = new LabResponse(); res.raw = raw;
            int lineEnd = raw.indexOf("\r\n"); res.statusLine = lineEnd > 0 ? raw.substring(0,lineEnd) : raw;
            String[] parts = res.statusLine.split(" "); if (parts.length > 1) try { res.statusCode = Integer.parseInt(parts[1]); } catch(Exception ignored) {}
            int body = raw.indexOf("\r\n\r\n"); res.body = body >= 0 ? raw.substring(body+4) : "";
            res.elapsedMs = (System.nanoTime()-start)/1_000_000L;
            return res;
        }
    }
}
