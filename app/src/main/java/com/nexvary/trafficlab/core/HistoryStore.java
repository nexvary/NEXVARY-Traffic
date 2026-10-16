package com.nexvary.trafficlab.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class HistoryStore {
    private final AtomicLong seq = new AtomicLong();
    private final List<HistoryEntry> entries = new ArrayList<>();
    public synchronized HistoryEntry add(LabRequest req, LabResponse res, List<Finding> findings){
        HistoryEntry e = new HistoryEntry(seq.incrementAndGet(), System.currentTimeMillis(), req.copy(), res, findings);
        entries.add(0,e); return e;
    }
    public synchronized List<HistoryEntry> all(){ return List.copyOf(entries); }
    public synchronized List<HistoryEntry> search(String q){
        if(q==null||q.isBlank()) return all(); String n=q.toLowerCase(Locale.ROOT); List<HistoryEntry> out=new ArrayList<>();
        for(HistoryEntry e:entries){ String raw=e.request.toRaw("").toLowerCase(Locale.ROOT); String rr=e.response==null?"":e.response.raw.toLowerCase(Locale.ROOT); if(raw.contains(n)||rr.contains(n)||e.tags.toString().toLowerCase(Locale.ROOT).contains(n)) out.add(e); }
        return out;
    }
    public synchronized void tag(long id,String tag){ for(HistoryEntry e:entries) if(e.id==id && tag!=null && !tag.isBlank()) e.tags.add(tag.trim()); }
    public synchronized void clear(){ entries.clear(); }
}
