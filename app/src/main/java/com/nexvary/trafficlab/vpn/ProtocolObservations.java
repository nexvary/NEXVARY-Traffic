package com.nexvary.trafficlab.vpn;
import com.nexvary.trafficlab.core.QuicMetadata;
import java.util.*;

/** Bounded live protocol metadata, derived from actual TUN packets in capture mode. */
public final class ProtocolObservations {
    private static final ArrayDeque<String> recent=new ArrayDeque<>();private static long quic;
    private ProtocolObservations(){}
    public static synchronized void clear(){recent.clear();quic=0;}
    public static synchronized void packet(byte[] bytes,int count){QuicMetadata.Header h=QuicMetadata.udpPacket(bytes,count);if(h==null)return;quic++;String row=h.version()+" · "+h.type()+" · DCID "+h.destinationId()+" · "+h.bytes()+" B";if(recent.size()==16)recent.removeFirst();recent.addLast(row);}
    public static synchronized String report(){return "QUIC long-header candidates: "+quic+"\n"+String.join("\n",recent)+"\nQUIC/TLS payloads remain encrypted. Short headers and IPv6 extension headers are not classified.";}
}
