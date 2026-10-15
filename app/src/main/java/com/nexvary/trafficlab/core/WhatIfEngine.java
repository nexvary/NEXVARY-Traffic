package com.nexvary.trafficlab.core;

public class WhatIfEngine {
    public static String analyze(LabRequest r){ String b=r.body==null?"":r.body.toLowerCase(); StringBuilder s=new StringBuilder();
        if(b.contains("\"price\"")) s.append("• Price should be derived from the server-side product catalog, not accepted as authoritative client input.\n");
        if(b.contains("\"role\"")||b.contains("\"isadmin\"")) s.append("• Authorization roles must come from the authenticated server-side session, not from request JSON.\n");
        if(b.contains("\"user_id\"")||b.contains("\"userid\"")) s.append("• Object access should be checked against the authenticated user to prevent IDOR/BOLA.\n");
        if(s.length()==0) s.append("• Validate type, range, ownership and authorization for every security-sensitive field on the server.\n"); return s.toString(); }
}
