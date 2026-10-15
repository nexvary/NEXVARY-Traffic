package com.nexvary.trafficlab.core;

import java.nio.ByteBuffer;
import java.util.*;

public final class GrpcInspector {
    public record Message(boolean compressed, int length, byte[] payload) {}
    private GrpcInspector() {}

    public static byte[] frame(byte[] payload, boolean compressed){
        if(payload==null)payload=new byte[0]; ByteBuffer b=ByteBuffer.allocate(5+payload.length);b.put((byte)(compressed?1:0));b.putInt(payload.length);b.put(payload);return b.array();
    }
    public static Message inspect(byte[] data){
        if(data==null||data.length<5)throw new IllegalArgumentException("gRPC message prefix requires 5 bytes"); ByteBuffer b=ByteBuffer.wrap(data);boolean c=b.get()!=0;int n=b.getInt();if(n<0||n>b.remaining())throw new IllegalArgumentException("truncated gRPC message");byte[] p=new byte[n];b.get(p);return new Message(c,n,p);
    }
    public static String contentTypeHint(Map<String,String> headers){
        for(var e:headers.entrySet())if(e.getKey().equalsIgnoreCase("content-type"))return e.getValue(); return "unknown";
    }
}
