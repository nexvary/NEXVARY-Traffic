package com.nexvary.trafficlab.vpn;

import android.content.Context;
import com.nexvary.trafficlab.packet.BoundedPcapCapture;
import java.io.*;

/** One app-private temporary capture; exports only after VPN stops. Never backed up. */
public final class VpnPacketCapture {
    public static final long LIMIT=32L*1024*1024;
    private static BoundedPcapCapture current;
    private static boolean active;
    private VpnPacketCapture(){}
    private static File file(Context context){return new File(context.getCacheDir(),"vpn-session.pcap");}
    public static synchronized BoundedPcapCapture begin(Context context)throws IOException{
        if(active)throw new IOException("Stop the active VPN recording first");
        current=new BoundedPcapCapture(file(context),LIMIT);active=true;return current;
    }
    public static synchronized void stop(){if(current!=null)current.close();active=false;}
    public static synchronized String status(Context context){return current!=null?current.status():file(context).isFile()?"SAVED · "+file(context).length()+" B":"EMPTY";}
    public static synchronized void export(Context context,OutputStream output)throws IOException{
        if(active)throw new IOException("Stop VPN before exporting PCAP");
        File source=file(context);if(!source.isFile()||source.length()<24)throw new IOException("No recorded VPN packets");
        try(InputStream in=new FileInputStream(source)){byte[] block=new byte[65536];int n;while((n=in.read(block))!=-1)output.write(block,0,n);}
    }
    public static synchronized void clear(Context context)throws IOException{
        if(active)throw new IOException("Stop VPN before deleting PCAP");
        if(file(context).exists()&&!file(context).delete())throw new IOException("Could not delete temporary PCAP");current=null;
    }
}
