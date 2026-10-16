package com.nexvary.trafficlab.packet;

import java.io.*;
import java.util.Arrays;

/** Complete raw IP packets, bounded on disk. Recording failure never stops forwarding. */
public final class BoundedPcapCapture implements Closeable {
    private PcapWriter writer;
    private long bytes=24, packets;
    private final long limit;
    private String state="RECORDING";
    public BoundedPcapCapture(File file,long limit)throws IOException{
        if(limit<24)throw new IllegalArgumentException("PCAP limit must include its header");
        this.limit=limit;writer=new PcapWriter(new BufferedOutputStream(new FileOutputStream(file),65536));
    }
    public synchronized void record(byte[] packet,int count,long millis){
        if(writer==null)return;
        if(count<1||count>65535||count>packet.length){finish("ERROR: invalid packet length");return;}
        if(bytes+16L+count>limit){finish("LIMIT_REACHED");return;}
        try{writer.write(Arrays.copyOf(packet,count),millis);bytes+=16L+count;packets++;}
        catch(IOException error){finish("ERROR: "+error.getMessage());}
    }
    private void finish(String next){
        state=next;if(writer!=null){try{writer.close();}catch(IOException error){state="ERROR: "+error.getMessage();}writer=null;}
    }
    public synchronized String status(){return state+" · "+packets+" packets / "+bytes+" B";}
    public synchronized long packets(){return packets;}
    @Override public synchronized void close(){if(writer!=null)finish("STOPPED");}
}
