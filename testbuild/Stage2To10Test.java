import com.nexvary.trafficlab.core.*;
import com.nexvary.trafficlab.packet.*;
import com.nexvary.trafficlab.evidence.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Stage2To10Test {
  static void ok(boolean c,String m){ if(!c) throw new RuntimeException("FAIL: "+m); }
  public static void main(String[] args) throws Exception {
    LabRequest r=new LabRequest(); r.method="POST"; r.path="/api/order"; r.headers.put("Content-Type","application/json"); r.body="{\"price\":100,\"token\":\"abc\"}";
    LabResponse s=new LabResponse(); s.statusCode=200; s.raw="HTTP/1.1 200 OK\r\n\r\n{}";
    HistoryStore h=new HistoryStore(); h.add(r,s,List.of()); ok(h.search("order").size()==1,"history search"); h.tag(1,"LECTURE"); ok(h.search("lecture").size()==1,"history tag");
    ok(DiffEngine.pretty("a\nb","a\nc").contains("CHANGE"),"diff");
    byte[] ws=WebSocketFrameCodec.encodeClientText("hello"); ok(new String(WebSocketFrameCodec.decode(ws).payload(),StandardCharsets.UTF_8).equals("hello"),"websocket");
    String red=PresenterRedactor.redact("Authorization: Bearer abc\n{\"password\":\"123\"}"); ok(!red.contains("Bearer abc")&&!red.contains("123"),"redaction");
    ok(ExplainEngine.explain(r,true).contains("الخادم"),"explain ar"); ok(WhatIfEngine.analyze(r).toLowerCase().contains("server"),"whatif");
    RulePack rp=RulePack.builtIn(); ok(rp.verifyIntegrity(),"rulepack");
    EvidenceWorkspace ew=new EvidenceWorkspace(); ew.add("one","A"); ew.add("two","B"); ok(ew.verifyChain(),"evidence chain");
    byte[] pkt=sample(); PacketInfo pi=PacketParser.parse(pkt); ok(pi.protocol().equals("UDP")&&pi.dstPort()==53,"packet parser"); int ihl=(pkt[0]&15)*4; DnsParser.DnsQuery dq=DnsParser.parseQuery(pkt,ihl+8,pkt.length-(ihl+8)); ok(dq.name().equals("nexvary.com"),"dns parser");
    ByteArrayOutputStream bo=new ByteArrayOutputStream(); try(PcapWriter p=new PcapWriter(bo)){p.write(pkt,1_700_000_000_000L);} ok(bo.toByteArray().length==24+16+pkt.length,"pcap writer");
    System.out.println("STAGES_2_TO_10_OK entries="+h.all().size()+" dns="+dq.name()+" pcap="+bo.size()+" ws="+ws.length);
  }
  static byte[] sample(){ byte[] dns=new byte[]{0x12,0x34,0x01,0x00,0x00,0x01,0,0,0,0,0,0,7,'n','e','x','v','a','r','y',3,'c','o','m',0,0,1,0,1}; int total=20+8+dns.length; byte[] p=new byte[total]; p[0]=0x45; p[2]=(byte)(total>>8);p[3]=(byte)total;p[8]=64;p[9]=17;p[12]=10;p[13]=77;p[14]=0;p[15]=2;p[16]=10;p[17]=77;p[18]=0;p[19]=1; int o=20;p[o]=0x30;p[o+1]=0x39;p[o+2]=0;p[o+3]=53;int u=8+dns.length;p[o+4]=(byte)(u>>8);p[o+5]=(byte)u;System.arraycopy(dns,0,p,28,dns.length);return p; }
}
