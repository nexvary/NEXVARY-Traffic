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
    public static volatile LocalHttpsLesson lesson;
    private int generation;
    private static volatile EmbeddedProxyService instance;
    public static void stopNow(){EmbeddedProxyService current=instance;if(current!=null)current.shutdown();}
    private synchronized void shutdown(){generation++;if(engine!=null)engine.close();engine=null;if(lesson!=null)lesson.close();lesson=null;}
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public synchronized int onStartCommand(Intent intent,int flags,int id) {
        if(intent==null || "stop".equals(intent.getAction()) || !AuthSession.isUnlocked()) {stopSelf();return START_NOT_STICKY;}
        if((engine!=null && engine.isRunning())||(instance==this&&state.equals("STARTING"))) return START_NOT_STICKY;
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("embedded_https","NEXVARY HTTPS Lab",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,EmbeddedProxyService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(8801,new Notification.Builder(this,"embedded_https").setSmallIcon(R.drawable.ic_shield)
                .setContentTitle("NEXVARY HTTPS Lab").setContentText("Local proxy · 127.0.0.1:8888")
                .setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build());
        instance=this;state="STARTING"; final int expected=++generation;
        new Thread(() -> {
            EmbeddedTlsProxy candidate=null;LocalHttpsLesson demo=null;
            try {
                LabCertificateAuthority ca=LabCertificateAuthority.open(this);
                if(intent.getBooleanExtra("lesson",false))demo=new LocalHttpsLesson(ca);
                candidate=demo==null?new EmbeddedTlsProxy(ca,intent.getStringExtra("host"),intent.getIntExtra("port",443),intent.getBooleanExtra("pause",true),intent.getBooleanExtra("pauseResponse",false),javax.net.ssl.SSLContext.getDefault()):
                    new EmbeddedTlsProxy(ca,"127.0.0.1",demo.port(),true,intent.getBooleanExtra("pauseResponse",false),demo.trustedUpstream());
                synchronized(this) {
                    if(expected!=generation) {candidate.close();if(demo!=null)demo.close();return;}
                    candidate.start(8888); engine=candidate;lesson=demo;state="RUNNING";if(demo!=null)demo.begin(8888);
                }
            } catch(Exception error) {
                if(candidate!=null) candidate.close();if(demo!=null)demo.close();
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
