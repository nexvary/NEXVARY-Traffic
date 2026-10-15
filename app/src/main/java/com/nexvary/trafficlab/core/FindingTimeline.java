package com.nexvary.trafficlab.core;
import java.util.*;
public final class FindingTimeline {
 public record Point(long timestamp,int risk,String label){}
 public static List<Point> build(List<HistoryEntry> es){List<Point>o=new ArrayList<>();for(var e:es)o.add(new Point(e.timestamp,RiskEngine.score(e.findings),e.request.method+" "+e.request.path));o.sort(Comparator.comparingLong(Point::timestamp));return o;}
}