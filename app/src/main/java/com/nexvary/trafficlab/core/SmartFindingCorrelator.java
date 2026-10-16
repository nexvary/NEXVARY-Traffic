package com.nexvary.trafficlab.core;

import java.util.*;

public final class SmartFindingCorrelator {
    public record Cluster(String key,int occurrences,int highestRisk,List<String> evidence,String explanation) {}
    private SmartFindingCorrelator() {}

    public static List<Cluster> correlate(List<HistoryEntry> history){
        class A{int n,r;Set<String> ev=new LinkedHashSet<>();}
        Map<String,A> m=new LinkedHashMap<>();
        for(HistoryEntry e:history)for(Finding f:e.findings){String key=f.title().trim().toLowerCase(Locale.ROOT);A a=m.computeIfAbsent(key,k->new A());a.n++;a.r=Math.max(a.r,weight(f.severity()));a.ev.add(e.request.method+" "+e.request.path);}
        List<Cluster> out=new ArrayList<>();for(var en:m.entrySet()){A a=en.getValue();out.add(new Cluster(en.getKey(),a.n,a.r,List.copyOf(a.ev),a.n>1?"Repeated finding across multiple observations; prioritize root-cause remediation.":"Single observed finding; verify with another authorized lab run."));}
        out.sort(Comparator.comparingInt(Cluster::highestRisk).reversed().thenComparingInt(Cluster::occurrences).reversed());return List.copyOf(out);
    }
    private static int weight(String s){return switch(s.toUpperCase(Locale.ROOT)){case "CRITICAL"->100;case "HIGH"->80;case "MEDIUM"->50;case "LOW"->20;default->10;};}
    public static String pretty(List<Cluster> cs){if(cs.isEmpty())return "No findings to correlate yet.";StringBuilder b=new StringBuilder();for(Cluster c:cs)b.append(c.highestRisk()).append(" • ").append(c.key()).append(" • occurrences=").append(c.occurrences()).append("\n   evidence=").append(String.join(", ",c.evidence())).append("\n   ").append(c.explanation()).append("\n");return b.toString();}
}
