package com.nexvary.trafficlab.packet;

public class DnsParser {
    public record DnsQuery(int id,String name,int type){}
    public static DnsQuery parseQuery(byte[] d,int off,int len){ if(d==null||len<12||off<0||off+len>d.length) throw new IllegalArgumentException("invalid DNS"); int id=u16(d,off); int qd=u16(d,off+4); if(qd<1) throw new IllegalArgumentException("no question"); int p=off+12,end=off+len; StringBuilder n=new StringBuilder(); while(p<end){ int l=d[p++]&255; if(l==0)break; if((l&0xC0)!=0||p+l>end)throw new IllegalArgumentException("compressed/invalid demo query"); if(n.length()>0)n.append('.'); for(int i=0;i<l;i++)n.append((char)(d[p++]&255)); } if(p+4>end)throw new IllegalArgumentException("truncated question"); int type=u16(d,p); return new DnsQuery(id,n.toString(),type); }
    private static int u16(byte[] p,int i){return ((p[i]&255)<<8)|(p[i+1]&255);} }
