package com.nexvary.trafficlab.core;
import java.util.*;
public final class DemoScenarioEngine {
 public record Scenario(String title,String lesson,LabRequest request){}
 public static List<Scenario> builtIns(){List<Scenario> a=new ArrayList<>(); a.add(new Scenario("Client-side price trust","Server must calculate authoritative price",req("POST","/api/order","{\\\"item\\\":\\\"LAB-1\\\",\\\"price\\\":100}"))); a.add(new Scenario("Sensitive header exposure","Mask secrets during presentations",req("GET","/api/profile",""))); return a;}
 static LabRequest req(String m,String p,String b){var r=new LabRequest();r.method=m;r.path=p;r.body=b;r.headers.put("Content-Type","application/json");return r;}
}