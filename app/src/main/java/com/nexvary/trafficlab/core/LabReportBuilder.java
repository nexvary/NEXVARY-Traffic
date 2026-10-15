package com.nexvary.trafficlab.core;
import java.util.*;
public final class LabReportBuilder {
 public static String markdown(String caseName,List<HistoryEntry> es){StringBuilder b=new StringBuilder("# NEXVARY Traffic Lab — ").append(caseName).append("\\n\\n"); b.append("Sessions: ").append(es.size()).append("\\n\\n"); for(var e:es){b.append("## ").append(e.summary()).append("\\n"); for(var f:e.findings)b.append("- ").append(f.severity()).append(": ").append(f.title()).append("\\n");} return b.toString();}
}