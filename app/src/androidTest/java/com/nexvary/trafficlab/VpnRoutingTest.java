package com.nexvary.trafficlab;
import android.app.Activity;
import android.content.*;
import android.net.VpnService;
import android.test.InstrumentationTestCase;
import com.nexvary.trafficlab.vpn.LabVpnService;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class VpnRoutingTest extends InstrumentationTestCase {
    public void testSeparateApplicationTcpUdpDnsAndStop()throws Exception{
        Context context=getInstrumentation().getTargetContext();AuthSession.grant();
        Activity activity=getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
        BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent intent){result.set(intent.getStringExtra("result"));done.countDown();}};
        context.registerReceiver(receiver,new IntentFilter("com.nexvary.trafficlab.FIXTURE_RESULT"),Context.RECEIVER_EXPORTED);
        try{
            assertNull("CI must grant VPN consent through appops",VpnService.prepare(context));
            getInstrumentation().runOnMainSync(()->context.startForegroundService(new Intent(context,LabVpnService.class)
                    .putExtra("packages",new String[]{"com.nexvary.trafficfixture"}).putExtra("host","lab.invalid").putExtra("port",443)));
            long until=System.currentTimeMillis()+10000;
            while(!LabVpnService.state.equals("RUNNING")&&System.currentTimeMillis()<until)Thread.sleep(25);
            assertEquals(LabVpnService.state,"RUNNING",LabVpnService.state);
            // Allow ConnectivityService to publish routes before the separate UID opens sockets.
            Thread.sleep(500);
            context.startActivity(new Intent().setClassName("com.nexvary.trafficfixture","com.nexvary.trafficfixture.TrafficFixtureActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            assertTrue("Training app did not return: "+LabVpnService.state,done.await(40,TimeUnit.SECONDS));
            assertEquals("TCP_UDP_DNS_OK",result.get());
            long[] counters=LabVpnService.stats();assertTrue("Traffic bypassed native tunnel",counters[0]>0&&counters[2]>0);
            context.stopService(new Intent(context,LabVpnService.class));
            until=System.currentTimeMillis()+5000;while(!LabVpnService.state.equals("STOPPED")&&System.currentTimeMillis()<until)Thread.sleep(25);
            assertEquals("STOPPED",LabVpnService.state);assertFalse(hev.htproxy.TProxyService.TProxyIsRunning());
        }finally{
            context.unregisterReceiver(receiver);context.stopService(new Intent(context,LabVpnService.class));
            getInstrumentation().runOnMainSync(activity::finish);AuthSession.revoke();
        }
    }
}
