package com.nexvary.trafficlab.core;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Http2FrameLab {
    public record Frame(int length, int type, int flags, int streamId, byte[] payload) {
        public String typeName(){ return switch(type){case 0->"DATA";case 1->"HEADERS";case 4->"SETTINGS";case 6->"PING";case 7->"GOAWAY";case 8->"WINDOW_UPDATE";default->"TYPE_"+type;}; }
    }
    private Http2FrameLab() {}

    public static byte[] encode(int type,int flags,int streamId,byte[] payload){
        if(payload==null)payload=new byte[0]; if(payload.length>0xFFFFFF)throw new IllegalArgumentException("payload too large");
        byte[] out=new byte[9+payload.length]; int n=payload.length; out[0]=(byte)(n>>>16);out[1]=(byte)(n>>>8);out[2]=(byte)n;out[3]=(byte)type;out[4]=(byte)flags;
        int sid=streamId&0x7fffffff;out[5]=(byte)(sid>>>24);out[6]=(byte)(sid>>>16);out[7]=(byte)(sid>>>8);out[8]=(byte)sid;System.arraycopy(payload,0,out,9,n);return out;
    }

    public static Frame decode(byte[] raw){
        if(raw==null||raw.length<9)throw new IllegalArgumentException("HTTP/2 frame requires 9-byte header");
        int len=((raw[0]&255)<<16)|((raw[1]&255)<<8)|(raw[2]&255); if(raw.length<9+len)throw new IllegalArgumentException("truncated frame");
        int sid=((raw[5]&127)<<24)|((raw[6]&255)<<16)|((raw[7]&255)<<8)|(raw[8]&255);
        return new Frame(len,raw[3]&255,raw[4]&255,sid,Arrays.copyOfRange(raw,9,9+len));
    }

    public static String describe(Frame f){ return f.typeName()+" stream="+f.streamId()+" flags=0x"+Integer.toHexString(f.flags())+" length="+f.length()+" payload="+printable(f.payload()); }
    private static String printable(byte[] p){String s=new String(p, StandardCharsets.UTF_8);return s.chars().allMatch(c->c==9||c==10||c==13||(c>=32&&c<127))?s:"<binary "+p.length+" bytes>";}
}
