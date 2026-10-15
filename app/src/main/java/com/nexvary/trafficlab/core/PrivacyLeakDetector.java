package com.nexvary.trafficlab.core;
import java.util.*; import java.util.regex.*;
public final class PrivacyLeakDetector {
 private static final Pattern EMAIL=Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",Pattern.CASE_INSENSITIVE);
 private static final Pattern TOKEN=Pattern.compile("(?i)(authorization|api[_-]?key|token)\\s*[:=]\\s*[^,\\s\\\"]+");
 public static List<String> scan(LabRequest r){List<String> out=new ArrayList<>(); String s=r.toRaw("lab.local"); if(EMAIL.matcher(s).find())out.add("Possible e-mail identifier in request"); if(TOKEN.matcher(s).find())out.add("Possible credential/token in request"); if(r.path.startsWith("http://"))out.add("Plain HTTP target"); return out;}
}