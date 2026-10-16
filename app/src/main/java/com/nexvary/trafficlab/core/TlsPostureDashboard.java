package com.nexvary.trafficlab.core;
import java.util.*;
public final class TlsPostureDashboard {
  public record Endpoint(String host,String protocol,String cipher,long daysRemaining,boolean trusted,List<String> notes){}
  public record Summary(int total,int healthy,int attention,List<String> notes){}
  private TlsPostureDashboard(){}
  public static Summary summarize(List<Endpoint> xs){int h=0,a=0;List<String> n=new ArrayList<>();for(Endpoint e:xs){boolean good=e.trusted&&e.daysRemaining>14&&!e.protocol.equalsIgnoreCase("TLSv1")&&!e.protocol.equalsIgnoreCase("TLSv1.1");if(good)h++;else{a++;n.add(e.host+": review TLS posture");}}return new Summary(xs.size(),h,a,List.copyOf(n));}
  public static String pretty(Summary s){return "TLS endpoints: "+s.total+"\nHealthy: "+s.healthy+"\nNeeds attention: "+s.attention+(s.notes.isEmpty()?"":"\n• "+String.join("\n• ",s.notes));}
}
