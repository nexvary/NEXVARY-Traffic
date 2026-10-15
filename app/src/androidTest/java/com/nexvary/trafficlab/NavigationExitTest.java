package com.nexvary.trafficlab;

import android.app.*;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.graphics.Rect;
import android.os.SystemClock;
import com.nexvary.trafficlab.tls.EmbeddedProxyService;
import java.lang.reflect.Field;
import java.net.*;
import java.util.*;

public class NavigationExitTest extends InstrumentationTestCase {
    private MainActivity activity;
    @Override protected void setUp() throws Exception {
        super.setUp(); LocaleController.setLanguage(getInstrumentation().getTargetContext(),"en"); AuthSession.grant();
        activity=(MainActivity)getInstrumentation().startActivitySync(new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getInstrumentation().waitForIdleSync();
    }
    @Override protected void tearDown() throws Exception {
        getInstrumentation().getTargetContext().stopService(new Intent(getInstrumentation().getTargetContext(),EmbeddedProxyService.class));
        getInstrumentation().getTargetContext().stopService(new Intent(getInstrumentation().getTargetContext(),com.nexvary.trafficlab.vpn.LabVpnService.class));
        if(activity!=null && !activity.isFinishing())getInstrumentation().runOnMainSync(()->activity.finish());
        getInstrumentation().waitForIdleSync(); AuthSession.revoke(); super.tearDown();
    }
    private Object field(String name) throws Exception {Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private String route() throws Exception {return (String)field("currentScreen");}
    private View find(View root,String tag){if(tag.equals(root.getTag()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View v=find(g.getChildAt(i),tag);if(v!=null)return v;}}return null;}
    private void screenBounds(View view,Rect bounds){int[] location=new int[2];view.getLocationOnScreen(location);bounds.set(location[0],location[1],location[0]+view.getWidth(),location[1]+view.getHeight());}
    private void touch(Rect bounds){
        long now=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
        MotionEvent up=MotionEvent.obtain(now,now+50,MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
        try{getInstrumentation().sendPointerSync(down);getInstrumentation().sendPointerSync(up);}finally{down.recycle();up.recycle();}
        getInstrumentation().waitForIdleSync();
    }
    private void click(String tag){
        final View[] found=new View[1];getInstrumentation().runOnMainSync(()->found[0]=find(activity.getWindow().getDecorView(),tag));
        if(found[0]==null && tag.startsWith("nav:")){
            click("app:tools");
            try{
                AlertDialog menu=(AlertDialog)field("toolsDialog");android.widget.ListView list=menu.getListView();
                String label=UiI18n.t(activity,tag.substring(4));int index=-1;
                for(int i=0;i<list.getAdapter().getCount();i++)if(label.equals(list.getAdapter().getItem(i)))index=i;
                assertTrue("Missing menu route "+tag,index>=0);final int position=index;
                getInstrumentation().runOnMainSync(()->list.setSelectionFromTop(position,0));getInstrumentation().waitForIdleSync();
                final Rect bounds=new Rect();getInstrumentation().runOnMainSync(()->{
                    View item=list.getChildAt(position-list.getFirstVisiblePosition());assertNotNull("Menu item absent "+tag,item);
                    assertTrue("Menu item not visible "+tag,item.getGlobalVisibleRect(bounds));screenBounds(item,bounds);
                });touch(bounds);
                long until=SystemClock.uptimeMillis()+3000;
                while(menu.isShowing()&&SystemClock.uptimeMillis()<until){Thread.sleep(20);getInstrumentation().waitForIdleSync();}
                assertFalse("Menu selection did not complete "+tag,menu.isShowing());return;
            }catch(Exception error){throw new AssertionError(error);}
        }
        getInstrumentation().runOnMainSync(()->{View view=found[0];assertNotNull("Missing button "+tag,view);view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),true);});
        getInstrumentation().waitForIdleSync();final Rect bounds=new Rect();
        getInstrumentation().runOnMainSync(()->{assertTrue("Button not visible "+tag,found[0].getGlobalVisibleRect(bounds));screenBounds(found[0],bounds);});touch(bounds);
    }
    private void collect(View root,Set<String> tags){Object tag=root.getTag();if(tag instanceof String && ((String)tag).startsWith("nav:") && !tag.equals("nav:back"))tags.add((String)tag);if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),tags);}}
    public void testEveryNavigationButtonAndBack() throws Exception {
        Set<String> tags=new LinkedHashSet<>();
        String[] routes={"API Graph","About","AutoDetect","Baseline","Cases","Cockpit","Composer","Contract","Dashboard","Data Journey","Dependencies","Diff","Evidence","Explain","Flow Studio","H2/gRPC","HTTPS Lab","History","Intercept","JWT/OAuth","Lecture","Matrix","Packet Lab","Packet Map","Presenter","Radar","Regression","Remediation","Repeater","Replay","Report","Story","TLS","TLS Posture","Timeline","Waterfall","WebSocket","What-If"};
        for(String route:routes)tags.add("nav:"+route);
        for(String tag:tags){
            if(tag.equals("nav:HTTPS Lab"))continue;
            click(tag);assertEquals(tag.substring(4),route());
            assertTrue("Empty page "+tag,((ViewGroup)field("content")).getChildCount()>0);
            if(!tag.equals("nav:Dashboard")){click("nav:back");assertEquals("Dashboard",route());}
        }
        click("nav:back");assertFalse(activity.isFinishing());assertEquals("Dashboard",route());
    }
    public void testBackStackAndRepeatedTabs() throws Exception {
        click("nav:Intercept");click("nav:Intercept");click("nav:Repeater");click("nav:History");
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);getInstrumentation().waitForIdleSync();assertEquals("Repeater",route());click("nav:back");assertEquals("Intercept",route());
        click("nav:back");assertEquals("Dashboard",route());click("nav:back");assertEquals("Dashboard",route());
    }
    public void testCapturedTrafficFeedsHistoryAndEvidence() throws Exception {
        String raw="GET /captured-lesson HTTP/1.1\r\nHost: lab.example\r\n\r\n";
        com.nexvary.trafficlab.core.CapturedTraffic.openSession();
        com.nexvary.trafficlab.core.CapturedTraffic.completed(raw,raw,"HTTP/1.1 200 OK\r\n\r\ntraining-response",12,false);
        click("nav:History");
        com.nexvary.trafficlab.core.HistoryStore history=(com.nexvary.trafficlab.core.HistoryStore)field("history");
        boolean found=false;for(com.nexvary.trafficlab.core.HistoryEntry entry:history.all())
            if(entry.request.path.equals("/captured-lesson")&&entry.tags.contains("REAL_HTTPS")&&entry.response.body.equals("training-response"))found=true;
        assertTrue("Captured traffic is absent from History",found);
        click("nav:Evidence");com.nexvary.trafficlab.evidence.EvidenceWorkspace evidence=(com.nexvary.trafficlab.evidence.EvidenceWorkspace)field("evidence");
        assertFalse(evidence.all().isEmpty());assertTrue("Captured evidence broke the hash chain",evidence.verifyChain());
    }
    public void testHttpsReturnsToExistingPage() throws Exception {
        click("nav:History");
        Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(HttpsLabActivity.class.getName(),null,false);
        click("nav:HTTPS Lab");Activity https=getInstrumentation().waitForMonitorWithTimeout(monitor,5000);assertNotNull(https);
        getInstrumentation().runOnMainSync(()->{View back=find(https.getWindow().getDecorView(),"tls:back");assertNotNull(back);back.performClick();});
        getInstrumentation().waitForIdleSync();assertEquals("History",route());
        click("nav:back");assertEquals("Dashboard",route());getInstrumentation().removeMonitor(monitor);
    }
    public void testCancelExitThenConfirmStopsEngineAndLocks() throws Exception {
        getInstrumentation().runOnMainSync(()->activity.startForegroundService(new Intent(activity,EmbeddedProxyService.class).putExtra("host","localhost").putExtra("port",443)));
        long deadline=System.currentTimeMillis()+15000;
        while(EmbeddedProxyService.engine==null && System.currentTimeMillis()<deadline)Thread.sleep(50);
        assertNotNull("Embedded engine did not start: "+EmbeddedProxyService.state,EmbeddedProxyService.engine);
        getInstrumentation().runOnMainSync(()->activity.startForegroundService(new Intent(activity,com.nexvary.trafficlab.vpn.LabVpnService.class).putExtra("packages",new String[]{"com.nexvary.trafficfixture"}).putExtra("host","localhost").putExtra("port",443)));
        deadline=System.currentTimeMillis()+10000;
        while(!com.nexvary.trafficlab.vpn.LabVpnService.state.equals("RUNNING")&&System.currentTimeMillis()<deadline)Thread.sleep(25);
        assertEquals("RUNNING",com.nexvary.trafficlab.vpn.LabVpnService.state);
        Socket active=new Socket("127.0.0.1",8888);active.setSoTimeout(5000);
        active.getOutputStream().write("CONNECT localhost:443 HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));active.getOutputStream().flush();
        int headerState=0, headerBytes=0;while(headerState!=0x0d0a0d0a && headerBytes++<32768){int value=active.getInputStream().read();assertTrue("No CONNECT acknowledgement",value>=0);headerState=(headerState<<8)|value;}
        final int exitedTask=activity.getTaskId();
        click("nav:About");click("app:exit");AlertDialog dialog=(AlertDialog)field("exitDialog");
        getInstrumentation().runOnMainSync(()->dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        assertFalse(activity.isFinishing());assertTrue(AuthSession.isUnlocked());assertTrue(EmbeddedProxyService.engine.isRunning());assertTrue(hev.htproxy.TProxyService.TProxyIsRunning());
        click("app:exit");AlertDialog confirm=(AlertDialog)field("exitDialog");
        getInstrumentation().runOnMainSync(()->confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick());getInstrumentation().waitForIdleSync();
        assertTrue(activity.isFinishing()||activity.isDestroyed());assertFalse(AuthSession.isUnlocked());
        deadline=System.currentTimeMillis()+5000;while(EmbeddedProxyService.engine!=null && System.currentTimeMillis()<deadline)Thread.sleep(25);
        assertNull("Exit left capture running",EmbeddedProxyService.engine);
        deadline=System.currentTimeMillis()+5000;
        while(!com.nexvary.trafficlab.vpn.LabVpnService.state.equals("STOPPED")&&System.currentTimeMillis()<deadline)Thread.sleep(25);
        assertEquals("Exit left VPN active","STOPPED",com.nexvary.trafficlab.vpn.LabVpnService.state);assertFalse(hev.htproxy.TProxyService.TProxyIsRunning());
        try {assertEquals("Exit left an active TLS connection open",-1,active.getInputStream().read());}finally{active.close();}
        for(ActivityManager.AppTask task:activity.getSystemService(ActivityManager.class).getAppTasks())assertTrue("Exit left its task in recents",task.getTaskInfo().taskId!=exitedTask);
        try(Socket socket=new Socket()){socket.connect(new InetSocketAddress("127.0.0.1",8888),500);fail("Exit left proxy listener open");}catch(java.io.IOException expected){}
        try(Socket socket=new Socket()){socket.connect(new InetSocketAddress("127.0.0.1",18088),500);fail("Exit left demo server open");}catch(java.io.IOException expected){}
        Instrumentation.ActivityMonitor loginMonitor=getInstrumentation().addMonitor(LoginActivity.class.getName(),null,false);
        getInstrumentation().getTargetContext().startActivity(new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Activity login=getInstrumentation().waitForMonitorWithTimeout(loginMonitor,5000);assertNotNull("Reopening after exit did not require login",login);
        getInstrumentation().runOnMainSync(()->login.finish());getInstrumentation().removeMonitor(loginMonitor);
    }
    public void testVpnAndArchiveNavigationReturnsToHistory() throws Exception {
        click("nav:History");
        Instrumentation.ActivityMonitor archiveMonitor=getInstrumentation().addMonitor(SessionArchiveActivity.class.getName(),null,false);
        click("history:archive");Activity archive=getInstrumentation().waitForMonitorWithTimeout(archiveMonitor,5000);assertNotNull(archive);
        getInstrumentation().runOnMainSync(()->{View back=find(archive.getWindow().getDecorView(),"archive:back");assertNotNull(back);back.performClick();});
        getInstrumentation().waitForIdleSync();assertEquals("History",route());getInstrumentation().removeMonitor(archiveMonitor);
        click("nav:Packet Lab");
        Instrumentation.ActivityMonitor vpnMonitor=getInstrumentation().addMonitor(VpnLabActivity.class.getName(),null,false);
        click("packet:vpn");Activity vpn=getInstrumentation().waitForMonitorWithTimeout(vpnMonitor,5000);assertNotNull(vpn);
        getInstrumentation().runOnMainSync(()->{View back=find(vpn.getWindow().getDecorView(),"vpn:back");assertNotNull(back);back.performClick();});
        getInstrumentation().waitForIdleSync();assertEquals("Packet Lab",route());getInstrumentation().removeMonitor(vpnMonitor);
        click("nav:back");assertEquals("History",route());
    }
    public void testArabicNavigation() throws Exception {
        getInstrumentation().runOnMainSync(()->activity.finish());getInstrumentation().waitForIdleSync();
        LocaleController.setLanguage(getInstrumentation().getTargetContext(),"ar");AuthSession.grant();
        activity=(MainActivity)getInstrumentation().startActivitySync(new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getInstrumentation().waitForIdleSync();click("nav:About");assertEquals("About",route());click("nav:back");assertEquals("Dashboard",route());
        testEveryNavigationButtonAndBack();
    }
}
