package com.nexvary.trafficlab.packet;
public record PacketInfo(int ipVersion,String protocol,String src,String dst,int srcPort,int dstPort,int length,String note){}
