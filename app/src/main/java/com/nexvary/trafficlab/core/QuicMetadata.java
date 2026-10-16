package com.nexvary.trafficlab.core;
import java.nio.*;
import java.util.*;

/** Passive QUIC long-header metadata only. Does not claim TLS/HTTP3 payload decryption. */
public final class QuicMetadata {
    public record Header(String version,String type,String destinationId,String sourceId,int bytes){}
    private static long variable(ByteBuffer b){int first=b.get()&255;int bytes=1<<(first>>>6);long value=first&63;for(int i=1;i<bytes;i++)value=(value<<8)|(b.get()&255);return value;}
    private static String id(ByteBuffer b){int n=b.get()&255;if(n>20||n>b.remaining())throw new IllegalArgumentException();StringBuilder out=new StringBuilder();for(int i=0;i<n;i++)out.append(String.format(Locale.ROOT,"%02x",b.get()&255));return out.toString();}
    public static Header parse(byte[] datagram){
        try{
            ByteBuffer b=ByteBuffer.wrap(datagram);int flags=b.get()&255;if((flags&128)==0)return null;
            int version=b.getInt();if(version!=0&&(flags&64)==0)return null;
            String dcid=id(b),scid=id(b);
            if(version==0){if(b.remaining()<4||b.remaining()%4!=0)return null;return new Header("NEGOTIATION","VERSION_NEGOTIATION",dcid,scid,datagram.length);}
            boolean v2=version==0x6b3343cf;if(version!=1&&!v2)return null;
            int type=(flags>>>4)&3;String name=(v2?new String[]{"RETRY","INITIAL","ZERO_RTT","HANDSHAKE"}:new String[]{"INITIAL","ZERO_RTT","HANDSHAKE","RETRY"})[type];
            if(name.equals("RETRY")){if(b.remaining()<16)return null;}
            else{
                if(name.equals("INITIAL")){long token=variable(b);if(token>b.remaining())return null;b.position(b.position()+(int)token);}
                long payload=variable(b);if(payload<17||payload>b.remaining())return null;
            }
            return new Header(v2?"QUIC v2":"QUIC v1",name,dcid,scid,datagram.length);
        }catch(RuntimeException malformed){return null;}
    }
    public static Header udpPacket(byte[] packet,int count){
        try{
            if(count<28||count>packet.length)return null;int version=(packet[0]&255)>>>4,offset,length;
            if(version==4){offset=(packet[0]&15)*4;length=((packet[2]&255)<<8)|(packet[3]&255);if(offset<20||length>count||(packet[9]&255)!=17||(packet[6]&0x3f)!=0||packet[7]!=0)return null;}
            else if(version==6){offset=40;length=40+(((packet[4]&255)<<8)|(packet[5]&255));if(count<48||length>count||(packet[6]&255)!=17)return null;}
            else return null;
            if(length<offset+8)return null;int udp=((packet[offset+4]&255)<<8)|(packet[offset+5]&255);if(udp<8||offset+udp>length)return null;
            return parse(Arrays.copyOfRange(packet,offset+8,offset+udp));
        }catch(RuntimeException malformed){return null;}
    }
}
