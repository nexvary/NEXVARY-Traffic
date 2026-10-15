package com.nexvary.trafficlab.core;

import java.util.*;

/** Converts technical history into a chronological lecture narrative. */
public final class SessionStoryEngine {
    public record Step(int order, String title, String detail, int risk) {}
    private SessionStoryEngine() {}

    public static List<Step> build(List<HistoryEntry> history) {
        if (history == null || history.isEmpty()) return List.of();
        List<HistoryEntry> copy=new ArrayList<>(history); Collections.reverse(copy);
        List<Step> out=new ArrayList<>(); int n=1;
        for(HistoryEntry e:copy){
            int risk=RiskEngine.score(e.findings);
            String verb=switch(e.request.method.toUpperCase(Locale.ROOT)){case "POST"->"Submitted";case "PUT","PATCH"->"Changed";case "DELETE"->"Removed";default->"Requested";};
            String title=verb+" "+friendly(e.request.path);
            String detail=e.request.method+" "+e.request.path+" → "+(e.response==null?"no response":e.response.statusCode+" in "+e.response.elapsedMs+" ms")+(risk>0?" • risk "+risk+"/100":" • no finding");
            out.add(new Step(n++,title,detail,risk));
        }
        return List.copyOf(out);
    }

    public static String pretty(List<Step> steps){if(steps==null||steps.isEmpty())return "No story yet. Run the local lecture demo first.";StringBuilder b=new StringBuilder();for(Step s:steps)b.append(s.order()).append(". ").append(s.title()).append("\n   ").append(s.detail()).append("\n");return b.toString().trim();}
    private static String friendly(String path){if(path==null||path.isBlank())return "API endpoint";String p=path.toLowerCase(Locale.ROOT);if(p.contains("login"))return "Login";if(p.contains("order"))return "Order";if(p.contains("profile"))return "Profile";if(p.contains("payment"))return "Payment";return path;}
}
