package com.nexvary.trafficlab.core;

import java.util.*;

public class HistoryEntry {
    public final long id;
    public final long timestamp;
    public final LabRequest request;
    public final LabResponse response;
    public final List<Finding> findings;
    public final Set<String> tags = new LinkedHashSet<>();

    public HistoryEntry(long id, long timestamp, LabRequest request, LabResponse response, List<Finding> findings) {
        this.id=id; this.timestamp=timestamp; this.request=request; this.response=response; this.findings=findings==null?List.of():List.copyOf(findings);
    }
    public String summary(){ return "#"+id+" "+request.method+" "+request.path+" → "+(response==null?"-":response.statusCode)+" [risk "+RiskEngine.score(findings)+"]"; }
}
