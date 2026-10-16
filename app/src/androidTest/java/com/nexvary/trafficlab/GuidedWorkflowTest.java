package com.nexvary.trafficlab;
import android.app.*;
import android.content.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import com.nexvary.trafficlab.tls.*;
import javax.net.ssl.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.concurrent.*;

public final class GuidedWorkflowTest extends InstrumentationTestCase {
 private View find(View v,String tag){if(tag.equals(v.getTag()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=find(g.getChildAt(i),tag);if(found!=null)return found;}}return null;}
 private void capture(String name)throws Exception{getInstrumentation().waitForIdleSync();android.graphics.Bitmap image=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);File folder=getInstrumentation().getTargetContext().getExternalFilesDir("qa");if(!folder.exists())folder.mkdirs();try(OutputStream out=new FileOutputStream(new File(folder,name+".png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}}
 private void click(Activity a,String tag)throws Exception{getInstrumentation().runOnMainSync(()->{View v=find(a.getWindow().getDecorView(),tag);assertNotNull(tag,v);v.performClick();});getInstrumentation().waitForIdleSync();}
 public void testFiveStepsActualWebsiteProbeAndNavigation()throws Exception{
  Context context=getInstrumentation().getTargetContext();LocaleController.setLanguage(context,"ar");AuthSession.grant();LabProbe.stop();EmbeddedProxyService.stopNow();context.stopService(new Intent(context,EmbeddedProxyService.class));
  LabCertificateAuthority ca=LabCertificateAuthority.open(context);KeyStore store=KeyStore.getInstance(KeyStore.getDefaultType());store.load(null);store.setCertificateEntry("lab",ca.certificate());TrustManagerFactory tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(store);SSLContext trust=SSLContext.getInstance("TLS");trust.init(null,tm.getTrustManagers(),null);
  ExecutorService pool=Executors.newSingleThreadExecutor();GuidedLabActivity activity=null;MainActivity main=null;
  try(SSLServerSocket server=(SSLServerSocket)ca.serverContext("127.0.0.1").getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"));EmbeddedTlsProxy proxy=new EmbeddedTlsProxy(ca,"127.0.0.1",server.getLocalPort(),false,trust)){
   server.setSoTimeout(15000);Future<?> response=pool.submit(()->{try(SSLSocket socket=(SSLSocket)server.accept()){socket.setSoTimeout(10000);ByteArrayOutputStream head=new ByteArrayOutputStream();int tail=0;while(tail!=0x0d0a0d0a){int b=socket.getInputStream().read();if(b<0)throw new EOFException();head.write(b);tail=(tail<<8)|b;}assertTrue(head.toString("US-ASCII").startsWith("GET /wizard HTTP/1.1"));socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 9\r\nConnection: close\r\n\r\nwizard-ok".getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();}catch(Exception e){throw new RuntimeException(e);}});
   proxy.start(8888);EmbeddedProxyService.engine=proxy;EmbeddedProxyService.state="RUNNING";
   main=(MainActivity)getInstrumentation().startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   capture("01-dashboard");Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(GuidedLabActivity.class.getName(),null,false);click(main,"workflow:open");activity=(GuidedLabActivity)getInstrumentation().waitForMonitorWithTimeout(monitor,5000);getInstrumentation().removeMonitor(monitor);assertNotNull(activity);
   GuidedLabActivity screen=activity;capture("02-website");click(screen,"workflow:next");assertNotNull("Empty target advanced",find(screen.getWindow().getDecorView(),"workflow:target"));
   getInstrumentation().runOnMainSync(()->((EditText)find(screen.getWindow().getDecorView(),"workflow:target")).setText("https://127.0.0.1:"+server.getLocalPort()+"/wizard"));click(screen,"workflow:next");capture("03-setup");assertNotNull(find(screen.getWindow().getDecorView(),"workflow:start"));
   click(screen,"workflow:next");click(screen,"workflow:http2");click(screen,"workflow:test");long until=System.currentTimeMillis()+10000;while(!LabProbe.state.equals("COMPLETE")&&System.currentTimeMillis()<until)Thread.sleep(20);assertEquals(LabProbe.result,"COMPLETE",LabProbe.state);assertTrue(LabProbe.result,LabProbe.result.contains("wizard-ok"));response.get(5,TimeUnit.SECONDS);capture("04-test");
   click(screen,"workflow:next");Instrumentation.ActivityMonitor https=getInstrumentation().addMonitor(HttpsLabActivity.class.getName(),null,false);click(screen,"workflow:inspect");Activity editor=getInstrumentation().waitForMonitorWithTimeout(https,5000);assertNotNull(editor);assertEquals("127.0.0.1",((EditText)find(editor.getWindow().getDecorView(),"tls:host")).getText().toString());click(editor,"tls:back");getInstrumentation().removeMonitor(https);
   click(screen,"workflow:next");capture("05-export");Instrumentation.ActivityMonitor archive=getInstrumentation().addMonitor(SessionArchiveActivity.class.getName(),null,false);click(screen,"workflow:archive");Activity files=getInstrumentation().waitForMonitorWithTimeout(archive,5000);assertNotNull(files);click(files,"archive:back");getInstrumentation().removeMonitor(archive);
   Instrumentation.ActivityMonitor vpn=getInstrumentation().addMonitor(VpnLabActivity.class.getName(),null,false);click(screen,"workflow:pcap");Activity network=getInstrumentation().waitForMonitorWithTimeout(vpn,5000);assertNotNull(network);click(network,"vpn:back");getInstrumentation().removeMonitor(vpn);
   click(screen,"workflow:previous");assertNotNull(find(screen.getWindow().getDecorView(),"workflow:inspect"));click(screen,"workflow:again");assertNotNull(find(screen.getWindow().getDecorView(),"workflow:test"));click(screen,"workflow:next");click(screen,"workflow:next");click(screen,"workflow:reset");assertNotNull(find(screen.getWindow().getDecorView(),"workflow:target"));click(screen,"workflow:back");assertTrue(screen.isFinishing()||screen.isDestroyed());
  }finally{LabProbe.stop();EmbeddedProxyService.engine=null;EmbeddedProxyService.state="STOPPED";pool.shutdownNow();if(activity!=null){Activity close=activity;getInstrumentation().runOnMainSync(close::finish);}if(main!=null){Activity close=main;getInstrumentation().runOnMainSync(close::finish);}LocaleController.setLanguage(context,"en");AuthSession.revoke();}
 }
}
