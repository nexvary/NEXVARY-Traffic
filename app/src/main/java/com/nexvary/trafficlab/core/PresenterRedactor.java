package com.nexvary.trafficlab.core;

import java.util.regex.*;

public class PresenterRedactor {
    private static final Pattern AUTH=Pattern.compile("(?im)^(Authorization\\s*:\\s*)(.+)$");
    private static final Pattern COOKIE=Pattern.compile("(?im)^(Cookie\\s*:\\s*)(.+)$");
    private static final Pattern JSON=Pattern.compile("(?i)(\\\"(?:password|token|secret|api[_-]?key)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");
    public static String redact(String s){ if(s==null)return ""; s=AUTH.matcher(s).replaceAll("$1<REDACTED>"); s=COOKIE.matcher(s).replaceAll("$1<REDACTED>"); s=JSON.matcher(s).replaceAll("$1<REDACTED>$2"); return s; }
}
