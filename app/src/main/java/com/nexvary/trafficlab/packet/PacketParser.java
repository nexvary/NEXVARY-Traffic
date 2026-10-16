package com.nexvary.trafficlab.packet;

public class PacketParser {
    public static PacketInfo parse(byte[] p){ if(p==null||p.length<20) return new PacketInfo(0,"UNKNOWN","","",-1,-1,p==null?0:p.length,"short packet"); int ver=(p[0]>>4)&15; if(ver!=4)return new PacketInfo(ver,"UNSUPPORTED","","",-1,-1,p.length,"IPv4 parser in current build"); int ihl=(p[0]&15)*4; if(ihl<20||p.length<ihl)return new PacketInfo(4,"MALFORMED","","",-1,-1,p.length,"invalid IHL"); int proto=p[9]&255; String src=ip(p,12),dst=ip(p,16); int sp=-1,dp=-1; String name=proto==6?"TCP":proto==17?"UDP":proto==1?"ICMP":"IP-"+proto; if((proto==6||proto==17)&&p.length>=ihl+4){sp=u16(p,ihl);dp=u16(p,ihl+2);} return new PacketInfo(4,name,src,dst,sp,dp,p.length,""); }
    private static int u16(byte[] p,int i){return ((p[i]&255)<<8)|(p[i+1]&255);} private static String ip(byte[] p,int i){return (p[i]&255)+"."+(p[i+1]&255)+"."+(p[i+2]&255)+"."+(p[i+3]&255);} }
