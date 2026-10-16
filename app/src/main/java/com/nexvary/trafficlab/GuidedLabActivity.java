package com.nexvary.trafficlab;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.nexvary.trafficlab.core.LabTarget;
import com.nexvary.trafficlab.tls.*;

/** Site -> setup -> test -> inspect -> export, with real engine/state gates. */
public final class GuidedLabActivity extends Activity {
    private int step;
    private String targetText="";
    private LabTarget target;
    private LinearLayout page;
    private TextView live;
    private boolean useHttp2;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean resumed;
    private String tr(String ar,String en){return "ar".equals(LocaleController.getLanguage(this))?ar:en;}
    private final Runnable poll=new Runnable(){public void run(){if(resumed){if(live!=null)live.setText(EmbeddedProxyService.state+"\nTEST: "+LabProbe.state+"\n"+LabProbe.result);handler.postDelayed(this,700);}}};
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(LocaleController.wrap(base));}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);if(!AuthSession.isUnlocked()){finish();return;}
        if(saved!=null){targetText=saved.getString("target","");step=saved.getInt("step",0);useHttp2=saved.getBoolean("h2",false);if(!targetText.isEmpty())try{target=LabTarget.parse(targetText);}catch(Exception ignored){step=0;}}
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->{if(step>0){step--;render();}else finish();});
        render();
    }
    private void render(){
        live=null;
        page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);int pad=dp(20);page.setPadding(pad,pad,pad,pad);page.setBackgroundColor(Color.rgb(2,9,18));page.setLayoutDirection("ar".equals(LocaleController.getLanguage(this))?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(page);LabIcons.insets(scroll);setContentView(scroll);
        String[] names={tr("الموقع","Website"),tr("إعداد HTTPS","HTTPS setup"),tr("بدء الاختبار","Run test"),tr("مراجعة الطلبات","Inspect traffic"),tr("تصدير النتائج","Export results")};
        int[] icons={R.drawable.color_globe,R.drawable.color_shield,R.drawable.color_play,R.drawable.color_search,R.drawable.color_folder};
        ImageView image=new ImageView(this);image.setImageResource(icons[step]);image.setContentDescription(names[step]);page.addView(image,new LinearLayout.LayoutParams(-1,dp(80)));
        text(tr("الخطوة "+(step+1)+" من 5 · "+names[step],(step+1)+" / 5 · "+names[step]),25);text(tr("الموقع  ←  الإعداد  ←  الاختبار  ←  المراجعة  ←  التصدير","Website  →  Setup  →  Test  →  Inspect  →  Export"),13);
        if(step==0){
            text(tr("أدخل رابط HTTPS الذي تريد فحصه داخل المختبر.","Enter the HTTPS website to test in your lab."),16);
            EditText input=new EditText(this);input.setTextColor(Color.WHITE);input.setHintTextColor(Color.LTGRAY);input.setHint("https://your-lab.example/path");input.setText(targetText);input.setSingleLine(true);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);input.setTag("workflow:target");page.addView(input);
            input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){targetText=s.toString();}public void afterTextChanged(android.text.Editable e){}});
            button(tr("التالي: إعداد HTTPS","Next: HTTPS setup"),"workflow:next",()->{try{target=LabTarget.parse(targetText);step=1;render();}catch(Exception e){input.setError(tr("أدخل رابط HTTPS صحيحًا دون كلمة مرور أو #","Enter a valid HTTPS URL without credentials or #"));}});
            button(tr("درس محلي دون إنترنت","Offline lesson"),"workflow:lesson",()->startActivity(new Intent(this,HttpsLabActivity.class)));
        }else{
            text(target.host()+":"+target.port()+target.path(),15);
            if(step==1){
                text(tr("ابدأ المحرك للموقع المحدد. اختبار الموقع الداخلي يثق بشهادة المختبر تلقائيًا؛ التطبيقات الخارجية تحتاج إعداد الثقة واختيارها في VPN.","Start the engine for this website. The built-in probe trusts the lab CA; external apps need CA trust and VPN selection."),16);
                button(tr("تجهيز وتشغيل محرك HTTPS","Prepare and start HTTPS engine"),"workflow:start",()->{
                    if(EmbeddedProxyService.engine!=null){message(matches()?tr("المحرك جاهز؛ اضغط التالي","Engine ready; press Next"):tr("أوقف المحرك الحالي لتغيير الموقع","Stop the current engine to change destination"));return;}
                    startForegroundService(new Intent(this,EmbeddedProxyService.class).putExtra("host",target.host()).putExtra("port",target.port()).putExtra("pause",false));
                });
                button(tr("إعداد الشهادة والتحرير المتقدم","Certificate and advanced editing"),"workflow:https",this::openHttps);
                button(tr("إيقاف المحرك","Stop engine"),"workflow:stop",()->{LabProbe.stop();EmbeddedProxyService.stopNow();stopService(new Intent(this,EmbeddedProxyService.class));});
            }else if(step==2){
                text(tr("يرسل الزر طلب GET واحدًا عبر محرك الفحص إلى المسار المحدد. لمراقبة تطبيق آخر افتح إعداد VPN.","Send one GET through the inspection engine to the selected path. To observe another app, configure VPN."),16);
                CheckBox protocol=new CheckBox(this);protocol.setText("HTTP/2 (ALPN)");protocol.setTextColor(Color.WHITE);protocol.setChecked(useHttp2);protocol.setTag("workflow:http2");protocol.setOnCheckedChangeListener((v,checked)->useHttp2=checked);page.addView(protocol);
                button(tr("ابدأ الاختبار","Start test"),"workflow:test",()->{
                    if(!matches()){message(tr("ارجع إلى الإعداد وشغّل المحرك لهذا الموقع","Return to setup and start the engine for this website"));return;}
                    new Thread(()->{try{if(!AuthSession.isUnlocked())return;LabProbe.start(target,LabCertificateAuthority.open(this),useHttp2);}catch(Exception e){runOnUiThread(()->message(e.getMessage()));}},"nexvary-probe-setup").start();
                });
                button(tr("اختيار تطبيقات VPN وتسجيل PCAP","Select VPN apps and record PCAP"),"workflow:vpn",()->startActivity(new Intent(this,VpnLabActivity.class).putExtra("host",target.host()).putExtra("port",target.port())));
            }else if(step==3){
                text(tr("راجع الطلب والاستجابة والنتيجة الفعلية. إذا فعّلت إيقاف الطلب، افتح المحرر لتمريره أو إسقاطه.","Review the actual request, response and result. If pausing is enabled, open the editor to forward or drop the request."),16);
                button(tr("عرض الطلبات والتحرير","Inspect requests and edit"),"workflow:inspect",this::openHttps);
                button(tr("إعادة الاختبار","Test again"),"workflow:again",()->{step=2;render();});
            }else{
                text(tr("HAR يعرض نصوص الفحص مع التنقيح الافتراضي. PCAP يسجل الحزم الفعلية ويُصدّر من شاشة VPN بعد إيقافه؛ تبقى بيانات TLS مشفرة فيه.","HAR exports inspected text previews with default redaction. Export actual PCAP from VPN after stopping; TLS remains encrypted in packet captures."),16);
                button(tr("تصدير HAR والأرشيف","Export HAR and archive"),"workflow:archive",()->startActivity(new Intent(this,SessionArchiveActivity.class)));
                button(tr("تصدير PCAP من شاشة VPN","Export PCAP from VPN"),"workflow:pcap",()->startActivity(new Intent(this,VpnLabActivity.class).putExtra("host",target.host()).putExtra("port",target.port())));
                button(tr("جلسة جديدة","New session"),"workflow:reset",()->{LabProbe.stop();EmbeddedProxyService.stopNow();com.nexvary.trafficlab.vpn.LabVpnService.stopNow();stopService(new Intent(this,com.nexvary.trafficlab.vpn.LabVpnService.class));stopService(new Intent(this,EmbeddedProxyService.class));com.nexvary.trafficlab.core.CapturedTraffic.endSession();step=0;targetText="";target=null;render();});
            }
            live=text("",13);live.setTag("workflow:state");
            if(step<4)button(tr("التالي","Next"),"workflow:next",()->{if(step==1&&!matches()){message(tr("شغّل محرك الموقع أولًا","Start this website's engine first"));return;}step++;render();});
            button(tr("الخطوة السابقة","Previous step"),"workflow:previous",()->{step--;render();});
        }
        button(tr("العودة إلى لوحة المختبر","Back to lab dashboard"),"workflow:back",this::finish);
    }
    private boolean matches(){return EmbeddedProxyService.engine!=null&&EmbeddedProxyService.engine.isRunning()&&EmbeddedProxyService.engine.matches(target.host(),target.port());}
    private void openHttps(){startActivity(new Intent(this,HttpsLabActivity.class).putExtra("host",target.host()).putExtra("port",target.port()));}
    private void message(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(0,dp(10),0,dp(10));page.addView(t);return t;}
    private void button(String label,String tag,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTag(tag);b.setMinHeight(dp(52));LabIcons.tagged(b,tag);b.setOnClickListener(v->action.run());page.addView(b,new LinearLayout.LayoutParams(-1,-2));}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onBackPressed(){if(step>0){step--;render();}else finish();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("step",step);out.putBoolean("h2",useHttp2);out.putString("target",targetText);super.onSaveInstanceState(out);}
    @Override protected void onResume(){super.onResume();resumed=true;handler.post(poll);}
    @Override protected void onPause(){resumed=false;handler.removeCallbacks(poll);super.onPause();}
}
