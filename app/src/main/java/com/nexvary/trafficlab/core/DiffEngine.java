package com.nexvary.trafficlab.core;

import java.util.*;

public class DiffEngine {
    public record DiffLine(String type,String left,String right){}
    public static List<DiffLine> lineDiff(String a,String b){
        String[] x=(a==null?"":a).split("\\R",-1), y=(b==null?"":b).split("\\R",-1); int n=Math.max(x.length,y.length); List<DiffLine> out=new ArrayList<>();
        for(int i=0;i<n;i++){ String l=i<x.length?x[i]:"", r=i<y.length?y[i]:""; out.add(new DiffLine(l.equals(r)?"SAME":(l.isEmpty()?"ADD":r.isEmpty()?"REMOVE":"CHANGE"),l,r)); }
        return out;
    }
    public static String pretty(String a,String b){ StringBuilder s=new StringBuilder(); for(DiffLine d:lineDiff(a,b)){ if(!d.type().equals("SAME")) s.append(d.type()).append("\n- ").append(d.left()).append("\n+ ").append(d.right()).append("\n\n"); } return s.length()==0?"No differences":s.toString(); }
}
