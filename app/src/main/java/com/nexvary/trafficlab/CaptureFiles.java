package com.nexvary.trafficlab;
import android.content.Context;
import android.security.keystore.*;
import com.nexvary.trafficlab.core.*;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Instant;
import java.util.*;

/** Explicit local retention: authenticated encryption with a non-exportable device key. */
public final class CaptureFiles {
    private static final int MAX_FILE=5*1024*1024;
    private static final String ALIAS="nexvary.capture.archive.v1";
    private CaptureFiles(){}
    private static SecretKey key()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias(ALIAS)){
            KeyGenerator generator=KeyGenerator.getInstance("AES","AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();
        }return (SecretKey)store.getKey(ALIAS,null);
    }
    public static byte[] bounded(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>MAX_FILE)throw new IOException("Session file exceeds 5 MiB");out.write(b,0,n);}return out.toByteArray();
    }
    public static synchronized void save(Context context,List<CapturedTraffic.Exchange> entries)throws Exception{
        byte[] plain=HarExport.create(entries,true).getBytes(StandardCharsets.UTF_8);if(plain.length>MAX_FILE-29)throw new IOException("Archive exceeds limit");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        File target=new File(context.getFilesDir(),"capture-session.enc"),temp=new File(context.getFilesDir(),"capture-session.tmp");
        try(FileOutputStream out=new FileOutputStream(temp)){out.write(1);out.write(cipher.getIV());out.write(cipher.doFinal(plain));out.getFD().sync();}
        if(!temp.renameTo(target)){temp.delete();throw new IOException("Cannot commit encrypted archive");}
    }
    public static synchronized String load(Context context)throws Exception{
        byte[] data;try(InputStream in=new FileInputStream(new File(context.getFilesDir(),"capture-session.enc"))){data=bounded(in);}
        if(data.length<30||data[0]!=1)throw new IOException("Invalid encrypted archive");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Arrays.copyOfRange(data,1,13)));
        return new String(cipher.doFinal(data,13,data.length-13),StandardCharsets.UTF_8);
    }
    public static synchronized void delete(Context context)throws IOException{
        for(String name:new String[]{"capture-session.enc","capture-session.tmp"}){File file=new File(context.getFilesDir(),name);if(file.exists()&&!file.delete())throw new IOException("Cannot delete local archive");}
    }
    private static String limited(String s)throws IOException{if(s.length()>32768)throw new IOException("HAR preview exceeds limit");return s;}
    private static String headers(JSONArray array)throws Exception{
        if(array==null)return "";if(array.length()>200)throw new IOException("Too many headers");StringBuilder out=new StringBuilder();
        for(int i=0;i<array.length();i++){JSONObject h=array.getJSONObject(i);String name=h.getString("name"),value=h.getString("value");
            if(!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")||value.indexOf('\r')>=0||value.indexOf('\n')>=0)throw new IOException("Invalid HAR header");
            out.append(name).append(": ").append(value).append("\r\n");if(out.length()>32768)throw new IOException("HAR headers exceed limit");}
        return out.toString();
    }
    public static List<String[]> parse(String content)throws Exception{
        if(content.getBytes(StandardCharsets.UTF_8).length>MAX_FILE)throw new IOException("HAR exceeds limit");
        JSONArray entries=new JSONObject(content).getJSONObject("log").getJSONArray("entries");if(entries.length()>32)throw new IOException("Import at most 32 exchanges per file");
        List<String[]> parsed=new ArrayList<>();
        for(int i=0;i<entries.length();i++){
            JSONObject e=entries.getJSONObject(i),request=e.getJSONObject("request"),response=e.getJSONObject("response");URI url=new URI(request.getString("url"));
            if(!Arrays.asList("http","https").contains(url.getScheme())||url.getHost()==null)throw new IOException("Invalid HAR URL");
            String method=request.getString("method");if(!method.matches("[A-Z]+"))throw new IOException("Invalid HAR method");
            String path=url.getRawPath();if(path==null||path.isEmpty())path="/";if(url.getRawQuery()!=null)path+="?"+url.getRawQuery();
            String req=method+" "+path+" HTTP/1.1\r\n"+headers(request.optJSONArray("headers"))+"\r\n";
            if(request.has("postData"))req+=request.getJSONObject("postData").optString("text","");
            int status=response.getInt("status");if(status<0||status>999)throw new IOException("Invalid HAR status");
            String res="HTTP/1.1 "+status+" Imported\r\n"+headers(response.optJSONArray("headers"))+"\r\n";
            JSONObject body=response.optJSONObject("content");if(body!=null&&!body.has("encoding"))res+=body.optString("text","");
            String original=e.optString("_nexvaryOriginalTextPreview",req),forwarded=e.optString("_nexvaryForwardedTextPreview",req),received=e.optString("_nexvaryResponseTextPreview",res);
            long timestamp=System.currentTimeMillis();try{timestamp=Instant.parse(e.optString("startedDateTime")).toEpochMilli();}catch(Exception ignored){}
            parsed.add(new String[]{limited(original),limited(forwarded),limited(received),String.valueOf(Math.max(0,e.optLong("time",0))),String.valueOf(timestamp)});
        }return parsed;
    }
}
