package com.nexvary.trafficlab.core;

import java.util.*;

/** Compact before/after comparison for presenter-friendly request changes. */
public final class RequestCompare {
    public record Change(String area, String key, String before, String after) {}
    private RequestCompare() {}

    public static List<Change> compare(LabRequest a, LabRequest b) {
        if (a == null) a = new LabRequest();
        if (b == null) b = new LabRequest();
        List<Change> out = new ArrayList<>();
        if (!Objects.equals(a.method,b.method)) out.add(new Change("request","method",a.method,b.method));
        if (!Objects.equals(a.path,b.path)) out.add(new Change("request","path",a.path,b.path));
        Set<String> headerKeys = new LinkedHashSet<>(); headerKeys.addAll(a.headers.keySet()); headerKeys.addAll(b.headers.keySet());
        for (String k:headerKeys) {
            String av = getIgnoreCase(a.headers,k), bv=getIgnoreCase(b.headers,k);
            if (!Objects.equals(av,bv)) out.add(new Change("header",k,av,bv));
        }
        Map<String,StructuredBodyEditor.Field> am=map(StructuredBodyEditor.parseFlatJson(a.body));
        Map<String,StructuredBodyEditor.Field> bm=map(StructuredBodyEditor.parseFlatJson(b.body));
        if (!am.isEmpty() || !bm.isEmpty()) {
            Set<String> keys=new LinkedHashSet<>(); keys.addAll(am.keySet()); keys.addAll(bm.keySet());
            for(String k:keys){ String av=am.containsKey(k)?am.get(k).value():null; String bv=bm.containsKey(k)?bm.get(k).value():null; if(!Objects.equals(av,bv)) out.add(new Change("body",k,av,bv)); }
        } else if (!Objects.equals(a.body,b.body)) out.add(new Change("body","raw",a.body,b.body));
        return List.copyOf(out);
    }

    public static String pretty(List<Change> changes) {
        if (changes == null || changes.isEmpty()) return "No changes yet.";
        StringBuilder b=new StringBuilder();
        for(Change c:changes) b.append(c.area()).append(" • ").append(c.key()).append("\n  ORIGINAL: ").append(show(c.before())).append("\n  MODIFIED: ").append(show(c.after())).append("\n\n");
        return b.toString().trim();
    }
    private static String show(String s){return s==null?"(missing)":s;}
    private static String getIgnoreCase(Map<String,String> m,String k){for(var e:m.entrySet())if(e.getKey().equalsIgnoreCase(k))return e.getValue();return null;}
    private static Map<String,StructuredBodyEditor.Field> map(List<StructuredBodyEditor.Field> fs){Map<String,StructuredBodyEditor.Field> m=new LinkedHashMap<>();for(var f:fs)m.put(f.key(),f);return m;}
}
