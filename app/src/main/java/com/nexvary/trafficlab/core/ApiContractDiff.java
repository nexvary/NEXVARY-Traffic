package com.nexvary.trafficlab.core;

import java.util.*;

public final class ApiContractDiff {
    public record Change(String kind,String field,String before,String after) {}
    private ApiContractDiff() {}

    public static List<Change> compare(Map<String,String> baseline,Map<String,String> current){
        Set<String> keys=new TreeSet<>();keys.addAll(baseline.keySet());keys.addAll(current.keySet());List<Change> out=new ArrayList<>();
        for(String k:keys){String a=baseline.get(k),b=current.get(k);if(a==null)out.add(new Change("ADDED",k,"-",b));else if(b==null)out.add(new Change("REMOVED",k,a,"-"));else if(!Objects.equals(a,b))out.add(new Change("CHANGED",k,a,b));}
        return List.copyOf(out);
    }
    public static Map<String,String> infer(LabRequest r,LabResponse p){Map<String,String> m=new LinkedHashMap<>();m.put("method",r.method);m.put("path",r.path);m.put("request.content-type",header(r.headers,"Content-Type"));m.put("request.body.present",String.valueOf(r.body!=null&&!r.body.isBlank()));if(p!=null){m.put("response.status",String.valueOf(p.statusCode));m.put("response.body.present",String.valueOf(p.body!=null&&!p.body.isBlank()));}return m;}
    private static String header(Map<String,String> h,String name){for(var e:h.entrySet())if(e.getKey().equalsIgnoreCase(name))return e.getValue();return "-";}
    public static String pretty(List<Change> c){if(c.isEmpty())return "No contract differences.";StringBuilder b=new StringBuilder();for(Change x:c)b.append(x.kind()).append(" • ").append(x.field()).append(": ").append(x.before()).append(" → ").append(x.after()).append('\n');return b.toString();}
}
