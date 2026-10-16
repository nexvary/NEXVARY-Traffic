package com.nexvary.trafficlab.core;
import java.util.*;
public final class SecurityRegressionGate {
  public record Result(boolean pass,int baselineRisk,int candidateRisk,List<String> reasons){}
  private SecurityRegressionGate(){}
  public static Result compare(List<Finding> baseline,List<Finding> candidate){int a=RiskEngine.score(baseline),b=RiskEngine.score(candidate);List<String> r=new ArrayList<>();if(b>a)r.add("Risk score increased from "+a+" to "+b);long ah=baseline==null?0:baseline.stream().filter(f->"HIGH".equalsIgnoreCase(f.severity())||"CRITICAL".equalsIgnoreCase(f.severity())).count();long bh=candidate==null?0:candidate.stream().filter(f->"HIGH".equalsIgnoreCase(f.severity())||"CRITICAL".equalsIgnoreCase(f.severity())).count();if(bh>ah)r.add("New high-severity findings appeared");if(r.isEmpty())r.add("No security regression detected");return new Result(r.size()==1&&r.get(0).startsWith("No security"),a,b,List.copyOf(r));}
}
