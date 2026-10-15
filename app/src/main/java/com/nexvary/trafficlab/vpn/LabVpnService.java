package com.nexvary.trafficlab.vpn;
import android.app.*;
import android.content.Intent;
import android.net.VpnService;
import android.os.*;
import com.nexvary.trafficlab.*;
import com.nexvary.trafficlab.tls.EmbeddedProxyService;
import hev.htproxy.TProxyService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** User-selected application routing. All forwarding sockets bypass this tunnel. */
public final class LabVpnService extends VpnService {
    public static final String ACTION_PACKET="com.nexvary.trafficlab.PACKET";
    public static volatile String state="STOPPED";
    private ParcelFileDescriptor tun;
    private LocalSocksGateway gateway;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active;
    private final Runnable monitor=new Runnable(){public void run(){
        if(!active)return;
        if(!TProxyService.TProxyIsRunning()){state="ERROR: forwarding engine stopped";stopSelf();return;}
        handler.postDelayed(this,1000);
    }};
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null||"stop".equals(intent.getAction())||!AuthSession.isUnlocked()){stopSelf();return START_NOT_STICKY;}
        if(active)return START_NOT_STICKY;
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("nexvary_lab_capture","NEXVARY VPN Lab",NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop=PendingIntent.getService(this,70,new Intent(this,LabVpnService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open=PendingIntent.getActivity(this,71,new Intent(this,VpnLabActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(7701,new Notification.Builder(this,"nexvary_lab_capture").setSmallIcon(R.drawable.ic_shield).setContentTitle("NEXVARY VPN Lab")
                .setContentText("Selected training applications · TCP / UDP").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build());
        state="STARTING";
        try{
            String[] packages=intent.getStringArrayExtra("packages");
            if(packages==null||packages.length==0)throw new IllegalArgumentException("Select at least one training application");
            Builder builder=new Builder().setSession("NEXVARY Traffic Lab").setMtu(1500)
                    .addAddress("198.18.0.1",32).addAddress("fd77::1",128)
                    .addRoute("0.0.0.0",0).addRoute("::",0).addDnsServer("198.18.0.2");
            Set<String> unique=new HashSet<>(Arrays.asList(packages));
            for(String name:unique){
                if(name==null||name.equals(getPackageName())||!name.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))throw new IllegalArgumentException("Invalid training package");
                builder.addAllowedApplication(name);
            }
            gateway=new LocalSocksGateway(new LocalSocksGateway.Protector(){
                public boolean protect(java.net.Socket s){return LabVpnService.this.protect(s);}
                public boolean protect(java.net.DatagramSocket s){return LabVpnService.this.protect(s);}
            },intent.getStringExtra("host"),intent.getIntExtra("port",443),8888,()->EmbeddedProxyService.engine!=null&&EmbeddedProxyService.engine.isRunning());
            int port=gateway.start();
            File config=new File(getFilesDir(),"vpn-tunnel.yml");
            try(FileOutputStream out=new FileOutputStream(config)){out.write(TunnelConfig.create(port,gateway.username(),gateway.password()).getBytes(StandardCharsets.UTF_8));}
            tun=builder.establish();if(tun==null)throw new IOException("Android VPN permission is required");
            if(!TProxyService.TProxyStartService(config.getAbsolutePath(),tun.getFd()))throw new IOException("Forwarding engine failed to start");
            active=true;state="RUNNING";handler.postDelayed(monitor,1000);
        }catch(Exception|LinkageError error){state="ERROR: "+error.getMessage();stopSelf();}
        return START_NOT_STICKY;
    }
    public static long[] stats(){try{return TProxyService.TProxyGetStats();}catch(LinkageError error){return new long[4];}}
    @Override public void onRevoke(){stopSelf();super.onRevoke();}
    @Override public void onDestroy(){
        handler.removeCallbacks(monitor);active=false;
        try{TProxyService.TProxyStopService();}catch(LinkageError ignored){}
        if(gateway!=null)gateway.close();gateway=null;
        try{if(tun!=null)tun.close();}catch(IOException ignored){}tun=null;
        new File(getFilesDir(),"vpn-tunnel.yml").delete();
        if(!state.startsWith("ERROR"))state="STOPPED";stopForeground(true);super.onDestroy();
    }
}
