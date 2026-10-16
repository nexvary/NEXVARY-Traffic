package com.nexvary.trafficlab.core;
import java.util.*;
public final class DemoScenarioComposer {
  public record Step(int order,String title,String action,String expected,String teachingPoint){}
  private DemoScenarioComposer(){}
  public static List<Step> standard(){return List.of(new Step(1,"Baseline","Send the unmodified localhost request","HTTP 200","Establish a known-good baseline"),new Step(2,"Intercept","Pause the next local request","Request visible","Explain method, headers and body"),new Step(3,"Modify","Change the demo client-controlled value","Diff appears","Show why server-side authority matters"),new Step(4,"Forward","Send the modified request to 127.0.0.1","Response captured","Observe behavior safely in the lab"),new Step(5,"Correlate","Open timeline, risk and data journey","Findings grouped","Connect evidence to remediation"),new Step(6,"Remediate","Switch to What-If and Regression Gate","Gate passes after fix","End with defensive engineering"));}
  public static String script(List<Step> xs){StringBuilder b=new StringBuilder();for(Step s:xs)b.append(s.order).append(". ").append(s.title).append("\n   ACTION: ").append(s.action).append("\n   EXPECT: ").append(s.expected).append("\n   TEACH: ").append(s.teachingPoint).append("\n");return b.toString();}
}
