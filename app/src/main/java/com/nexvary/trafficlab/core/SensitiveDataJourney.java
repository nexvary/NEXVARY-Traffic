package com.nexvary.trafficlab.core;
import java.util.*; import java.util.regex.*;
public final class SensitiveDataJourney {
  public record Hop(long historyId,String route,Set<String> categories,int risk){}
  private static final Pattern EMAIL=Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",Pattern.CASE_INSENSITIVE);
  private SensitiveDataJourney(){}
  public static List<Hop> trace(List<HistoryEntry> entries){List<Hop> out=new ArrayList<>();if(entries==null)return out;for(HistoryEntry e:entries){String x=(e.request.body==null?"":e.request.body)+" "+e.request.headers;Set<String> c=new LinkedHashSet<>();if(EMAIL.matcher(x).find())c.add("email");if(x.toLowerCase().matches(".*(password|passwd|pwd).*"))c.add("credential-field");if(x.toLowerCase().matches(".*(token|authorization|bearer).*"))c.add("token/auth");if(x.toLowerCase().matches(".*(phone|mobile).*"))c.add("phone-field");if(!c.isEmpty())out.add(new Hop(e.id,e.request.method+" "+e.request.path,c,RiskEngine.score(e.findings)));}return out;}
  public static String pretty(List<Hop> xs){if(xs.isEmpty())return "No sensitive-data categories observed in current lab history.";StringBuilder b=new StringBuilder();for(Hop h:xs)b.append('#').append(h.historyId).append(" → ").append(h.route).append(" → ").append(h.categories).append(" • risk ").append(h.risk).append('\n');return b.toString();}
}
