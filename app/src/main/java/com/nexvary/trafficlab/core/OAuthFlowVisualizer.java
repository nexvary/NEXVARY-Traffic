package com.nexvary.trafficlab.core;

import java.util.*;

public final class OAuthFlowVisualizer {
    public record Step(int order,String actor,String action,String securityNote) {}
    private OAuthFlowVisualizer() {}

    public static List<Step> authorizationCodePkce(){ return List.of(
        new Step(1,"Mobile App","Create code_verifier + code_challenge","Keep verifier on device; never send it in the authorization request."),
        new Step(2,"Mobile App → Authorization Server","Open /authorize with challenge + state","Validate returned state to reduce request-forgery risk."),
        new Step(3,"User / Authorization Server","Authenticate and grant consent","Credentials stay with the authorization server."),
        new Step(4,"Authorization Server → App","Return short-lived authorization code","Code alone is insufficient without the verifier."),
        new Step(5,"App → Token Endpoint","Exchange code + code_verifier","Use TLS and exact redirect URI matching."),
        new Step(6,"Token Endpoint → App","Return access token / optional refresh token","Store tokens in protected app storage and minimize lifetime."),
        new Step(7,"App → Resource API","Send access token","API validates issuer, audience, expiry and signature.")
    ); }
    public static String ascii(List<Step> s){StringBuilder b=new StringBuilder();for(Step x:s)b.append(x.order()).append(". [").append(x.actor()).append("] ").append(x.action()).append("\n   ↳ ").append(x.securityNote()).append("\n");return b.toString();}
}
