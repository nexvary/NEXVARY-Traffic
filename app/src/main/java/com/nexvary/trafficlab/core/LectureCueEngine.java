package com.nexvary.trafficlab.core;
import java.util.*;
public final class LectureCueEngine {
 public record Cue(int order,String title,String action,String talkingPoint){}
 public static List<Cue> standard(){return List.of(
  new Cue(1,"Baseline","Send the original localhost request","Show normal client/server flow"),
  new Cue(2,"Intercept","Pause before forwarding","Explain that requests are data, not trust"),
  new Cue(3,"Modify","Change the demo price field","Demonstrate client-controlled input"),
  new Cue(4,"Compare","Open Before/After Diff","Make the exact mutation visible"),
  new Cue(5,"Defend","Open What-If simulator","Explain server-side authoritative validation"),
  new Cue(6,"Preserve","Add the session to Evidence","Show integrity and auditability") );}
 public static String prompt(int index){List<Cue> c=standard();if(index<0||index>=c.size())return "Lecture complete";Cue x=c.get(index);return x.order()+". "+x.title()+" — "+x.action()+"\n"+x.talkingPoint();}
}
