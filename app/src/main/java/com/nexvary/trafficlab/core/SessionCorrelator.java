package com.nexvary.trafficlab.core;
import java.util.*;
public final class SessionCorrelator {
 public static class Session { public final String key; public final List<HistoryEntry> entries=new ArrayList<>(); Session(String k){key=k;} public int maxRisk(){int m=0; for(var e:entries)m=Math.max(m,RiskEngine.score(e.findings));return m;} }
 public static List<Session> group(List<HistoryEntry> entries){ Map<String,Session> m=new LinkedHashMap<>(); for(var e:entries){String h=e.request.headers.getOrDefault("Host","local"); String k=h+"|"+e.request.headers.getOrDefault("X-Demo-Session","default"); m.computeIfAbsent(k,Session::new).entries.add(e);} return new ArrayList<>(m.values()); }
}