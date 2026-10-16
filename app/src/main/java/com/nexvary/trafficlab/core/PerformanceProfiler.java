package com.nexvary.trafficlab.core;
public final class PerformanceProfiler {
 public record Sample(long totalMs,int requestBytes,int responseBytes){ public String grade(){return totalMs<100?"A":totalMs<300?"B":totalMs<800?"C":"D";} }
 public static Sample profile(LabRequest q,LabResponse r,long ms){return new Sample(ms,q.toRaw("lab.local").length(),r==null?0:r.body.length());}
}