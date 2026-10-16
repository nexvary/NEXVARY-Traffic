package com.nexvary.trafficlab.core;

import java.util.*;

public final class LiveEndpointRadar {
    public record Endpoint(String route, int hits, int maxRisk, long avgMs, String state) {}
    private LiveEndpointRadar() {}

    public static List<Endpoint> summarize(List<HistoryEntry> history) {
        class Acc { int n,r; long ms; }
        Map<String,Acc> m=new LinkedHashMap<>();
        for(HistoryEntry e:history){
            String route=e.request.method+" "+normalize(e.request.path); Acc a=m.computeIfAbsent(route,k->new Acc());
            a.n++; a.r=Math.max(a.r,RiskEngine.score(e.findings)); if(e.response!=null)a.ms+=e.response.elapsedMs;
        }
        List<Endpoint> out=new ArrayList<>();
        for(var en:m.entrySet()){
            Acc a=en.getValue(); long avg=a.n==0?0:a.ms/a.n; String state=a.r>=70?"HIGH":a.r>=35?"WATCH":"NORMAL";
            out.add(new Endpoint(en.getKey(),a.n,a.r,avg,state));
        }
        out.sort(Comparator.comparingInt(Endpoint::maxRisk).reversed().thenComparing(Endpoint::route));
        return List.copyOf(out);
    }

    private static String normalize(String p){ return p==null||p.isBlank()?"/":p.replaceAll("/\\d+(?=/|$)","/{id}"); }

    public static String radarText(List<Endpoint> es){
        if(es.isEmpty())return "Radar idle — generate a local lab session.";
        StringBuilder b=new StringBuilder(); int i=1;
        for(Endpoint e:es) b.append(i++).append(". ").append(e.state()).append(" • ").append(e.route()).append(" • hits=").append(e.hits()).append(" • risk=").append(e.maxRisk()).append(" • avg=").append(e.avgMs()).append("ms\n");
        return b.toString();
    }
}
