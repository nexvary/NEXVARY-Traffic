package com.nexvary.trafficlab.core;
import java.net.*;

/** Explicit HTTPS origin and request target; reject credentials and malformed authorities. */
public record LabTarget(String url,String host,int port,String path){
    public static LabTarget parse(String value){
        try{
            String raw=value.trim();if(!raw.contains("://"))raw="https://"+raw;
            URI uri=new URI(new URI(raw).toASCIIString());String host=uri.getHost();int port=uri.getPort()==-1?443:uri.getPort();
            if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||!host.matches("[A-Za-z0-9][A-Za-z0-9.-]{0,252}")||uri.getRawUserInfo()!=null||uri.getRawFragment()!=null||port<1||port>65535)throw new IllegalArgumentException();
            String path=uri.getRawPath();if(path==null||path.isEmpty())path="/";if(uri.getRawQuery()!=null)path+="?"+uri.getRawQuery();
            if(raw.length()>4096||path.contains("\r")||path.contains("\n"))throw new IllegalArgumentException();
            return new LabTarget(raw,host.toLowerCase(java.util.Locale.ROOT),port,path);
        }catch(Exception error){throw new IllegalArgumentException("Enter a valid HTTPS website without credentials or a fragment");}
    }
}
