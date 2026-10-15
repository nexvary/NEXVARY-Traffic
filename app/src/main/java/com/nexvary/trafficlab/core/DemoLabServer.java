package com.nexvary.trafficlab.core;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loopback-only HTTP server used by the authorized lecture scenarios. */
public class DemoLabServer {
    private static final int MAX_HEADER_BYTES = 32 * 1024;
    private static final int MAX_BODY_BYTES = 1024 * 1024;
    private static final Pattern PRICE = Pattern.compile("\\\"price\\\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
    private final int port;
    private volatile boolean running;
    private ServerSocket server;
    private final ExecutorService handlers = Executors.newFixedThreadPool(4);
    private Thread acceptThread;

    public DemoLabServer(int port) { this.port = port; }

    public synchronized void start() throws IOException {
        if (running) return;
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        running = true;
        acceptThread = new Thread(this::acceptLoop, "nexvary-lab-http");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = server.accept();
                s.setSoTimeout(5000);
                handlers.execute(() -> handle(s));
            } catch (IOException e) {
                if (running) System.err.println("NEXVARY lab accept error: " + e.getMessage());
            }
        }
    }

    public synchronized void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        if (acceptThread != null) acceptThread.interrupt();
        handlers.shutdownNow();
    }

    private void handle(Socket socket) {
        try (socket; InputStream in = new BufferedInputStream(socket.getInputStream()); OutputStream out = socket.getOutputStream()) {
            byte[] headerBytes = readHeaders(in);
            if (headerBytes == null) return;
            String headers = new String(headerBytes, StandardCharsets.ISO_8859_1);
            String[] lines = headers.split("\\r\\n");
            if (lines.length == 0) { respond(out, 400, "{\"ok\":false,\"error\":\"bad request\"}"); return; }
            String[] requestLine = lines[0].trim().split("\\s+");
            if (requestLine.length < 2 || requestLine[1].isEmpty()) { respond(out, 400, "{\"ok\":false,\"error\":\"bad request line\"}"); return; }
            int contentLength = 0;
            for (int i=1;i<lines.length;i++) {
                int p = lines[i].indexOf(':');
                if (p <= 0) continue;
                String name = lines[i].substring(0,p).trim().toLowerCase(Locale.ROOT);
                if (name.equals("content-length")) {
                    try { contentLength = Integer.parseInt(lines[i].substring(p+1).trim()); }
                    catch (NumberFormatException ex) { respond(out,400,"{\"ok\":false,\"error\":\"invalid content-length\"}"); return; }
                }
            }
            if (contentLength < 0 || contentLength > MAX_BODY_BYTES) { respond(out,413,"{\"ok\":false,\"error\":\"body too large\"}"); return; }
            byte[] bodyBytes = new byte[contentLength];
            int received = 0;
            while (received < contentLength) {
                int count = in.read(bodyBytes, received, contentLength - received);
                if (count < 0) break;
                received += count;
            }
            if (received != contentLength) { respond(out,400,"{\"ok\":false,\"error\":\"incomplete body\"}"); return; }
            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            String path = requestLine[1];
            if (path.equals("/api/order")) {
                Matcher m = PRICE.matcher(body);
                String price = m.find() ? m.group(1) : "100";
                respond(out,200,"{\"ok\":true,\"lab\":true,\"accepted_price\":"+price+",\"message\":\"Demo server intentionally trusts client input\"}");
            } else if (path.equals("/api/login")) {
                respond(out,200,"{\"ok\":true,\"token\":\"LAB-DEMO-TOKEN-ONLY\",\"role\":\"viewer\"}");
            } else {
                respond(out,404,"{\"ok\":false,\"error\":\"not found\"}");
            }
        } catch (Exception ignored) { }
    }

    private static byte[] readHeaders(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int state = 0;
        while (b.size() < MAX_HEADER_BYTES) {
            int x = in.read();
            if (x < 0) return b.size()==0 ? null : b.toByteArray();
            b.write(x);
            if (state==0 && x=='\r') state=1;
            else if (state==1 && x=='\n') state=2;
            else if (state==2 && x=='\r') state=3;
            else if (state==3 && x=='\n') return b.toByteArray();
            else state = x=='\r' ? 1 : 0;
        }
        throw new IOException("request headers exceed limit");
    }

    private static void respond(OutputStream out, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String reason = code==200?"OK":code==400?"Bad Request":code==404?"Not Found":code==413?"Payload Too Large":"Error";
        String hdr = "HTTP/1.1 " + code + " " + reason + "\r\nContent-Type: application/json; charset=utf-8\r\nX-NEXVARY-Lab: true\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n";
        out.write(hdr.getBytes(StandardCharsets.ISO_8859_1));
        out.write(bytes);
        out.flush();
    }
}
