package com.nexvary.trafficlab.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import com.nexvary.trafficlab.R;
import com.nexvary.trafficlab.packet.*;
import java.io.*;
import java.util.Arrays;

/** Closed lab-tunnel capture service; route scope is limited to 10.77.0.0/24. */
public class LabVpnService extends VpnService {
    public static final String ACTION_PACKET="com.nexvary.trafficlab.PACKET";
    private static final String CHANNEL="nexvary_lab_capture";
    private static final int NOTIFICATION_ID=7701;
    private ParcelFileDescriptor tun;
    private volatile boolean running;
    private PcapWriter pcap;
    private Thread reader;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel(CHANNEL,"NEXVARY Lab Capture",NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, notification());
        if (tun == null) {
            tun = new Builder().setSession("NEXVARY Lab Capture").addAddress("10.77.0.2",24).addRoute("10.77.0.0",24).establish();
            running = tun != null;
            if (running) {
                try { pcap=new PcapWriter(new BufferedOutputStream(new FileOutputStream(new File(getFilesDir(),"lab-tunnel.pcap")))); } catch(Exception ignored){}
                reader=new Thread(this::readLoop, "nexvary-lab-tun"); reader.setDaemon(true); reader.start();
            }
        }
        return START_NOT_STICKY;
    }

    private Notification notification(){
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        return b.setContentTitle(getString(R.string.app_name)).setContentText("Authorized closed lab capture is active").setSmallIcon(android.R.drawable.stat_sys_download_done).setOngoing(true).build();
    }

    private void readLoop() {
        if (tun == null) return;
        try (FileInputStream in = new FileInputStream(tun.getFileDescriptor())) {
            byte[] buffer = new byte[65535]; int n;
            while (running && (n=in.read(buffer)) >= 0) {
                if(n==0) continue;
                byte[] packet=Arrays.copyOf(buffer,n);
                try { if(pcap!=null) pcap.write(packet,System.currentTimeMillis()); } catch(Exception ignored){}
                try {
                    PacketInfo info=PacketParser.parse(packet);
                    Intent e=new Intent(ACTION_PACKET); e.setPackage(getPackageName());
                    e.putExtra("summary",info.protocol()+" "+info.src()+":"+info.srcPort()+" → "+info.dst()+":"+info.dstPort()+" ("+info.length()+"B)");
                    sendBroadcast(e);
                } catch(Exception ignored) { /* malformed packets must not stop capture */ }
            }
        } catch (Exception ignored) { }
    }

    @Override public void onDestroy() {
        running=false;
        if(reader!=null) reader.interrupt();
        try { if (pcap!=null) pcap.close(); } catch(Exception ignored){} pcap=null;
        try { if (tun!=null) tun.close(); } catch(Exception ignored) {} tun=null;
        stopForeground(true);
        super.onDestroy();
    }
}
