package com.nexvary.trafficlab.core;
import java.util.*;
public final class RegressionComparator {
 public static List<String> compare(LabResponse a, LabResponse b){List<String> o=new ArrayList<>(); if(a==null||b==null){o.add("Missing baseline or candidate response");return o;} if(a.statusCode!=b.statusCode)o.add("Status changed: "+a.statusCode+" → "+b.statusCode); if(!Objects.equals(a.body,b.body))o.add("Response body changed ("+a.body.length()+" → "+b.body.length()+" chars)"); if(o.isEmpty())o.add("No observable regression"); return o;}
}