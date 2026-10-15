package com.nexvary.trafficlab;
import android.app.*;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.VpnService;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.nexvary.trafficlab.vpn.LabVpnService;
import java.util.*;

/** Explicit selection and Android VPN consent, with live native packet counters. */
public final class VpnLabActivity extends Activity {
    private EditText packages,host,port;
    private TextView status;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean resumed;
    private final Runnable poll=new Runnable(){public void run(){if(resumed){
        long[] n=LabVpnService.stats();status.setText(LabVpnService.state+"\nTX "+n[0]+" packets / "+n[1]+" B · RX "+n[2]+" packets / "+n[3]+" B");
        handler.postDelayed(this,1000);
    }}};
    private String tr(String ar,String en){return "ar".equals(LocaleController.getLanguage(this))?ar:en;}
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleController.wrap(base));}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);if(!AuthSession.isUnlocked()){startActivity(new Intent(this,LoginActivity.class));finish();return;}
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(24,24,24,24);page.setBackgroundColor(Color.rgb(2,9,18));
        page.setLayoutDirection("ar".equals(LocaleController.getLanguage(this))?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
        text(page,tr("شبكة المختبر المدمجة","Integrated VPN Lab"),24);
        text(page,tr("اختر تطبيقات التدريب؛ تمر اتصالات TCP وUDP عبر محرك NEXVARY المحلي دون إعداد Proxy داخلها. سيطلب Android إذن VPN، ويحل هذا الاتصال محل أي VPN آخر على الجهاز.",
            "Select training apps. TCP and UDP use the local NEXVARY engine without app proxy settings. Android asks for VPN consent; this replaces any other device VPN."),15);
        packages=input(page,getPreferences(0).getString("packages",""),"vpn:packages");
        packages.setHint(tr("أسماء الحزم، مفصولة بفاصلة","Package names, comma separated"));
        button(page,tr("اختيار تطبيقات التدريب","Choose training apps"),"vpn:choose",()->choose(packages));
        text(page,tr("فك HTTPS اختياري: شغّل محرك HTTPS وثبّت الثقة بشهادته أولًا. يُفحص اسم خادم المختبر المحدد فقط عند استخدام DNS المختبر. بقية الاتصالات تمر كما هي. Pinning وDNS المشفر قد يمنعان الفحص.",
            "HTTPS inspection is optional: start the HTTPS engine and configure CA trust first. Only the configured lab hostname is inspected via lab DNS. Other connections pass through. Pinning and encrypted DNS can prevent inspection."),14);
        host=input(page,getIntent().getStringExtra("host")==null?"api.training.example":getIntent().getStringExtra("host"),"vpn:host");
        port=input(page,String.valueOf(getIntent().getIntExtra("port",443)),"vpn:port");port.setInputType(2);
        status=text(page,LabVpnService.state,14);status.setTag("vpn:state");
        button(page,tr("تشغيل VPN للتطبيقات المحددة","Start VPN for selected apps"),"vpn:start",()->{
            try{selection();Intent consent=VpnService.prepare(this);if(consent!=null)startActivityForResult(consent,91);else start();}
            catch(Exception error){status.setText(error.getMessage());}
        });
        button(page,tr("إيقاف VPN","Stop VPN"),"vpn:stop",()->stopService(new Intent(this,LabVpnService.class)));
        button(page,tr("رجوع","Back"),"vpn:back",this::finish);
    }
    private String[] selection(){
        String raw=packages.getText().toString().trim();if(raw.isEmpty())throw new IllegalArgumentException(tr("اختر تطبيقًا واحدًا على الأقل","Select at least one app"));
        Set<String> names=new LinkedHashSet<>();
        for(String name:raw.split("[,\\s]+")){
            if(name.equals(getPackageName())||!name.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))throw new IllegalArgumentException("Invalid training package: "+name);
            names.add(name);
        }return names.toArray(new String[0]);
    }
    private void start(){
        try{
            String[] selected=selection();String h=host.getText().toString().trim();int p=Integer.parseInt(port.getText().toString());
            if(!h.matches("[A-Za-z0-9][A-Za-z0-9.-]{0,252}")||p<1||p>65535)throw new IllegalArgumentException("Invalid lab host / port");
            getPreferences(0).edit().putString("packages",String.join(",",selected)).apply();
            startForegroundService(new Intent(this,LabVpnService.class).putExtra("packages",selected).putExtra("host",h).putExtra("port",p));
        }catch(Exception error){status.setText(error.getMessage());}
    }
    private void choose(EditText field){
        List<ResolveInfo> apps=getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0);
        TreeMap<String,String> names=new TreeMap<>();
        for(ResolveInfo app:apps)if(!getPackageName().equals(app.activityInfo.packageName))names.put(app.activityInfo.packageName,String.valueOf(app.loadLabel(getPackageManager())));
        String[] ids=names.keySet().toArray(new String[0]),labels=new String[ids.length];boolean[] checks=new boolean[ids.length];
        Set<String> selected=new LinkedHashSet<>(Arrays.asList(field.getText().toString().split("[,\\s]+")));selected.remove("");
        for(int i=0;i<ids.length;i++){labels[i]=names.get(ids[i])+"\n"+ids[i];checks[i]=selected.contains(ids[i]);}
        new AlertDialog.Builder(this).setTitle(tr("تطبيقات التدريب","Training applications")).setMultiChoiceItems(labels,checks,(dialog,index,checked)->{if(checked)selected.add(ids[index]);else selected.remove(ids[index]);})
                .setPositiveButton(android.R.string.ok,(dialog,which)->field.setText(String.join(",",selected))).setNegativeButton(android.R.string.cancel,null).show();
    }
    private TextView text(LinearLayout p,String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setPadding(0,12,0,16);p.addView(v);return v;}
    private EditText input(LinearLayout p,String s,String tag){EditText v=new EditText(this);v.setText(s);v.setTextColor(Color.WHITE);v.setHintTextColor(Color.LTGRAY);v.setSingleLine(true);v.setTag(tag);p.addView(v);return v;}
    private void button(LinearLayout p,String s,String tag,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTag(tag);b.setOnClickListener(v->action.run());p.addView(b);}
    @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);if(code==91){if(result==RESULT_OK)start();else status.setText(tr("لم يُمنح إذن VPN","VPN permission was not granted"));}}
    @Override protected void onResume(){super.onResume();resumed=true;handler.post(poll);}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(poll);super.onPause();}
}
