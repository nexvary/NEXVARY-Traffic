package com.nexvary.trafficlab.core;
import java.util.*;
public final class CommandPalette {
 private static final List<String> COMMANDS=List.of("Intercept request","Open repeater","Export PCAP","Show TLS metadata","Run passive scan","Redact presenter secrets","Compare responses","Build evidence timeline","Learn API schema","Open demo scenario");
 public static List<String> search(String q){String x=q==null?"":q.toLowerCase();return COMMANDS.stream().filter(c->c.toLowerCase().contains(x)).toList();}
}