package com.nexvary.trafficlab.vpn;

import android.os.ParcelFileDescriptor;
import android.system.*;
import com.nexvary.trafficlab.packet.BoundedPcapCapture;
import java.io.*;

/** Owns a datagram pair, never the Android TUN. Copies both IP directions exactly once. */
public final class TunPacketBridge implements Closeable {
    private final FileDescriptor tun,bridge=new FileDescriptor(),engine=new FileDescriptor();
    private final ParcelFileDescriptor nativeFd;
    private final BoundedPcapCapture capture;
    private final Thread outgoing,incoming;
    private volatile boolean running=true;
    public volatile String error="";
    public TunPacketBridge(FileDescriptor tun,BoundedPcapCapture capture)throws Exception{
        this.tun=tun;this.capture=capture;
        Os.socketpair(OsConstants.AF_UNIX,OsConstants.SOCK_DGRAM|OsConstants.SOCK_NONBLOCK,0,bridge,engine);
        try{
            nativeFd=ParcelFileDescriptor.dup(engine);
        }catch(Exception error){Os.close(bridge);Os.close(engine);throw error;}
        outgoing=new Thread(()->pump(tun,bridge),"nexvary-pcap-out");
        incoming=new Thread(()->pump(bridge,tun),"nexvary-pcap-in");
        outgoing.setDaemon(true);incoming.setDaemon(true);
    }
    public int engineFd(){return nativeFd.getFd();}
    public void start(){outgoing.start();incoming.start();}
    private boolean ready(FileDescriptor fd,short events)throws ErrnoException{
        StructPollfd poll=new StructPollfd();poll.fd=fd;poll.events=events;
        int n=Os.poll(new StructPollfd[]{poll},200);
        if(n>0&&(poll.revents&(OsConstants.POLLERR|OsConstants.POLLHUP|OsConstants.POLLNVAL))!=0)throw new ErrnoException("packet bridge closed",OsConstants.EIO);
        return n>0&&(poll.revents&events)!=0;
    }
    private void pump(FileDescriptor from,FileDescriptor to){
        byte[] packet=new byte[65535];
        try{while(running){
            if(!ready(from,(short)OsConstants.POLLIN))continue;
            int count;
            try{count=Os.read(from,packet,0,packet.length);}catch(ErrnoException e){if(e.errno==OsConstants.EAGAIN||e.errno==OsConstants.EINTR)continue;throw e;}
            if(count<=0)throw new IOException("Packet descriptor reached EOF");
            capture.record(packet,count,System.currentTimeMillis());
            // Datagram boundaries must survive: never split a partial packet into another write.
            while(running){
                if(!ready(to,(short)OsConstants.POLLOUT))continue;
                try{if(Os.write(to,packet,0,count)!=count)throw new IOException("Partial packet write");break;}
                catch(ErrnoException e){if(e.errno!=OsConstants.EAGAIN&&e.errno!=OsConstants.EINTR)throw e;}
            }
        }}catch(Exception failure){if(running)error=failure.getMessage();running=false;}
    }
    public boolean isRunning(){return running;}
    @Override public void close(){
        running=false;
        for(Thread thread:new Thread[]{outgoing,incoming})try{thread.join(1000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        try{nativeFd.close();}catch(IOException ignored){}
        try{Os.close(engine);}catch(ErrnoException ignored){}
        try{Os.close(bridge);}catch(ErrnoException ignored){}
        capture.close();
    }
}
