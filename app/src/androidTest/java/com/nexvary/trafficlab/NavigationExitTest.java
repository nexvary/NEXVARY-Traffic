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
        if(activity!=null && !activity.isFinishing())getInstrumentation().runOnMainSync(()->activity.finish());
        getInstrumentation().waitForIdleSync(); AuthSession.revoke(); super.tearDown();
    }
    private Object field(String name) throws Exception {Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private String route() throws Exception {return (String)field("currentScreen");}
    private View find(View root,String tag){if(tag.equals(root.getTag()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View v=find(g.getChildAt(i),tag);if(v!=null)return v;}}return null;}
    private void click(String tag){
        getInstrumentation().runOnMainSync(()->{View view=find(activity.getWindow().getDecorView(),tag);assertNotNull("Missing button "+tag,view);view.requestRectangleOnScreen(new Rect(0,0,view.getWidth(),view.getHeight()),true);});
        getInstrumentation().waitForIdleSync();final Rect bounds=new Rect();
        getInstrumentation().runOnMainSync(()->{View view=find(activity.getWindow().getDecorView(),tag);assertTrue("Button not visible "+tag,view.getGlobalVisibleRect(bounds));});
        long now=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
        MotionEvent up=MotionEvent.obtain(now,now+50,MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
        try{getInstrumentation().sendPointerSync(down);getInstrumentation().sendPointerSync(up);}finally{down.recycle();up.recycle();}
        getInstrumentation().waitForIdleSync();
    }
    private void collect(View root,Set<String> tags){Object tag=root.getTag();if(tag instanceof String && ((String)tag).startsWith("nav:") && !tag.equals("nav:back"))tags.add((String)tag);if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),tags);}}
    public void testEveryNavigationButtonAndBack() throws Exception {
        Set<String> tags=new LinkedHashSet<>();getInstrumentation().runOnMainSync(()->collect(activity.getWindow().getDecorView(),tags));
        assertTrue("Expected advanced page coverage",tags.size()>30);
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
        click("nav:back");assertEquals("Repeater",route());click("nav:back");assertEquals("Intercept",route());
        click("nav:back");assertEquals("Dashboard",route());click("nav:back");assertEquals("Dashboard",route());
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
        click("nav:About");click("app:exit");AlertDialog dialog=(AlertDialog)field("exitDialog");
        getInstrumentation().runOnMainSync(()->dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
        assertFalse(activity.isFinishing());assertTrue(AuthSession.isUnlocked());assertTrue(EmbeddedProxyService.engine.isRunning());
        click("app:exit");AlertDialog confirm=(AlertDialog)field("exitDialog");
        getInstrumentation().runOnMainSync(()->confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick());getInstrumentation().waitForIdleSync();
        assertTrue(activity.isFinishing()||activity.isDestroyed());assertFalse(AuthSession.isUnlocked());
        deadline=System.currentTimeMillis()+5000;while(EmbeddedProxyService.engine!=null && System.currentTimeMillis()<deadline)Thread.sleep(25);
        assertNull("Exit left capture running",EmbeddedProxyService.engine);
        try(Socket socket=new Socket()){socket.connect(new InetSocketAddress("127.0.0.1",8888),500);fail("Exit left proxy listener open");}catch(java.io.IOException expected){}
        try(Socket socket=new Socket()){socket.connect(new InetSocketAddress("127.0.0.1",18088),500);fail("Exit left demo server open");}catch(java.io.IOException expected){}
    }
    public void testArabicNavigation() throws Exception {
        getInstrumentation().runOnMainSync(()->activity.finish());getInstrumentation().waitForIdleSync();
        LocaleController.setLanguage(getInstrumentation().getTargetContext(),"ar");AuthSession.grant();
        activity=(MainActivity)getInstrumentation().startActivitySync(new Intent(getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getInstrumentation().waitForIdleSync();click("nav:About");assertEquals("About",route());click("nav:back");assertEquals("Dashboard",route());
    }
}
