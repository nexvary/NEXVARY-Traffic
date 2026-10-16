package com.nexvary.trafficlab;
import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Color;
import android.widget.*;
import com.nexvary.trafficlab.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class SessionArchiveActivity extends Activity {
    private TextView state;
    private CheckBox includeSecrets;
    private byte[] export;
    private long captureSession;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private String tr(String ar,String en){return "ar".equals(LocaleController.getLanguage(this))?ar:en;}
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleController.wrap(base));}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);if(!AuthSession.isUnlocked()){finish();return;}
        captureSession=CapturedTraffic.archiveSession();
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(24,24,24,24);page.setBackgroundColor(Color.rgb(2,9,18));
        ScrollView scroll=new ScrollView(this);scroll.addView(page);LabIcons.insets(scroll);setContentView(scroll);
        text(page,tr("أرشيف جلسات المختبر","Lab session archive"),24);
        text(page,tr("تصدير HAR يخفي الأجسام وقيم الاستعلامات والترويسات الحساسة افتراضيًا. المعاينات النصية قد تكون مبتورة، ولا تمثل نسخة بايتية كاملة من البيانات.","HAR exports omit bodies, query values and sensitive headers by default. Text previews can be truncated and are not byte-exact captures."),15);
        state=text(page,"",14);state.setTag("archive:state");refresh();
        includeSecrets=new CheckBox(this);includeSecrets.setText(tr("تضمين معاينات البيانات الحساسة في التصدير الكامل","Include sensitive text previews in full export"));includeSecrets.setTextColor(Color.WHITE);page.addView(includeSecrets);
        button(page,tr("تصدير HAR","Export HAR"),"archive:export",()->{
            export=HarExport.create(CapturedTraffic.all(),includeSecrets.isChecked()).getBytes(StandardCharsets.UTF_8);
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"NEXVARY-Session.har"),81);
        });
        button(page,tr("استيراد HAR للتحليل","Import HAR for analysis"),"archive:import",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),82));
        text(page,tr("الحفظ المحلي اختياري، ويحتفظ بمعاينات الجلسة الحساسة مشفرة بمفتاح هذا الجهاز. زر الخروج يمسح الجلسة المؤقتة؛ الأرشيف المحفوظ يبقى حتى تحذفه.","Local saving is optional and retains sensitive session previews encrypted with this device's key. Exit clears the temporary session; the saved archive remains until deleted."),14);
        button(page,tr("حفظ الجلسة مشفرة على الجهاز","Save encrypted session on device"),"archive:save",()->{
            List<CapturedTraffic.Exchange> entries=CapturedTraffic.all();work(()->{CaptureFiles.save(this,entries);return tr("حُفظت الجلسة مشفرة","Session saved encrypted");});
        });
        button(page,tr("استعادة الجلسة المحفوظة","Restore saved session"),"archive:restore",()->work(()->{List<String[]> rows=CaptureFiles.parse(CaptureFiles.load(this));importRows(rows,"RESTORED_CAPTURE");return tr("استُعيدت الجلسة","Saved session restored");}));
        button(page,tr("مسح الجلسات والأرشيف المحفوظ","Clear sessions and saved archive"),"archive:clear",()->work(()->{CaptureFiles.delete(this);CapturedTraffic.clear();return tr("مُسحت الجلسات والأرشيف","Sessions and saved archive cleared");}));
        button(page,tr("رجوع","Back"),"archive:back",this::finish);
    }
    private interface Task{String run()throws Exception;}
    private void work(Task task){state.setText(tr("جار التنفيذ…","Working…"));worker.execute(()->{String message;try{message=task.run();}catch(Exception e){message="ERROR: "+e.getMessage();}String result=message;runOnUiThread(()->{if(!isDestroyed()){state.setText(result+"\n"+CapturedTraffic.all().size()+" / 32");}});});}
    private void importRows(List<String[]> rows,String source){
        if(!AuthSession.isUnlocked())return;
        for(String[] row:rows)CapturedTraffic.imported(captureSession,row[0],row[1],row[2],Long.parseLong(row[3]),Long.parseLong(row[4]),source);
    }
    @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null){if(code==81)export=null;return;}
        if(code==81&&export!=null){byte[] payload=export;export=null;work(()->{try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException("Cannot open document");out.write(payload);}return tr("صُدّر HAR","HAR exported");});}
        if(code==82)work(()->{String content;try(InputStream in=getContentResolver().openInputStream(data.getData())){if(in==null)throw new IOException("Cannot read document");content=new String(CaptureFiles.bounded(in),StandardCharsets.UTF_8);}List<String[]> rows=CaptureFiles.parse(content);importRows(rows,"HAR_IMPORT");return tr("استُورد HAR للتحليل","HAR imported for analysis");});
    }
    private void refresh(){state.setText(CapturedTraffic.all().size()+" / 32 "+tr("جلسة مؤقتة","temporary exchanges"));}
    private TextView text(LinearLayout p,String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setPadding(0,12,0,16);p.addView(v);return v;}
    private void button(LinearLayout p,String s,String tag,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTag(tag);LabIcons.tagged(b,tag);b.setOnClickListener(v->action.run());p.addView(b);}
    @Override protected void onDestroy(){export=null;worker.shutdown();super.onDestroy();}
}
