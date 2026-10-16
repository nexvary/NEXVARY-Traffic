package com.nexvary.trafficlab.core;

import java.util.*;

public final class VisualPacketMap {
    public record Link(String source, String destination, String protocol, int packets, long bytes) {}
    public record MapView(List<Link> links, long totalBytes) {}

    private VisualPacketMap() {}

    public static MapView fromHistory(List<HistoryEntry> history) {
        Map<String,int[]> counts = new LinkedHashMap<>();
        long total=0;
        for (HistoryEntry e: history) {
            String dst = hostOf(e.request);
            String proto = "HTTP";
            long bytes = e.request.body == null ? 0 : e.request.body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (e.response != null && e.response.body != null) bytes += e.response.body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            String key="Android Lab Client|"+dst+"|"+proto;
            int[] v=counts.computeIfAbsent(key,k->new int[]{0,0});
            v[0]++; v[1]+= (int)Math.min(Integer.MAX_VALUE,bytes); total+=bytes;
        }
        List<Link> out=new ArrayList<>();
        for (var en:counts.entrySet()) {
            String[] p=en.getKey().split("\\|",3); int[] v=en.getValue();
            out.add(new Link(p[0],p[1],p[2],v[0],v[1]));
        }
        return new MapView(List.copyOf(out),total);
    }

    private static String hostOf(LabRequest r) {
        String h=r.headers.getOrDefault("Host","127.0.0.1:18088");
        return h.isBlank()?"127.0.0.1:18088":h;
    }

    public static String ascii(MapView m) {
        if (m.links().isEmpty()) return "No packet-flow history yet.";
        StringBuilder b=new StringBuilder();
        for(Link l:m.links()) b.append("[").append(l.source()).append("] --").append(l.protocol()).append(" / ").append(l.packets()).append(" req / ").append(l.bytes()).append(" B--> [").append(l.destination()).append("]\n");
        b.append("Total observed payload bytes: ").append(m.totalBytes());
        return b.toString();
    }
}
