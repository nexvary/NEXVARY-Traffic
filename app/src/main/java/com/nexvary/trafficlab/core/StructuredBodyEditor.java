package com.nexvary.trafficlab.core;

import java.util.*;

/**
 * Small dependency-free structured-body helper for the visual request editor.
 * It intentionally supports flat JSON objects used by the local lecture lab.
 * Nested JSON values are preserved as RAW values instead of being rewritten.
 */
public final class StructuredBodyEditor {
    public enum Kind { STRING, NUMBER, BOOLEAN, NULL, RAW }
    public record Field(String key, String value, Kind kind) {}

    private StructuredBodyEditor() {}

    public static List<Field> parseFlatJson(String json) {
        if (json == null) return List.of();
        String s = json.trim();
        if (s.length() < 2 || s.charAt(0) != '{' || s.charAt(s.length()-1) != '}') return List.of();
        List<String> parts = splitTopLevel(s.substring(1, s.length()-1), ',');
        List<Field> out = new ArrayList<>();
        for (String part : parts) {
            int colon = findTopLevelColon(part);
            if (colon < 0) return List.of();
            String k = part.substring(0, colon).trim();
            String v = part.substring(colon + 1).trim();
            if (!isQuoted(k)) return List.of();
            String key = unquote(k);
            if (isQuoted(v)) out.add(new Field(key, unquote(v), Kind.STRING));
            else if (v.equals("true") || v.equals("false")) out.add(new Field(key, v, Kind.BOOLEAN));
            else if (v.equals("null")) out.add(new Field(key, "", Kind.NULL));
            else if (v.matches("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")) out.add(new Field(key, v, Kind.NUMBER));
            else out.add(new Field(key, v, Kind.RAW));
        }
        return out;
    }

    public static String toJson(List<Field> fields) {
        StringBuilder b = new StringBuilder("{");
        for (int i=0;i<fields.size();i++) {
            if (i>0) b.append(',');
            Field f=fields.get(i);
            b.append(quote(f.key())).append(':');
            switch (f.kind()) {
                case STRING -> b.append(quote(f.value()));
                case NUMBER -> b.append(validNumber(f.value()) ? f.value() : "0");
                case BOOLEAN -> b.append(Boolean.parseBoolean(f.value()));
                case NULL -> b.append("null");
                case RAW -> b.append(f.value().isBlank() ? "null" : f.value());
            }
        }
        return b.append('}').toString();
    }

    public static List<Field> replace(List<Field> fields, String key, String newValue) {
        List<Field> out = new ArrayList<>();
        for (Field f: fields) out.add(f.key().equals(key) ? new Field(f.key(), newValue, f.kind()) : f);
        return out;
    }

    public static String incrementNumber(String value, double delta) {
        try {
            double d = Double.parseDouble(value) + delta;
            if (Math.rint(d) == d) return Long.toString((long)d);
            String s = Double.toString(d);
            return s.endsWith(".0") ? s.substring(0,s.length()-2) : s;
        } catch (Exception e) { return value; }
    }

    public static boolean validNumber(String s) {
        return s != null && s.matches("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");
    }

    private static int findTopLevelColon(String s) {
        boolean q=false, esc=false; int depth=0;
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if(q) { if(esc) esc=false; else if(c=='\\') esc=true; else if(c=='\"') q=false; continue; }
            if(c=='\"') { q=true; continue; }
            if(c=='{'||c=='[') depth++; else if(c=='}'||c==']') depth--; else if(c==':'&&depth==0) return i;
        }
        return -1;
    }

    private static List<String> splitTopLevel(String s, char sep) {
        List<String> out=new ArrayList<>(); boolean q=false, esc=false; int depth=0,start=0;
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if(q) { if(esc) esc=false; else if(c=='\\') esc=true; else if(c=='\"') q=false; continue; }
            if(c=='\"') {q=true; continue;}
            if(c=='{'||c=='[') depth++; else if(c=='}'||c==']') depth--; else if(c==sep&&depth==0){out.add(s.substring(start,i).trim());start=i+1;}
        }
        String tail=s.substring(start).trim(); if(!tail.isEmpty()) out.add(tail); return out;
    }

    private static boolean isQuoted(String s){return s.length()>=2&&s.charAt(0)=='\"'&&s.charAt(s.length()-1)=='\"';}
    private static String unquote(String s){
        String x=s.substring(1,s.length()-1); StringBuilder b=new StringBuilder(); boolean esc=false;
        for(int i=0;i<x.length();i++){char c=x.charAt(i); if(esc){switch(c){case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'b'->b.append('\b');case 'f'->b.append('\f');default->b.append(c);}esc=false;}else if(c=='\\')esc=true;else b.append(c);} if(esc)b.append('\\'); return b.toString();
    }
    private static String quote(String s){StringBuilder b=new StringBuilder("\""); for(char c:s.toCharArray()){switch(c){case '\\'->b.append("\\\\");case '\"'->b.append("\\\"");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->b.append(c);}} return b.append('\"').toString();}
}
