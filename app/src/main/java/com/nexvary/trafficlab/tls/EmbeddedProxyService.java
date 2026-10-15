package com.nexvary.trafficlab.tls;
import android.app.*;
import android.content.Intent;
import android.os.IBinder;
import com.nexvary.trafficlab.AuthSession;
import com.nexvary.trafficlab.MainActivity;
import com.nexvary.trafficlab.R;

public final class EmbeddedProxyService extends Service {
    public static volatile EmbeddedTlsProxy engine;
    public static volatile String state = "STOPPED";
    private int generation;
    private static volatile EmbeddedProxyService instance;
    public static void stopNow(){EmbeddedProxyService current=instance;if(current!=null)current.shutdown();}
    private synchronized void shutdown(){generation++;if(engine!=null)engine.close();engine=null;}
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public synchronized int onStartCommand(Intent intent,int flags,int id) {
        if(intent==null || "stop".equals(intent.getAction()) || !AuthSession.isUnlocked()) {stopSelf();return START_NOT_STICKY;}
        if(engine!=null && engine.isRunning()) return START_NOT_STICKY;
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("embedded_https","NEXVARY HTTPS Lab",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,EmbeddedProxyService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(8801,new Notification.Builder(this,"embedded_https").setSmallIcon(R.drawable.ic_shield)
                .setContentTitle("NEXVARY HTTPS Lab").setContentText("Local proxy · 127.0.0.1:8888")
                .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build());
        instance=this;state="STARTING"; final int expected=++generation;
        new Thread(() -> {
            EmbeddedTlsProxy candidate=null;
            try {
                candidate=new EmbeddedTlsProxy(LabCertificateAuthority.open(this),intent.getStringExtra("host"),intent.getIntExtra("port",443),intent.getBooleanExtra("pause",true),intent.getBooleanExtra("pauseResponse",false),javax.net.ssl.SSLContext.getDefault());
                synchronized(this) {
                    if(expected!=generation) {candidate.close();return;}
                    candidate.start(8888); engine=candidate; state="RUNNING";
                }
            } catch(Exception error) {
                if(candidate!=null) candidate.close();
                synchronized(this) {if(expected==generation) {state="ERROR: "+error.getMessage();stopSelf();}}
            }
        },"nexvary-proxy-start").start();
        return START_NOT_STICKY;
    }
    @Override public synchronized void onDestroy() {
        shutdown();instance=null;
        if(!state.startsWith("ERROR")) state="STOPPED";
        stopForeground(true); super.onDestroy();
    }
}
