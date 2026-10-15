package com.nexvary.trafficlab;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Backend-neutral email recovery client. No SMTP/API secret is stored in the APK. */
public final class RecoveryClient {
    public record Result(boolean ok, String message) {}
    private RecoveryClient() {}

    public static boolean isConfigured() {
        return BuildConfig.RECOVERY_ENDPOINT != null && BuildConfig.RECOVERY_ENDPOINT.trim().startsWith("https://");
    }

    public static Result request(String email) {
        return post("/request", "{\"email\":\""+esc(email)+"\"}");
    }

    public static Result verify(String email, String code) {
        return post("/verify", "{\"email\":\""+esc(email)+"\",\"code\":\""+esc(code)+"\"}");
    }

    private static Result post(String path, String json) {
        if (!isConfigured()) return new Result(false, "RECOVERY_NOT_CONFIGURED");
        HttpURLConnection c=null;
        try {
            URL u=new URL(trimSlash(BuildConfig.RECOVERY_ENDPOINT)+path);
            c=(HttpURLConnection)u.openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(8000); c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setRequestProperty("Content-Type","application/json; charset=utf-8");
            c.setRequestProperty("Accept","application/json");
            byte[] b=json.getBytes(StandardCharsets.UTF_8); c.setFixedLengthStreamingMode(b.length);
            try(OutputStream out=c.getOutputStream()){out.write(b);} int code=c.getResponseCode();
            return new Result(code>=200&&code<300, "HTTP_"+code);
        } catch(Exception e){ return new Result(false, e.getClass().getSimpleName()); }
        finally { if(c!=null)c.disconnect(); }
    }
    private static String trimSlash(String s){while(s.endsWith("/"))s=s.substring(0,s.length()-1);return s;}
    private static String esc(String s){return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
}
