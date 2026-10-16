package com.nexvary.trafficlab.core;
import java.util.*;
public final class ApiGraphBuilder {
 public record Node(String id,String type,int count){}
 public record Edge(String from,String to,String label,int count){}
 public record Graph(List<Node> nodes,List<Edge> edges){}
 public static Graph build(List<HistoryEntry> entries){
  Map<String,Integer> nc=new LinkedHashMap<>(), ec=new LinkedHashMap<>();
  for(HistoryEntry e:entries){
   String app="Android Client", route=e.request.method+" "+ApiSchemaLearner.normalize(e.request.path), status="HTTP "+(e.response==null?"-":e.response.statusCode);
   nc.merge("client:"+app,1,Integer::sum); nc.merge("route:"+route,1,Integer::sum); nc.merge("status:"+status,1,Integer::sum);
   ec.merge("client:"+app+"\u0000route:"+route+"\u0000calls",1,Integer::sum); ec.merge("route:"+route+"\u0000status:"+status+"\u0000returns",1,Integer::sum);
  }
  List<Node> ns=new ArrayList<>(); for(var x:nc.entrySet()){String[] p=x.getKey().split(":",2);ns.add(new Node(p[1],p[0],x.getValue()));}
  List<Edge> es=new ArrayList<>(); for(var x:ec.entrySet()){String[] p=x.getKey().split("\u0000",3);es.add(new Edge(p[0],p[1],p[2],x.getValue()));}
  return new Graph(List.copyOf(ns),List.copyOf(es));
 }
 public static String ascii(Graph g){StringBuilder b=new StringBuilder();for(Edge e:g.edges)b.append('[').append(e.from).append("] --").append(e.label).append("×").append(e.count).append("--> [").append(e.to).append("]\n");return b.toString();}
}
