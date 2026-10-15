package com.nexvary.trafficlab.core;
import java.util.*;
public final class RequestMutationMatrix {
 public record Variant(String label,LabRequest request,String learningGoal){}
 public static List<Variant> safeLectureVariants(LabRequest base){
  List<Variant> o=new ArrayList<>(); o.add(new Variant("Baseline",base.copy(),"Establish the expected lab response"));
  LabRequest a=base.copy(); a.body=a.body.replace("\"price\":100","\"price\":1"); o.add(new Variant("Client-controlled price",a,"Show why the server must calculate authoritative price"));
  LabRequest b=base.copy(); b.headers.put("X-NEXVARY-Demo","modified"); o.add(new Variant("Header variation",b,"Observe server handling of non-authoritative metadata"));
  LabRequest c=base.copy(); c.body=c.body.replace("\"qty\":1","\"qty\":2"); o.add(new Variant("Quantity variation",c,"Compare request/response behavior"));
  return List.copyOf(o);
 }
 public static String table(List<Variant> vs){StringBuilder b=new StringBuilder("Variant | Method | Path | Goal\n");for(Variant v:vs)b.append(v.label).append(" | ").append(v.request.method).append(" | ").append(v.request.path).append(" | ").append(v.learningGoal).append('\n');return b.toString();}
}
