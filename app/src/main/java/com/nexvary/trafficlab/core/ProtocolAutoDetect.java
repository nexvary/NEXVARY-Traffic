package com.nexvary.trafficlab.core;
import java.nio.charset.StandardCharsets;
public final class ProtocolAutoDetect {
  public record Detection(String protocol,int confidence,String reason){}
  private ProtocolAutoDetect(){}
  public static Detection detect(byte[] data,int port){
    if(data==null)data=new byte[0]; String s=new String(data,StandardCharsets.ISO_8859_1);
    if(s.startsWith("PRI * HTTP/2.0")) return new Detection("HTTP/2",99,"HTTP/2 connection preface");
    if(s.startsWith("GET ")||s.startsWith("POST ")||s.startsWith("PUT ")||s.startsWith("DELETE ")||s.startsWith("PATCH ")||s.startsWith("HTTP/1.")) return new Detection("HTTP/1.1",98,"HTTP start-line detected");
    if(data.length>=5 && (data[0]&0xff)==0x16 && (data[1]&0xff)==0x03) return new Detection("TLS",97,"TLS handshake record signature");
    if(port==53) return new Detection("DNS",90,"DNS service port");
    if(port==443) return new Detection("TLS/HTTPS",75,"HTTPS service port");
    if(port==80) return new Detection("HTTP",75,"HTTP service port");
    return new Detection("UNKNOWN",25,"No strong signature; metadata only");
  }
}
