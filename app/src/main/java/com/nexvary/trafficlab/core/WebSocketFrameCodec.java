package com.nexvary.trafficlab.core;

import java.nio.*; import java.nio.charset.StandardCharsets; import java.security.SecureRandom;

public class WebSocketFrameCodec {
    public record Frame(boolean fin,int opcode,byte[] payload){}
    public static byte[] encodeClientText(String text){ byte[] p=text.getBytes(StandardCharsets.UTF_8); if(p.length>125) throw new IllegalArgumentException("Demo codec supports payload <=125 bytes"); byte[] mask=new byte[4]; new SecureRandom().nextBytes(mask); ByteBuffer b=ByteBuffer.allocate(2+4+p.length); b.put((byte)0x81); b.put((byte)(0x80|p.length)); b.put(mask); for(int i=0;i<p.length;i++) b.put((byte)(p[i]^mask[i%4])); return b.array(); }
    public static Frame decode(byte[] raw){ if(raw==null||raw.length<2) throw new IllegalArgumentException("short frame"); int p=0; int b0=raw[p++]&255,b1=raw[p++]&255; boolean masked=(b1&128)!=0; int len=b1&127; if(len>125) throw new IllegalArgumentException("demo codec <=125 bytes"); byte[] mask=null; if(masked){ if(raw.length<p+4) throw new IllegalArgumentException("short mask"); mask=new byte[]{raw[p++],raw[p++],raw[p++],raw[p++]}; } if(raw.length<p+len) throw new IllegalArgumentException("short payload"); byte[] out=new byte[len]; for(int i=0;i<len;i++){ byte v=raw[p+i]; out[i]=(byte)(masked?v^mask[i%4]:v); } return new Frame((b0&128)!=0,b0&15,out); }
}
