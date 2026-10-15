package com.nexvary.trafficlab.core;

import java.time.Instant;
import java.util.*;

public final class CaseWorkspace {
    public record Note(long timestamp,String text) {}
    public record CaseFile(String id,String name,long createdAt,Set<Long> historyIds,List<Note> notes) {}
    private final Map<String,Mutable> cases=new LinkedHashMap<>();
    private static final class Mutable{String id,name;long created;Set<Long> ids=new LinkedHashSet<>();List<Note> notes=new ArrayList<>();}

    public synchronized String create(String name){Mutable m=new Mutable();m.id="CASE-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);m.name=name==null||name.isBlank()?"Untitled Lab Case":name.trim();m.created=System.currentTimeMillis();cases.put(m.id,m);return m.id;}
    public synchronized void attach(String id,long historyId){Mutable m=req(id);m.ids.add(historyId);}
    public synchronized void note(String id,String text){Mutable m=req(id);m.notes.add(new Note(System.currentTimeMillis(),text==null?"":text));}
    public synchronized List<CaseFile> all(){List<CaseFile> out=new ArrayList<>();for(Mutable m:cases.values())out.add(new CaseFile(m.id,m.name,m.created,Set.copyOf(m.ids),List.copyOf(m.notes)));return List.copyOf(out);}
    public synchronized String summary(){if(cases.isEmpty())return "No cases yet.";StringBuilder b=new StringBuilder();for(CaseFile c:all())b.append(c.id()).append(" • ").append(c.name()).append(" • sessions=").append(c.historyIds().size()).append(" • notes=").append(c.notes().size()).append(" • ").append(Instant.ofEpochMilli(c.createdAt())).append('\n');return b.toString();}
    private Mutable req(String id){Mutable m=cases.get(id);if(m==null)throw new IllegalArgumentException("unknown case: "+id);return m;}
}
