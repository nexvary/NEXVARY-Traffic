package com.nexvary.trafficlab.core;
import java.util.*; import java.util.regex.*;
public final class ApiSchemaLearner {
 private static final Pattern KEY=Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:");
 public static Map<String,Set<String>> learn(List<HistoryEntry> es){Map<String,Set<String>> m=new LinkedHashMap<>(); for(var e:es){String route=e.request.method+" "+normalize(e.request.path); Set<String>s=m.computeIfAbsent(route,k->new LinkedHashSet<>()); var x=KEY.matcher(e.request.body);while(x.find())s.add(x.group(1));}return m;}
 static String normalize(String p){return p.replaceAll("/\\d+(?=/|$)","/{id}");}
}