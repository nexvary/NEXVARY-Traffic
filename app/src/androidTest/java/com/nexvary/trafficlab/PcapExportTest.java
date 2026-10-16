package com.nexvary.trafficlab;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.test.InstrumentationTestCase;
import android.view.*;
import com.nexvary.trafficlab.vpn.*;
import java.io.*;
import java.nio.*;

/** Real content-provider destination and the VPN screen's export result handler. */
public final class PcapExportTest extends InstrumentationTestCase {
    private View find(View root,String tag){if(tag.equals(root.getTag()))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),tag);if(found!=null)return found;}}return null;}
    public void testVpnScreenExportsCompletePcapAndDeletesTemporaryFile()throws Exception{
        Context context=getInstrumentation().getTargetContext();AuthSession.grant();LabVpnService.stopNow();
        VpnPacketCapture.clear(context);
        byte[] packet=new byte[]{0x45,0,0,20,0,0,0,0,64,1,0,0,127,0,0,1,127,0,0,1};
        VpnPacketCapture.begin(context).record(packet,packet.length,1234567);
        VpnLabActivity activity=(VpnLabActivity)getInstrumentation().startActivitySync(new Intent(context,VpnLabActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,"nexvary-ci-"+System.nanoTime()+".pcap");values.put(MediaStore.Downloads.MIME_TYPE,"application/vnd.tcpdump.pcap");values.put(MediaStore.Downloads.IS_PENDING,1);
        Uri uri=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);assertNotNull(uri);
        IntentFilter pickerFilter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);pickerFilter.addDataType("application/vnd.tcpdump.pcap");pickerFilter.addCategory(Intent.CATEGORY_OPENABLE);
        Instrumentation.ActivityMonitor picker=getInstrumentation().addMonitor(pickerFilter,new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);
        try{
            getInstrumentation().runOnMainSync(()->find(activity.getWindow().getDecorView(),"vpn:export").performClick());
            assertEquals("Active recording must not launch the picker",0,picker.getHits());VpnPacketCapture.stop();
            getInstrumentation().runOnMainSync(()->find(activity.getWindow().getDecorView(),"vpn:export").performClick());
            assertEquals("Export button did not launch document creation",1,picker.getHits());
            getInstrumentation().runOnMainSync(()->activity.onActivityResult(92,Activity.RESULT_OK,new Intent().setData(uri)));
            byte[] data=new byte[0];long deadline=System.currentTimeMillis()+5000;
            while(data.length<60&&System.currentTimeMillis()<deadline){try(InputStream in=context.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] block=new byte[128];int n;while((n=in.read(block))!=-1)out.write(block,0,n);data=out.toByteArray();}if(data.length<60)Thread.sleep(25);}
            assertEquals(60,data.length);ByteBuffer pcap=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);assertEquals(0xa1b2c3d4,pcap.getInt());pcap.position(20);assertEquals(101,pcap.getInt());
            assertEquals(1234,pcap.getInt());assertEquals(567000,pcap.getInt());assertEquals(20,pcap.getInt());assertEquals(20,pcap.getInt());byte[] actual=new byte[20];pcap.get(actual);assertTrue(java.util.Arrays.equals(packet,actual));
            getInstrumentation().runOnMainSync(()->find(activity.getWindow().getDecorView(),"vpn:clear").performClick());assertEquals("EMPTY",VpnPacketCapture.status(context));
            getInstrumentation().runOnMainSync(()->find(activity.getWindow().getDecorView(),"vpn:back").performClick());assertTrue(activity.isFinishing()||activity.isDestroyed());
        }finally{getInstrumentation().removeMonitor(picker);context.getContentResolver().delete(uri,null,null);VpnPacketCapture.stop();VpnPacketCapture.clear(context);getInstrumentation().runOnMainSync(activity::finish);AuthSession.revoke();}
    }
}
