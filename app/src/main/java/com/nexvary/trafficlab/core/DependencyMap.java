package com.nexvary.trafficlab.core;
import java.net.URI; import java.util.*;
public final class DependencyMap {
  public record Dependency(String host,int calls,Set<String> routes,boolean thirdParty){}
  private DependencyMap(){}
  public static List<Dependency> build(List<HistoryEntry> entries,String firstPartyHost){
    Map<String,List<HistoryEntry>> m=new LinkedHashMap<>(); if(entries!=null)for(HistoryEntry e:entries){String host=e.request.headers.getOrDefault("Host",firstPartyHost==null?"localhost":firstPartyHost);try{if(e.request.path.startsWith("http"))host=URI.create(e.request.path).getHost();}catch(Exception ignored){}m.computeIfAbsent(host,k->new ArrayList<>()).add(e);} List<Dependency> out=new ArrayList<>();
    for(var x:m.entrySet()){Set<String> routes=new LinkedHashSet<>();for(var e:x.getValue())routes.add(e.request.path);boolean tp=firstPartyHost!=null&&!x.getKey().equalsIgnoreCase(firstPartyHost);out.add(new Dependency(x.getKey(),x.getValue().size(),routes,tp));}return out;
  }
  public static String pretty(List<Dependency> xs){StringBuilder b=new StringBuilder();for(Dependency d:xs)b.append(d.thirdParty?"THIRD-PARTY  ":"FIRST-PARTY  ").append(d.host).append(" • calls=").append(d.calls).append(" • routes=").append(d.routes.size()).append('\n');return b.toString();}
}
