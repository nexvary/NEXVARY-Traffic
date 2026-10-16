package com.nexvary.trafficlab.core;
import java.util.*;
public final class FindingRemediationGraph {
  public record Edge(String finding,String remediation,String verification){}
  private FindingRemediationGraph(){}
  public static List<Edge> build(List<Finding> findings){List<Edge> out=new ArrayList<>();if(findings==null)return out;for(Finding f:findings){String t=f.title().toLowerCase();String r=t.contains("price")||t.contains("client")?"Validate authoritative values on the server":t.contains("http")?"Require HTTPS and secure transport":t.contains("cookie")?"Apply Secure/HttpOnly/SameSite cookie attributes":t.contains("token")?"Minimize token exposure and rotate compromised credentials":"Apply the recommended control and add a regression test";out.add(new Edge(f.title(),r,"Replay the authorized lab scenario and confirm the finding is gone"));}return out;}
  public static String pretty(List<Edge> xs){if(xs.isEmpty())return "No findings to map.";StringBuilder b=new StringBuilder();for(Edge e:xs)b.append("FINDING: ").append(e.finding).append("\n  ↓ REMEDIATE: ").append(e.remediation).append("\n  ↓ VERIFY: ").append(e.verification).append("\n\n");return b.toString();}
}
