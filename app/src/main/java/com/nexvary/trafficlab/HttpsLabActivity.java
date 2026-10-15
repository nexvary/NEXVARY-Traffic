package com.nexvary.trafficlab;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import com.nexvary.trafficlab.tls.*;
import java.io.OutputStream;
import java.util.List;

/** Self-contained explicit HTTPS lab proxy, CA export, request gate and flow viewer. */
public final class HttpsLabActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView state, response;
    private EditText host, port, request;
    private CheckBox pause;
    private Spinner selector;
    private List<EmbeddedTlsProxy.Flow> visible=java.util.Collections.emptyList();
    private long selected=-1;
    private String signature="";
    private byte[] publicCertificate;
    private boolean foreground;
    private final Runnable poll=new Runnable(){public void run(){if(foreground){refresh();handler.postDelayed(this,1000);}}};
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleController.wrap(base));}
    private String tr(String ar,String en){return "ar".equals(LocaleController.getLanguage(this))?ar:en;}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        if(!AuthSession.isUnlocked()){startActivity(new Intent(this,LoginActivity.class));finish();return;}
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(24,24,24,24);page.setBackgroundColor(Color.rgb(2,9,18));
        page.setLayoutDirection("ar".equals(LocaleController.getLanguage(this))?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
        text(page,tr("مختبر HTTPS المدمج","Embedded HTTPS Lab"),24);
        text(page,tr("المحرك داخل NEXVARY؛ لا يحتاج PCAPdroid. اضبط Proxy في تطبيق التدريب أو إعدادات Wi-Fi على 127.0.0.1 والمنفذ 8888. يقبل المحرك وجهة المختبر المحددة فقط.",
                "Engine runs inside NEXVARY. Configure your training app or Wi-Fi proxy as 127.0.0.1:8888. Only the configured lab destination is accepted."),15);
        text(page,tr("هذا المسار يدعم HTTPS عبر CONNECT وHTTP/1.1، وليس VPN لالتقاط كل التطبيقات. التطبيق المستهدف يجب أن يثق بشهادة المختبر. لا يدعم تجاوز Pinning أو QUIC أو ترقيات WebSocket بعد.",
                "This path supports HTTPS CONNECT and HTTP/1.1, not all-app VPN capture. The target must trust the lab CA. Pinning bypass, QUIC and WebSocket upgrades are not implemented."),14);
        host=input(page,"api.training.example","tls:host");host.setHint(tr("اسم خادم التدريب","Training server hostname"));
        port=input(page,"443","tls:port");port.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        pause=new CheckBox(this);pause.setText(tr("إيقاف الطلب قبل إرساله للتحرير","Pause request before forwarding"));pause.setTextColor(Color.WHITE);pause.setChecked(true);page.addView(pause);
        state=text(page,"",14);state.setTag("tls:state");
        button(page,tr("إنشاء وتصدير شهادة المختبر","Create and export lab CA"),"tls:certificate",this::exportCertificate);
        button(page,tr("إعدادات شهادات Android","Android certificate settings"),"tls:settings",()->startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)));
        button(page,tr("تشغيل المحرك","Start engine"),"tls:start",()->{
            try{
                new EmbeddedTlsProxy(null,host.getText().toString().trim(),Integer.parseInt(port.getText().toString()),true,javax.net.ssl.SSLContext.getDefault()).close();
                startForegroundService(new Intent(this,EmbeddedProxyService.class).putExtra("host",host.getText().toString().trim())
                        .putExtra("port",Integer.parseInt(port.getText().toString())).putExtra("pause",pause.isChecked()));
            }catch(Exception error){state.setText(error.getMessage());}
        });
        button(page,tr("إيقاف المحرك","Stop engine"),"tls:stop",()->{stopService(new Intent(this,EmbeddedProxyService.class));refresh();});
        selector=new Spinner(this);page.addView(selector);selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){if(position<visible.size()){selected=visible.get(position).id;showSelected();}}
        });
        text(page,tr("الطلب: يمكن تعديل النص عند حالة PAUSED. افصل الترويسات عن الجسم بسطر فارغ.","Request: editable in PAUSED state. Separate headers and body with a blank line."),14);
        request=input(page,"","tls:request");request.setSingleLine(false);request.setMinLines(6);request.setMaxLines(16);
        button(page,tr("تمرير الطلب الأصلي","Forward original request"),"tls:original",()->{try{if(EmbeddedProxyService.engine==null)throw new IllegalStateException("Engine stopped");EmbeddedProxyService.engine.decide(selected,null,false);refresh();}catch(Exception error){state.setText(error.getMessage());}});
        button(page,tr("تمرير الطلب المعدل","Forward edited request"),"tls:forward",()->decide(false));
        button(page,tr("إسقاط الطلب","Drop request"),"tls:drop",()->decide(true));
        response=text(page,"",13);response.setTextIsSelectable(true);response.setTag("tls:response");
        button(page,tr("رجوع","Back"),"tls:back",this::finish);
    }
    private TextView text(LinearLayout page,String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setPadding(0,12,0,16);page.addView(v);return v;}
    private EditText input(LinearLayout page,String value,String tag){EditText v=new EditText(this);v.setText(value);v.setTextColor(Color.WHITE);v.setHintTextColor(Color.LTGRAY);v.setSingleLine(true);v.setTag(tag);page.addView(v);return v;}
    private void button(LinearLayout page,String title,String tag,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTag(tag);b.setOnClickListener(v->action.run());page.addView(b);}
    private void exportCertificate(){
        state.setText(tr("جار تجهيز الشهادة…","Preparing certificate…"));
        new Thread(()->{try{byte[] certificate=LabCertificateAuthority.open(this).encoded();runOnUiThread(()->{
            if(isFinishing()||isDestroyed())return;publicCertificate=certificate;
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/x-x509-ca-cert").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"NEXVARY-Lab-CA.crt"),77);
        });}catch(Exception error){runOnUiThread(()->state.setText(error.toString()));}},"nexvary-ca-export").start();
    }
    @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);
        if(code==77 && result==RESULT_OK && data!=null && data.getData()!=null && publicCertificate!=null){
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new java.io.IOException("Cannot open document");out.write(publicCertificate);state.setText(tr("صُدرت الشهادة العامة فقط. ثبّتها كشهادة CA من إعدادات Android وأعد تطبيق التدريب.","Public CA exported. Install it as a CA in Android settings and configure the training app."));}
            catch(Exception error){state.setText(error.toString());}finally{publicCertificate=null;}
        }
    }
    private void decide(boolean drop){try{EmbeddedTlsProxy engine=EmbeddedProxyService.engine;if(engine==null)throw new IllegalStateException("Engine stopped");
        String edited=request.getText().toString().replace("\r\n","\n").replace("\n","\r\n");engine.decide(selected,drop?null:edited,drop);refresh();
    }catch(Exception error){state.setText(error.getMessage());}}
    private void refresh(){
        if(state==null)return;state.setText(EmbeddedProxyService.state+" · 127.0.0.1:8888");
        EmbeddedTlsProxy engine=EmbeddedProxyService.engine;visible=engine==null?java.util.Collections.emptyList():engine.flows();
        StringBuilder key=new StringBuilder();String[] labels=new String[visible.size()];int position=0;
        for(int i=0;i<visible.size();i++){EmbeddedTlsProxy.Flow flow=visible.get(i);labels[i]="#"+flow.id+" "+flow.host+" · "+flow.status;key.append(labels[i]);if(flow.id==selected)position=i;}
        if(!signature.equals(key.toString())){signature=key.toString();selector.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));selector.setSelection(position);}
        if(selected<0 && !visible.isEmpty())selected=visible.get(0).id;
        showSelected();
    }
    private void showSelected(){for(EmbeddedTlsProxy.Flow flow:visible)if(flow.id==selected){
        if(!request.hasFocus() || !flow.status.equals("PAUSED"))request.setText(flow.request);
        request.setEnabled(flow.status.equals("PAUSED"));response.setText(flow.status+"\n"+flow.error+"\n"+flow.response);return;
    }}
    @Override protected void onResume(){super.onResume();foreground=true;handler.post(poll);}
    @Override protected void onPause(){foreground=false;handler.removeCallbacks(poll);super.onPause();}
}
