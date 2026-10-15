package com.nexvary.trafficlab.packet;

import java.io.*; import java.nio.*;

public class PcapWriter implements Closeable {
    private final OutputStream out;
    public PcapWriter(OutputStream out) throws IOException{this.out=out; ByteBuffer h=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN); h.putInt(0xa1b2c3d4).putShort((short)2).putShort((short)4).putInt(0).putInt(0).putInt(65535).putInt(101); out.write(h.array());}
    public synchronized void write(byte[] packet,long timeMillis)throws IOException{long sec=timeMillis/1000, usec=(timeMillis%1000)*1000; ByteBuffer h=ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN); h.putInt((int)sec).putInt((int)usec).putInt(packet.length).putInt(packet.length); out.write(h.array()); out.write(packet);}
    public void close()throws IOException{out.close();}
}
