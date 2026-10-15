package com.nexvary.trafficlab.core;
import java.util.*;
public final class LabExportBundle {
 public record Item(String name,String mime,String content,String sha256){}
 public static List<Item> build(String title,List<HistoryEntry> es){List<Item>o=new ArrayList<>();String md=LabReportBuilder.markdown(title,es), html=InteractiveReportBuilder.html(title,es), graph=ApiGraphBuilder.ascii(ApiGraphBuilder.build(es)), replay=SessionReplay.script(SessionReplay.from(es));o.add(item("report.md","text/markdown",md));o.add(item("report.html","text/html",html));o.add(item("api-graph.txt","text/plain",graph));o.add(item("replay.txt","text/plain",replay));return List.copyOf(o);}
 private static Item item(String n,String m,String c){return new Item(n,m,c,IntegrityManifest.sha256(c));}
 public static boolean verify(List<Item>is){for(Item i:is)if(!IntegrityManifest.sha256(i.content).equals(i.sha256))return false;return true;}
}
