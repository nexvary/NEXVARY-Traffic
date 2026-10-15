package com.nexvary.trafficlab;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.nexvary.trafficlab.core.*;
import com.nexvary.trafficlab.packet.*;
import com.nexvary.trafficlab.evidence.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PORT = 18088;
    private final DemoLabServer server = new DemoLabServer(PORT);
    private final InterceptEngine intercept = new InterceptEngine();
    private final HistoryStore history = new HistoryStore();
    private final EvidenceWorkspace evidence = new EvidenceWorkspace();
    private final CaseWorkspace cases = new CaseWorkspace();
    private String activeCaseId;
    private final Set<String> importedCaptures=new LinkedHashSet<>();
    private LabRequest beforeRequest;
    private LabRequest lastRequest;
    private LabResponse lastResponse;
    private LinearLayout content;
    private TextView status;
    private AlertDialog exitDialog, toolsDialog;
    private final Deque<String> navStack = new ArrayDeque<>();
    private final Map<String,Runnable> screens = new LinkedHashMap<>();
    private String currentScreen = "Dashboard";
    private boolean lectureMode = false;
    private final int bg = Color.rgb(2,9,18), panel = Color.rgb(6,20,34), cyan = Color.rgb(10,185,255), green=Color.rgb(57,255,136), purple=Color.rgb(0,200,151), silver=Color.rgb(232,239,244), gold=Color.rgb(218,183,86), danger=Color.rgb(255,88,111);
    private final int panel2 = Color.rgb(4,15,27), line = Color.rgb(24,104,148), muted = Color.rgb(137,160,176);

    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleController.wrap(base)); }
    @Override public void onCreate(Bundle b) { super.onCreate(b); if(!AuthSession.isUnlocked()){ startActivity(new Intent(this,LoginActivity.class)); finish(); return; } if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::goBack); getWindow().setStatusBarColor(bg); boolean serverOk=true; String serverError=""; try { server.start(); } catch(Exception e){ serverOk=false; serverError=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(); } build(); status.setText(serverOk?"● "+getString(R.string.authorized_mode)+" • 127.0.0.1:"+PORT:"● LAB SERVER ERROR • "+serverError); showDashboard(); }
    @Override protected void onDestroy() { server.stop(); super.onDestroy(); }

    private void build() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg); root.setPadding(dp(10),dp(8),dp(10),dp(8));
        root.setLayoutDirection(isRtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        if(Build.VERSION.SDK_INT>=30) root.setOnApplyWindowInsetsListener((v,i)->{ android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars()); v.setPadding(dp(10)+x.left,dp(8)+x.top,dp(10)+x.right,dp(8)+x.bottom); return i; });

        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(2),dp(2),dp(2),dp(6));
        Button back=outlineIconButton("",gold,R.drawable.ic_back); back.setTag("nav:back"); back.setContentDescription(getString(R.string.back)); back.setOnClickListener(v->goBack()); top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(44)));
        LinearLayout brand=new LinearLayout(this); brand.setOrientation(LinearLayout.VERTICAL); brand.setGravity(Gravity.CENTER);
        TextView title=text("NEXVARY",23,silver); title.setTypeface(Typeface.create("sans-serif-light",Typeface.BOLD)); title.setGravity(Gravity.CENTER); title.setLetterSpacing(.18f); title.setPadding(0,0,0,0); brand.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView sub=text("TRAFFIC LAB",10,cyan); sub.setTypeface(Typeface.DEFAULT_BOLD); sub.setGravity(Gravity.CENTER); sub.setLetterSpacing(.24f); sub.setPadding(0,0,0,0); brand.addView(sub,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1); bp.setMargins(dp(6),0,dp(6),0); top.addView(brand,bp);
        Button language=outlineIconButton("",cyan,R.drawable.ic_language); language.setContentDescription(getString(R.string.language)); language.setOnClickListener(v->showLanguageMenu(language)); top.addView(language,new LinearLayout.LayoutParams(dp(48),dp(44)));
        Button lock=outlineIconButton("",gold,R.drawable.ic_lock); lock.setContentDescription(getString(R.string.lock_app)); lock.setOnClickListener(v->lockApp()); LinearLayout.LayoutParams lockLp=new LinearLayout.LayoutParams(dp(48),dp(44)); lockLp.setMargins(dp(5),0,0,0); top.addView(lock,lockLp);
        root.addView(top);

        status = statusPill("● "+getString(R.string.authorized_mode)+"  •  127.0.0.1:"+PORT); root.addView(status,new LinearLayout.LayoutParams(-1,dp(34)));

        // Secondary tool ribbon: keeps advanced modules discoverable without crowding the main screen.
        LinearLayout nav=new LinearLayout(this);
        addNav(nav,"HTTPS Lab",()->startActivity(new Intent(this,HttpsLabActivity.class))); addNav(nav,"Story",this::showStory); addNav(nav,"Repeater",this::showRepeater); addNav(nav,"Diff",this::showDiff); addNav(nav,"Packet Lab",this::showPacketLab); addNav(nav,"WebSocket",this::showWebSocket); addNav(nav,"TLS",this::showTls); addNav(nav,"Evidence",this::showEvidence); addNav(nav,"Presenter",this::showPresenter); addNav(nav,"Explain",this::showExplain); addNav(nav,"What-If",this::showWhatIf); addNav(nav,"Flow Studio",this::showFlow); addNav(nav,"API Graph",this::showApiGraph); addNav(nav,"Timeline",this::showTimeline); addNav(nav,"Matrix",this::showMatrix); addNav(nav,"Replay",this::showReplay); addNav(nav,"Report",this::showReport); addNav(nav,"Lecture",this::showLecture); addNav(nav,"Packet Map",this::showPacketMap); addNav(nav,"Radar",this::showRadar); addNav(nav,"H2/gRPC",this::showModernProtocols); addNav(nav,"JWT/OAuth",this::showIdentityLab); addNav(nav,"Contract",this::showContract); addNav(nav,"Cases",this::showCases); addNav(nav,"AutoDetect",this::showAutoDetect); addNav(nav,"Waterfall",this::showWaterfall); addNav(nav,"Dependencies",this::showDependencies); addNav(nav,"Regression",this::showRegression); addNav(nav,"Baseline",this::showBaseline); addNav(nav,"Data Journey",this::showDataJourney); addNav(nav,"TLS Posture",this::showTlsPosture); addNav(nav,"Composer",this::showComposer); addNav(nav,"Remediation",this::showRemediation); addNav(nav,"Cockpit",this::showCockpit);
        Button tools=outlineIconButton(isRtl()?"أدوات المختبر والصفحات":"Lab tools and pages",cyan,R.drawable.ic_dashboard);
        tools.setTag("app:tools"); tools.setOnClickListener(v->showToolsMenu());
        root.addView(tools,new LinearLayout.LayoutParams(-1,dp(48)));

        ScrollView sv = new ScrollView(this); sv.setFillViewport(true); sv.setClipToPadding(false); content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,dp(2),0,dp(18)); sv.addView(content,new ScrollView.LayoutParams(-1,-2)); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL); bottom.setGravity(Gravity.CENTER); bottom.setPadding(dp(4),dp(5),dp(4),dp(2)); GradientDrawable bgNav=new GradientDrawable(); bgNav.setColor(panel2); bgNav.setCornerRadius(dp(18)); bgNav.setStroke(dp(1),line); bottom.setBackground(bgNav); bottom.setElevation(dp(8));
        addBottomNav(bottom,"Dashboard",R.drawable.ic_dashboard,cyan,this::showDashboard);
        addBottomNav(bottom,"Intercept",R.drawable.ic_intercept,green,this::showIntercept);
        addBottomNav(bottom,"History",R.drawable.ic_history,gold,this::showHistory);
        addBottomNav(bottom,"Cockpit",R.drawable.ic_radar,green,this::showCockpit);
        addBottomNav(bottom,"About",R.drawable.ic_about,cyan,this::showAbout);
        root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(68)));
        setContentView(root);
        screens.put("Dashboard",this::showDashboard); screens.put("Intercept",this::showIntercept); screens.put("History",this::showHistory); screens.put("About",this::showAbout);
    }
    private void addNav(LinearLayout nav,String name,Runnable r){ screens.put(name,r); }
    private void showToolsMenu(){
        String[] routes=screens.keySet().toArray(new String[0]);
        String[] labels=new String[routes.length];for(int i=0;i<routes.length;i++)labels[i]=UiI18n.t(this,routes[i]);
        toolsDialog=new AlertDialog.Builder(this).setTitle(isRtl()?"أدوات المختبر":"Lab tools")
                .setItems(labels,(dialog,index)->navigate(routes[index],screens.get(routes[index])))
                .setNegativeButton(getString(R.string.cancel),null).show();
    }
    private void addBottomNav(LinearLayout nav,String name,int icon,int color,Runnable r){ screens.put(name,r); LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(2),dp(4),dp(2),dp(2)); android.widget.ImageView iv=new android.widget.ImageView(this); iv.setImageResource(icon); iv.setColorFilter(color); box.addView(iv,new LinearLayout.LayoutParams(dp(24),dp(24))); TextView label=text(UiI18n.t(this,name),10,name.equals("About")?cyan:silver); label.setTextIsSelectable(false); label.setGravity(Gravity.CENTER); label.setSingleLine(true); label.setPadding(0,dp(2),0,0); box.addView(label,new LinearLayout.LayoutParams(-1,-2)); box.setTag("nav:"+name); box.setContentDescription(UiI18n.t(this,name)); box.setOnClickListener(v->navigate(name,r)); box.setClickable(true); box.setFocusable(true); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1); lp.setMargins(dp(2),0,dp(2),0); nav.addView(box,lp); }
    private void navigate(String name,Runnable r){
        importCapturedTraffic();
        if ("HTTPS Lab".equals(name)) { r.run(); return; }
        if(!name.equals(currentScreen)){ navStack.push(currentScreen); currentScreen=name; }
        r.run();
    }
    private void goBack(){ if(navStack.isEmpty()){ Toast.makeText(this,getString(R.string.already_home),Toast.LENGTH_SHORT).show(); if(!"Dashboard".equals(currentScreen)){currentScreen="Dashboard";showDashboard();} return;} String prev=navStack.pop(); currentScreen=prev; Runnable r=screens.get(prev); if(r!=null)r.run(); else showDashboard(); }
    @Override public void onBackPressed(){ goBack(); }
    private void lockApp(){ AuthSession.revoke(); Intent i=new Intent(this,LoginActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); finish(); }
    private int navIcon(String n){ if(n.equals("Dashboard")||n.equals("Story"))return R.drawable.ic_dashboard; if(n.equals("Radar")||n.equals("Packet Map")||n.equals("Waterfall"))return R.drawable.ic_radar; if(n.equals("Evidence")||n.equals("TLS")||n.equals("TLS Posture")||n.equals("JWT/OAuth"))return R.drawable.ic_shield; return R.drawable.ic_dashboard; }
    private int navColor(String n){ int h=Math.abs(n.hashCode()%3); return h==0?gold:(h==1?green:cyan); }
    private boolean isRtl(){ return "ar".equals(LocaleController.getLanguage(this)); }
    private void clear(){ content.removeAllViews(); }
    private void showLanguageMenu(View anchor){ PopupMenu p=new PopupMenu(this,anchor); String[] names={"English","العربية","Türkçe","Русский","Español","Deutsch","Italiano"}; String[] codes={"en","ar","tr","ru","es","de","it"}; for(int i=0;i<names.length;i++)p.getMenu().add(0,i,i,names[i]); p.setOnMenuItemClickListener(item->{int i=item.getItemId(); if(i>=0&&i<codes.length){LocaleController.setLanguage(this,codes[i]); recreate(); return true;} return false;}); p.show(); }
    private TextView text(String s,int sp,int c){ TextView t=new TextView(this); t.setText(UiI18n.t(this,s)); t.setTextSize(sp); t.setTextColor(c); t.setPadding(dp(10),dp(8),dp(10),dp(8)); t.setTextIsSelectable(true); t.setGravity(isRtl()?Gravity.RIGHT:Gravity.LEFT); t.setTextDirection(isRtl()?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR); t.setLineSpacing(0,1.08f); return t; }
    private Button button(String s,int c){ Button b=outlineIconButton(UiI18n.t(this,s),c,0); b.setTextColor(c); return b; }
    private Button iconButton(String s,int c,int icon){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.rgb(2,9,18)); b.setTextSize(13); b.setAllCaps(false); b.setSingleLine(true); b.setGravity(Gravity.CENTER); b.setCompoundDrawablePadding(dp(6)); if(icon!=0)b.setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0); GradientDrawable g=new GradientDrawable(); g.setColor(c); g.setCornerRadius(dp(14)); g.setStroke(dp(1),Color.argb(160,255,255,255)); b.setBackground(g); b.setPadding(dp(12),0,dp(12),0); b.setMinHeight(dp(48)); return b; }
    private Button outlineIconButton(String s,int c,int icon){ Button b=new Button(this); b.setText(s); b.setTextColor(c); b.setTextSize(12); b.setAllCaps(false); b.setSingleLine(true); b.setGravity(Gravity.CENTER); b.setCompoundDrawablePadding(dp(6)); if(icon!=0){ b.setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0); b.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(c)); } GradientDrawable g=new GradientDrawable(); g.setColor(panel2); g.setCornerRadius(dp(13)); g.setStroke(dp(1),Color.argb(190,Color.red(c),Color.green(c),Color.blue(c))); b.setBackground(g); b.setPadding(dp(10),0,dp(10),0); b.setMinHeight(dp(44)); b.setElevation(dp(2)); return b; }
    private TextView statusPill(String s){ TextView t=text(s,11,green); t.setGravity(Gravity.CENTER); t.setSingleLine(true); GradientDrawable g=new GradientDrawable(); g.setColor(Color.rgb(3,22,29)); g.setCornerRadius(dp(13)); g.setStroke(dp(1),Color.rgb(22,91,116)); t.setBackground(g); t.setPadding(dp(8),0,dp(8),0); return t; }
    private EditText edit(String s){ EditText e=new EditText(this); e.setText(s); e.setTextColor(silver); e.setHintTextColor(Color.GRAY); GradientDrawable g=new GradientDrawable(); g.setColor(panel); g.setCornerRadius(dp(12)); g.setStroke(dp(1),Color.rgb(29,92,68)); e.setBackground(g); e.setPadding(dp(12),dp(10),dp(12),dp(10)); e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE); e.setGravity(isRtl()?Gravity.RIGHT:Gravity.LEFT); e.setTextDirection(isRtl()?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR); return e; }
    private LinearLayout card(String title){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(14),dp(12),dp(14),dp(12)); GradientDrawable g=new GradientDrawable(); g.setColor(panel); g.setCornerRadius(dp(18)); g.setStroke(dp(1),line); c.setBackground(g); c.setElevation(dp(3)); TextView h=text(title,16,gold); h.setTypeface(Typeface.DEFAULT_BOLD); c.addView(h); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(11)); content.addView(c,p); return c; }

    private LabRequest sampleOrder(){
        LabRequest r=new LabRequest();
        r.method="POST";
        r.path="/api/order";
        r.headers.put("Content-Type","application/json");
        r.headers.put("X-NEXVARY-Demo","lecture");
        r.body="{\"item\":\"TE70\",\"price\":100,\"qty\":1}";
        return r;
    }

    private void showDashboard(){ importCapturedTraffic(); clear();
        LinearLayout hero=card("NEXVARY TRAFFIC LAB"); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView big=text("NEXVARY",30,silver); big.setTypeface(Typeface.create("sans-serif-light",Typeface.BOLD)); big.setLetterSpacing(.18f); big.setGravity(Gravity.CENTER); hero.addView(big);
        TextView strap=text("EXECUTIVE TECHNOLOGY PROTECTION",10,gold); strap.setGravity(Gravity.CENTER); strap.setLetterSpacing(.10f); hero.addView(strap);
        TextView areas=text("TSCM  •  COUNTER-SURVEILLANCE  •  CYBERSECURITY",10,cyan); areas.setGravity(Gravity.CENTER); hero.addView(areas);
        hero.addView(text("Authorized Android traffic-analysis and lecture workbench",13,silver));

        LinearLayout live=card("CAPTURE STATUS");
        LinearLayout stats=new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(metric("MODE",lectureMode?"LECTURE":"LAB",lectureMode?gold:green),new LinearLayout.LayoutParams(0,-2,1));
        stats.addView(metric("SESSIONS",String.valueOf(history.all().size()),cyan),new LinearLayout.LayoutParams(0,-2,1));
        int findings=0; for(HistoryEntry e:history.all()) findings+=e.findings.size();
        stats.addView(metric("FINDINGS",String.valueOf(findings),findings>0?danger:green),new LinearLayout.LayoutParams(0,-2,1));
        live.addView(stats);

        LinearLayout q=card("QUICK TOOLS");
        LinearLayout r1=new LinearLayout(this); r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.addView(toolTile("Intercept",R.drawable.ic_intercept,green,this::showIntercept),new LinearLayout.LayoutParams(0,dp(92),1));
        r1.addView(toolTile("History",R.drawable.ic_history,cyan,this::showHistory),new LinearLayout.LayoutParams(0,dp(92),1));
        r1.addView(toolTile("Repeater",R.drawable.ic_repeater,gold,this::showRepeater),new LinearLayout.LayoutParams(0,dp(92),1)); q.addView(r1);
        LinearLayout r2=new LinearLayout(this); r2.setOrientation(LinearLayout.HORIZONTAL);
        r2.addView(toolTile("Flow Studio",R.drawable.ic_flow,cyan,this::showFlow),new LinearLayout.LayoutParams(0,dp(92),1));
        r2.addView(toolTile("Radar",R.drawable.ic_radar,green,this::showRadar),new LinearLayout.LayoutParams(0,dp(92),1));
        r2.addView(toolTile("Evidence",R.drawable.ic_shield,gold,this::showEvidence),new LinearLayout.LayoutParams(0,dp(92),1)); q.addView(r2);

        LinearLayout lecture=card("LECTURE MODE"); Button oneTap=button("▶ One-Tap Lecture Mode",gold); oneTap.setTextColor(gold); oneTap.setOnClickListener(v->{ lectureMode=true; history.clear(); LabRequest x=sampleOrder(); beforeRequest=x.copy(); intercept.intercept(x); status.setText("● LECTURE MODE  •  VISUAL EDITOR  •  LOCALHOST ONLY"); navigate("Intercept",this::showIntercept); }); lecture.addView(oneTap,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView note=text("Start a clean scenario, pause the request, modify fields visually, then compare and explain the result.",12,muted); lecture.addView(note);

        LinearLayout recent=card("RECENT ACTIVITY"); if(history.all().isEmpty()) recent.addView(text("No sessions yet. Start the one-tap lecture scenario.",13,muted)); else { int shown=0; for(HistoryEntry e:history.all()){ recent.addView(text("◆  "+e.summary(),12,silver)); if(++shown>=4)break; } }
    }

    private LinearLayout metric(String label,String value,int color){ LinearLayout b=new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); b.setGravity(Gravity.CENTER); b.setPadding(dp(4),dp(8),dp(4),dp(8)); TextView l=text(label,9,muted); l.setGravity(Gravity.CENTER); l.setPadding(0,0,0,2); b.addView(l); TextView v=text(value,17,color); v.setTypeface(Typeface.DEFAULT_BOLD); v.setGravity(Gravity.CENTER); v.setPadding(0,0,0,0); b.addView(v); return b; }
    private LinearLayout toolTile(String label,int icon,int color,Runnable action){ LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(6),dp(8),dp(6),dp(8)); GradientDrawable g=new GradientDrawable(); g.setColor(panel2); g.setCornerRadius(dp(14)); g.setStroke(dp(1),Color.argb(185,Color.red(color),Color.green(color),Color.blue(color))); box.setBackground(g); box.setElevation(dp(2)); android.widget.ImageView iv=new android.widget.ImageView(this); iv.setImageResource(icon); iv.setColorFilter(color); box.addView(iv,new LinearLayout.LayoutParams(dp(30),dp(30))); TextView t=text(UiI18n.t(this,label),11,silver); t.setTextIsSelectable(false); t.setGravity(Gravity.CENTER); t.setSingleLine(true); t.setPadding(0,dp(5),0,0); box.addView(t,new LinearLayout.LayoutParams(-1,-2)); LinearLayout.LayoutParams m=new LinearLayout.LayoutParams(-1,-1); m.setMargins(dp(4),dp(4),dp(4),dp(4)); box.setLayoutParams(m); box.setTag("nav:"+label); box.setOnClickListener(v->navigate(label,action)); box.setClickable(true); box.setFocusable(true); screens.put(label,action); return box; }

    private void showIntercept(){
        clear();
        if(intercept.pending()==null){ LabRequest s=sampleOrder(); beforeRequest=s.copy(); intercept.intercept(s); }
        LabRequest r=intercept.pending();
        LinearLayout c=card("INTERCEPTED REQUEST — paused before forwarding");
        if(lectureMode) c.addView(text("● LECTURE MODE — use visual controls, then Send & Compare. RAW data remains available but is not required.",13,green));
        EditText method=edit(r.method), path=edit(r.path), headers=edit(headers(r)), body=edit(r.body);
        body.setMinLines(6);
        c.addView(text("Method",12,cyan)); c.addView(method);
        c.addView(text("Path",12,cyan)); c.addView(path);
        c.addView(text("Headers",12,cyan)); c.addView(headers);

        LinearLayout modeRow=new LinearLayout(this); modeRow.setOrientation(LinearLayout.HORIZONTAL);
        Button visualMode=button("Visual Editor",gold), rawMode=button("RAW",cyan);
        LinearLayout.LayoutParams halfTop=new LinearLayout.LayoutParams(0,dp(48),1); halfTop.setMargins(dp(3),0,dp(3),0);
        modeRow.addView(visualMode,halfTop); modeRow.addView(rawMode,halfTop); c.addView(modeRow);

        LinearLayout visualBox=new LinearLayout(this); visualBox.setOrientation(LinearLayout.VERTICAL);
        visualBox.setPadding(0,dp(6),0,dp(6)); c.addView(visualBox);
        LinearLayout rawBox=new LinearLayout(this); rawBox.setOrientation(LinearLayout.VERTICAL);
        rawBox.addView(text("Body",12,cyan)); rawBox.addView(body); rawBox.setVisibility(View.GONE); c.addView(rawBox);

        List<StructuredBodyEditor.Field> fields=new ArrayList<>(StructuredBodyEditor.parseFlatJson(r.body));
        final boolean[] visualActive={true};
        Runnable rebuildVisual=()->renderVisualFields(visualBox,fields,body);
        rebuildVisual.run();

        LinearLayout compareCard=cardInside("⚡ LIVE ORIGINAL ↔ MODIFIED COMPARE");
        TextView comparePreview=text("",12,silver); compareCard.addView(comparePreview); c.addView(compareCard);
        Runnable refreshCompare=()->{ LabRequest x=candidate(method,path,headers,body); comparePreview.setText(RequestCompare.pretty(RequestCompare.compare(r,x))+"\n\nUTF-8 Content-Length: "+RequestNormalizer.utf8Length(x.body)); };
        TextWatcher watcher=new TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){refreshCompare.run();} public void afterTextChanged(Editable e){} };
        method.addTextChangedListener(watcher); path.addTextChangedListener(watcher); headers.addTextChangedListener(watcher); body.addTextChangedListener(watcher); refreshCompare.run();

        visualMode.setOnClickListener(v->{
            List<StructuredBodyEditor.Field> parsed=StructuredBodyEditor.parseFlatJson(body.getText().toString());
            if(!parsed.isEmpty()){ fields.clear(); fields.addAll(parsed); rebuildVisual.run(); }
            visualActive[0]=true; visualBox.setVisibility(View.VISIBLE); rawBox.setVisibility(View.GONE);
        });
        rawMode.setOnClickListener(v->{ if(!fields.isEmpty()&&visualActive[0]) body.setText(StructuredBodyEditor.toJson(fields)); visualActive[0]=false; visualBox.setVisibility(View.GONE); rawBox.setVisibility(View.VISIBLE); });

        LinearLayout smart=cardInside("✨ SMART EDIT — no JSON typing needed");
        Button price=button("Price 100 → 1",green), qty=button("Quantity +1",gold), restore=button("Restore original",cyan);
        smart.addView(price,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout qr=new LinearLayout(this); qr.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams qh=new LinearLayout.LayoutParams(0,dp(48),1); qh.setMargins(dp(3),dp(4),dp(3),0); qr.addView(qty,qh); qr.addView(restore,qh); smart.addView(qr);
        c.addView(smart);
        price.setOnClickListener(v->{ setVisualField(fields,"price","1"); body.setText(StructuredBodyEditor.toJson(fields)); rebuildVisual.run(); });
        qty.setOnClickListener(v->{ for(int i=0;i<fields.size();i++){var f=fields.get(i); if(f.key().equals("qty")&&f.kind()==StructuredBodyEditor.Kind.NUMBER){fields.set(i,new StructuredBodyEditor.Field(f.key(),StructuredBodyEditor.incrementNumber(f.value(),1),f.kind()));}} body.setText(StructuredBodyEditor.toJson(fields)); rebuildVisual.run(); });
        restore.setOnClickListener(v->{ fields.clear(); fields.addAll(StructuredBodyEditor.parseFlatJson(r.body)); body.setText(r.body); rebuildVisual.run(); });

        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button forward=button("Forward",green), drop=button("Drop",danger);
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,dp(52),1); half.setMargins(dp(3),dp(6),dp(3),0);
        row.addView(forward,half); row.addView(drop,half); c.addView(row,new LinearLayout.LayoutParams(-1,-2));
        Button compare=button("Send & Compare",gold); c.addView(compare,new LinearLayout.LayoutParams(-1,dp(52)));

        forward.setOnClickListener(v->{ if(visualActive[0]&&!fields.isEmpty()) body.setText(StructuredBodyEditor.toJson(fields)); intercept.replace(method.getText().toString(),path.getText().toString(),headers.getText().toString(),body.getText().toString()); LabRequest send=intercept.forward(); lastRequest=send.copy(); execute(send); });
        compare.setOnClickListener(v->{ beforeRequest=r.copy(); if(visualActive[0]&&!fields.isEmpty()) body.setText(StructuredBodyEditor.toJson(fields)); intercept.replace(method.getText().toString(),path.getText().toString(),headers.getText().toString(),body.getText().toString()); LabRequest send=intercept.forward(); lastRequest=send.copy(); execute(send); });
        drop.setOnClickListener(v->{intercept.drop(); status.setText("● REQUEST DROPPED — nothing left the lab client"); navigate("Dashboard",this::showDashboard);});
    }

    private LinearLayout cardInside(String title){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(10),dp(8),dp(10),dp(8));
        GradientDrawable g=new GradientDrawable(); g.setColor(panel2); g.setCornerRadius(dp(14)); g.setStroke(dp(1),line); c.setBackground(g);
        TextView h=text(title,14,gold); h.setTypeface(Typeface.DEFAULT_BOLD); c.addView(h); return c;
    }

    private void setVisualField(List<StructuredBodyEditor.Field> fields,String key,String value){
        for(int i=0;i<fields.size();i++){ StructuredBodyEditor.Field f=fields.get(i); if(f.key().equals(key)){ fields.set(i,new StructuredBodyEditor.Field(f.key(),value,f.kind())); return; } }
    }

    private void renderVisualFields(LinearLayout host,List<StructuredBodyEditor.Field> fields,EditText rawBody){
        host.removeAllViews();
        if(fields.isEmpty()){
            host.addView(text("Smart Edit could not map this body. Use RAW mode for this request.",13,danger));
            return;
        }
        host.addView(text("Schema-Aware Editor chooses the best control for each value automatically.",13,green));
        List<SchemaAwareEditor.FieldHint> hints=SchemaAwareEditor.infer(fields);
        for(int i=0;i<fields.size();i++){
            final int index=i; StructuredBodyEditor.Field f=fields.get(i); SchemaAwareEditor.FieldHint hint=hints.get(i);
            LinearLayout box=cardInside(f.key()+"   •   "+hint.control());
            box.addView(text(hint.explanation(),11,cyan));
            if(hint.control()==SchemaAwareEditor.Control.STEPPER){
                LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
                Button minus=button("−",gold), plus=button("+",green); TextView value=text(f.value(),18,silver); value.setGravity(Gravity.CENTER); value.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams small=new LinearLayout.LayoutParams(dp(62),dp(48)); LinearLayout.LayoutParams mid=new LinearLayout.LayoutParams(0,dp(48),1);
                row.addView(minus,small); row.addView(value,mid); row.addView(plus,small); box.addView(row);
                minus.setOnClickListener(v->{ StructuredBodyEditor.Field cur=fields.get(index); fields.set(index,new StructuredBodyEditor.Field(cur.key(),StructuredBodyEditor.incrementNumber(cur.value(),-1),cur.kind())); rawBody.setText(StructuredBodyEditor.toJson(fields)); renderVisualFields(host,fields,rawBody); });
                plus.setOnClickListener(v->{ StructuredBodyEditor.Field cur=fields.get(index); fields.set(index,new StructuredBodyEditor.Field(cur.key(),StructuredBodyEditor.incrementNumber(cur.value(),1),cur.kind())); rawBody.setText(StructuredBodyEditor.toJson(fields)); renderVisualFields(host,fields,rawBody); });
                if(f.key().equalsIgnoreCase("price")){ Button one=button("Set 1",cyan); one.setOnClickListener(v->{ StructuredBodyEditor.Field cur=fields.get(index); fields.set(index,new StructuredBodyEditor.Field(cur.key(),"1",cur.kind())); rawBody.setText(StructuredBodyEditor.toJson(fields)); renderVisualFields(host,fields,rawBody); }); box.addView(one,new LinearLayout.LayoutParams(-1,dp(44))); }
            } else if(hint.control()==SchemaAwareEditor.Control.TOGGLE){
                Button toggle=button(f.value().equals("true")?"✓ TRUE":"✕ FALSE",f.value().equals("true")?green:danger); box.addView(toggle,new LinearLayout.LayoutParams(-1,dp(48)));
                toggle.setOnClickListener(v->{ StructuredBodyEditor.Field cur=fields.get(index); fields.set(index,new StructuredBodyEditor.Field(cur.key(),Boolean.toString(!Boolean.parseBoolean(cur.value())),cur.kind())); rawBody.setText(StructuredBodyEditor.toJson(fields)); renderVisualFields(host,fields,rawBody); });
            } else if(hint.control()==SchemaAwareEditor.Control.CHOICE){
                box.addView(text(getString(R.string.value_label)+": "+f.value(),14,silver));
                LinearLayout choices=new LinearLayout(this); choices.setOrientation(LinearLayout.HORIZONTAL);
                for(String preset:hint.choices()){ Button b=button(preset.isEmpty()?"Empty":preset,(preset.equals(f.value())?green:cyan)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(44)); lp.setMargins(dp(2),0,dp(2),0); choices.addView(b,lp); b.setOnClickListener(v->{ StructuredBodyEditor.Field cur=fields.get(index); fields.set(index,new StructuredBodyEditor.Field(cur.key(),preset,cur.kind())); rawBody.setText(StructuredBodyEditor.toJson(fields)); renderVisualFields(host,fields,rawBody); }); }
                HorizontalScrollView sc=new HorizontalScrollView(this); sc.setHorizontalScrollBarEnabled(false); sc.addView(choices,new HorizontalScrollView.LayoutParams(-2,-2)); box.addView(sc);
            } else {
                EditText e=edit(f.value()); box.addView(e); Button apply=button("Apply",cyan); box.addView(apply,new LinearLayout.LayoutParams(-1,dp(44))); apply.setOnClickListener(v->{ StructuredBodyEditor.Field cur=fields.get(index); fields.set(index,new StructuredBodyEditor.Field(cur.key(),e.getText().toString(),cur.kind())); rawBody.setText(StructuredBodyEditor.toJson(fields)); renderVisualFields(host,fields,rawBody); });
            }
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2); bp.setMargins(0,dp(4),0,dp(6)); host.addView(box,bp);
        }
        rawBody.setText(StructuredBodyEditor.toJson(fields));
    }

    private void execute(LabRequest req){ LabRequest normalized=RequestNormalizer.normalize(req); status.setText("● FORWARDING TO LOCAL LAB SERVER … • Content-Length "+RequestNormalizer.utf8Length(normalized.body)); new Thread(()->{ try { LabResponse res=RawHttpClient.sendLocal(PORT,normalized); List<Finding> f=RiskEngine.scan(normalized,res); history.add(normalized,res,f); lastRequest=normalized.copy(); lastResponse=res; evidence.add("HTTP "+normalized.method+" "+normalized.path, normalized.toRaw("127.0.0.1:"+PORT)+"\n\n"+res.raw); runOnUiThread(()->showResult(normalized,res,f)); } catch(Exception e){ runOnUiThread(()->status.setText("Error: "+e.getMessage())); }}).start(); }
    private void showResult(LabRequest req,LabResponse res,List<Finding> f){ clear(); status.setText("● RESPONSE "+res.statusCode+" • "+res.elapsedMs+" ms • LOCAL LAB"); LinearLayout a=card("Forwarded request"); a.addView(text(req.toRaw("127.0.0.1:"+PORT),12,silver)); LinearLayout b=card("Server response"); b.addView(text(res.raw,12,green)); LinearLayout c=card("NEXVARY Risk Engine — score "+RiskEngine.score(f)+"/100"); for(Finding x:f)c.addView(text(x.severity()+" • "+x.title()+"\n"+x.detail(),13,x.severity().equals("HIGH")?danger:gold)); Button again=button("Send to Repeater",purple); again.setOnClickListener(v->{intercept.intercept(req);navigate("Repeater",this::showRepeater);}); c.addView(again); }

    private void showHistory(){ importCapturedTraffic(); clear(); LinearLayout c=card("HTTP HISTORY — demo and captured HTTPS sessions"); EditText q=edit(""); q.setHint(UiI18n.t(this,"Search method, path, header, body, response or tag")); c.addView(q); Button search=button("Search",cyan); c.addView(search); LinearLayout results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); c.addView(results); Runnable render=()->{ results.removeAllViews(); List<HistoryEntry> list=history.search(q.getText().toString()); if(list.isEmpty()) results.addView(text("No sessions yet.",13,silver)); for(HistoryEntry e:list){ TextView t=text(e.summary()+"\nTags: "+e.tags,13,silver); results.addView(t); Button tag=button("Tag #"+e.id+" as LECTURE",gold); tag.setOnClickListener(v->{history.tag(e.id,"LECTURE");showHistory();}); results.addView(tag);} }; search.setOnClickListener(v->render.run()); render.run(); }

    private void showRepeater(){ clear(); LabRequest r=intercept.pending(); if(r==null)r=lastRequest==null?sampleOrder():lastRequest; LinearLayout c=card("REPEATER — localhost-authorized target"); EditText raw=edit(r.toRaw("127.0.0.1:"+PORT)); raw.setMinLines(12); c.addView(raw); Button send=button("Send again",green); c.addView(send); LabRequest finalR=r; send.setOnClickListener(v->{ LabRequest parsed=parseRaw(raw.getText().toString(), finalR); beforeRequest=finalR.copy(); execute(parsed); }); }
    private LabRequest candidate(EditText method,EditText path,EditText headers,EditText body){ LabRequest x=new LabRequest(); x.method=method.getText().toString().trim().toUpperCase(Locale.ROOT); x.path=path.getText().toString().trim(); for(String line:headers.getText().toString().split("\\r?\\n")){ int p=line.indexOf(':'); if(p>0)x.headers.put(line.substring(0,p).trim(),line.substring(p+1).trim()); } x.body=body.getText().toString(); return RequestNormalizer.normalize(x); }

    private LabRequest parseRaw(String raw,LabRequest fallback){ try{ String[] parts=raw.split("\\r?\\n\\r?\\n",2); String[] lines=parts[0].split("\\r?\\n"); String[] first=lines[0].split(" "); LabRequest r=new LabRequest(); r.method=first[0];r.path=first[1];for(int i=1;i<lines.length;i++){int p=lines[i].indexOf(':');if(p>0){String k=lines[i].substring(0,p).trim(); if(!k.equalsIgnoreCase("Host")&&!k.equalsIgnoreCase("Content-Length")&&!k.equalsIgnoreCase("Connection"))r.headers.put(k,lines[i].substring(p+1).trim());}}r.body=parts.length>1?parts[1]:"";return r;}catch(Exception e){return fallback;} }

    private void showDiff(){ clear(); LinearLayout c=card("BEFORE / AFTER DIFF"); LabRequest a=beforeRequest==null?sampleOrder():beforeRequest; LabRequest b=lastRequest==null?a:lastRequest; String left=a.toRaw("127.0.0.1:"+PORT), right=b.toRaw("127.0.0.1:"+PORT); c.addView(text(DiffEngine.pretty(left,right),13,silver)); }

    private void showStory(){ clear(); LinearLayout c=card("SESSION STORY MODE — technical traffic as a lecture narrative"); var steps=SessionStoryEngine.build(history.all()); c.addView(text(SessionStoryEngine.pretty(steps),14,silver)); if(!steps.isEmpty()){ LinearLayout summary=cardInside("Presenter summary"); summary.addView(text("Follow the numbered flow instead of explaining isolated packets. High-risk steps are highlighted by the Risk Engine and can be opened from History for evidence.",13,gold)); c.addView(summary); } }

    private void showPacketLab(){ clear(); LinearLayout c=card("PACKET LAB — decoder + PCAP"); byte[] packet=sampleIpv4UdpDns(); PacketInfo i=PacketParser.parse(packet); c.addView(text("IPv"+i.ipVersion()+" "+i.protocol()+"  "+i.src()+":"+i.srcPort()+" → "+i.dst()+":"+i.dstPort()+"\nLength: "+i.length(),14,cyan)); try{ int ihl=(packet[0]&15)*4; DnsParser.DnsQuery q=DnsParser.parseQuery(packet,ihl+8,packet.length-(ihl+8)); c.addView(text("DNS query: "+q.name()+"  type="+q.type()+"  id="+q.id(),14,green)); }catch(Exception e){c.addView(text("DNS: "+e.getMessage(),13,danger));}
        Button save=button("Write sample capture.pcap",gold); c.addView(save); save.setOnClickListener(v->{try{File f=new File(getFilesDir(),"nexvary-sample.pcap"); try(PcapWriter p=new PcapWriter(new FileOutputStream(f))){p.write(packet,System.currentTimeMillis());} evidence.add("PCAP sample",Base64.getEncoder().encodeToString(packet)); status.setText("● PCAP SAVED: "+f.getAbsolutePath());}catch(Exception e){status.setText("PCAP error: "+e.getMessage());}}); }

    private byte[] sampleIpv4UdpDns(){ byte[] dns=new byte[]{0x12,0x34,0x01,0x00,0x00,0x01,0,0,0,0,0,0,7,'n','e','x','v','a','r','y',3,'c','o','m',0,0,1,0,1}; int total=20+8+dns.length; byte[] p=new byte[total]; p[0]=0x45; p[2]=(byte)(total>>8);p[3]=(byte)total;p[8]=64;p[9]=17;p[12]=10;p[13]=77;p[14]=0;p[15]=2;p[16]=10;p[17]=77;p[18]=0;p[19]=1; int o=20;p[o]=0x30;p[o+1]=0x39;p[o+2]=0;p[o+3]=53;int u=8+dns.length;p[o+4]=(byte)(u>>8);p[o+5]=(byte)u;System.arraycopy(dns,0,p,28,dns.length);return p; }

    private void showWebSocket(){ clear(); LinearLayout c=card("WEBSOCKET FRAME LAB"); EditText msg=edit("{\"event\":\"demo\",\"value\":100}"); c.addView(msg); Button encode=button("Encode → inspect frame",purple); c.addView(encode); TextView out=text("",13,silver); c.addView(out); encode.setOnClickListener(v->{try{byte[] raw=WebSocketFrameCodec.encodeClientText(msg.getText().toString()); WebSocketFrameCodec.Frame f=WebSocketFrameCodec.decode(raw); out.setText("FIN="+f.fin()+" opcode="+f.opcode()+"\nPayload="+new String(f.payload(),StandardCharsets.UTF_8)+"\nFrame bytes="+hex(raw));}catch(Exception e){out.setText(e.getMessage());}}); }

    private void showTls(){ clear(); LinearLayout c=card("TLS / X.509 METADATA INSPECTOR"); c.addView(text("This module parses DER X.509 certificates locally and reports subject, issuer, validity, signature algorithm and SHA-256 fingerprint. It is intentionally metadata-focused; it does not bypass certificate pinning.",14,silver)); c.addView(text("Use the inspector API TlsMetadataInspector.inspectCertificate(byte[]) when a certificate is available from an authorized lab TLS session or imported evidence.",13,cyan)); }

    private void showEvidence(){ clear(); LinearLayout c=card("EVIDENCE WORKSPACE — SHA-256 HASH CHAIN"); List<EvidenceWorkspace.Evidence> all=evidence.all(); c.addView(text("Chain verification: "+(evidence.verifyChain()?"VALID":"BROKEN")+" • items="+all.size(),14,green)); for(EvidenceWorkspace.Evidence e:all)c.addView(text(new Date(e.timestamp())+"\n"+e.label()+"\nSHA-256 "+e.sha256()+"\nPrev "+e.previousHash(),12,silver)); }

    private void showPresenter(){ clear(); LinearLayout c=card("PRESENTER MODE — automatic secret redaction"); String demo="Authorization: Bearer demo-secret-token\nCookie: session=abc123\nContent-Type: application/json\n\n{\"user\":\"student\",\"password\":\"secret\",\"token\":\"xyz\"}"; c.addView(text("RAW",12,danger)); c.addView(text(demo,12,silver)); c.addView(text("SAFE FOR PROJECTOR",12,green)); c.addView(text(PresenterRedactor.redact(demo),12,green)); }

    private void showExplain(){ clear(); LabRequest r=lastRequest==null?sampleOrder():lastRequest; LinearLayout a=card("EXPLAIN REQUEST — العربية"); a.addView(text(ExplainEngine.explain(r,true),14,silver)); LinearLayout e=card("EXPLAIN REQUEST — English"); e.addView(text(ExplainEngine.explain(r,false),14,silver)); }
    private void showWhatIf(){ clear(); LabRequest r=lastRequest==null?sampleOrder():lastRequest; LinearLayout c=card("WHAT-IF SECURE DESIGN SIMULATOR"); c.addView(text("Observed client request:\n"+r.body+"\n\nSafer server-side design:\n"+WhatIfEngine.analyze(r),14,gold)); }

    private void showFlow(){ clear(); LinearLayout c=card("NEXVARY FLOW STUDIO"); c.addView(text("[ Android Demo Client ]\n          ↓\n[ INTERCEPT • PAUSE ]\n          ↓ modify headers/body\n[ DIFF • BEFORE / AFTER ]\n          ↓\n[ LOCAL LAB API :18088 ]\n          ↓\n[ RESPONSE + TIMING ]\n          ↓\n[ HISTORY + RISK ENGINE ]\n          ↓\n[ EVIDENCE HASH CHAIN ]\n          ↓\n[ PRESENTER / EXPLAIN / WHAT-IF ]",16,cyan)); c.addView(text("Lecture sequence: intercept → modify a client-controlled value → forward to the intentionally vulnerable localhost API → show Diff → explain why server-side validation must be authoritative → preserve the session as evidence.",14,gold)); }

    private void showApiGraph(){ clear(); LinearLayout c=card("API GRAPH — learned from local lab history"); var g=ApiGraphBuilder.build(history.all()); String out=ApiGraphBuilder.ascii(g); c.addView(text(out.isBlank()?"No sessions yet. Run the quick lecture demo first.":out,14,cyan)); c.addView(text("Nodes: "+g.nodes().size()+" • Edges: "+g.edges().size(),12,silver)); }

    private void showTimeline(){ clear(); LinearLayout c=card("LIVE SECURITY TIMELINE"); var es=LiveSecurityTimeline.build(history.all()); String out=LiveSecurityTimeline.compact(es); c.addView(text(out.isBlank()?"No timeline events yet.":out,13,silver)); }

    private void showMatrix(){ clear(); LinearLayout c=card("REQUEST MUTATION MATRIX — localhost lecture variants"); LabRequest base=lastRequest==null?sampleOrder():lastRequest; var vs=RequestMutationMatrix.safeLectureVariants(base); c.addView(text(RequestMutationMatrix.table(vs),13,silver)); for(var v:vs){ Button b=button("Load: "+v.label(),purple); b.setOnClickListener(x->{ beforeRequest=base.copy(); intercept.intercept(v.request().copy()); navigate("Intercept",this::showIntercept); }); c.addView(b); } }

    private void showReplay(){ clear(); LinearLayout c=card("SESSION REPLAY — chronological lecture script"); var steps=SessionReplay.from(history.all()); String out=SessionReplay.script(steps); c.addView(text(out.isBlank()?"No captured session to replay yet.":out,13,green)); }

    private void showReport(){ clear(); LinearLayout c=card("PRESENTER DASHBOARD + INTERACTIVE REPORT"); var snap=PresenterDashboardModel.summarize(history.all()); c.addView(text("Sessions: "+snap.sessions()+"\nFindings: "+snap.findings()+"\nMax risk: "+snap.maxRisk()+"/100\nAverage response: "+snap.avgMs()+" ms\nTop route: "+snap.topRoute()+"\n"+snap.headline(),14,gold)); Button save=button("Export verified lecture bundle",green); c.addView(save); save.setOnClickListener(v->{ try { File dir=new File(getFilesDir(),"lecture-export"); dir.mkdirs(); var items=LabExportBundle.build("NEXVARY Lecture",history.all()); for(var i:items){ try(FileOutputStream o=new FileOutputStream(new File(dir,i.name()))){o.write(i.content().getBytes(StandardCharsets.UTF_8));} } status.setText("● VERIFIED EXPORT: "+(LabExportBundle.verify(items)?"OK":"FAIL")+" • "+dir.getAbsolutePath()); } catch(Exception e){ status.setText("Export error: "+e.getMessage()); } }); }

    private void showLecture(){ clear(); LinearLayout c=card("LECTURE DIRECTOR — guided demo cues"); for(var x:LectureCueEngine.standard()) c.addView(text(x.order()+". "+x.title()+"\n"+x.action()+"\n"+x.talkingPoint(),13,x.order()%2==0?cyan:gold)); var anomalies=AnomalyExplainer.explain(history.all()); LinearLayout a=card("ANOMALY EXPLAINER"); for(String x:anomalies)a.addView(text("• "+x,13,silver)); if(lastRequest!=null){ String fp=DataFlowFingerprint.fingerprint(lastRequest,lastResponse); a.addView(text("Data-flow fingerprint\n"+fp,12,green)); } LinearLayout sc=card("SMART FINDING CORRELATION"); sc.addView(text(SmartFindingCorrelator.pretty(SmartFindingCorrelator.correlate(history.all())),12,silver)); }

    private void showPacketMap(){ clear(); LinearLayout c=card("VISUAL PACKET MAP — session topology"); c.addView(text(VisualPacketMap.ascii(VisualPacketMap.fromHistory(history.all())),14,cyan)); }

    private void showRadar(){ clear(); LinearLayout c=card("LIVE ENDPOINT RADAR — risk-ranked routes"); c.addView(text(LiveEndpointRadar.radarText(LiveEndpointRadar.summarize(history.all())),13,green)); }

    private void showModernProtocols(){ clear(); LinearLayout h=card("HTTP/2 FRAME LAB"); byte[] frame=Http2FrameLab.encode(0,1,1,"NEXVARY demo DATA".getBytes(StandardCharsets.UTF_8)); h.addView(text(Http2FrameLab.describe(Http2FrameLab.decode(frame)),13,cyan)); LinearLayout g=card("gRPC MESSAGE INSPECTOR"); byte[] grpc=GrpcInspector.frame("demo-protobuf-bytes".getBytes(StandardCharsets.UTF_8),false); var x=GrpcInspector.inspect(grpc); g.addView(text("compressed="+x.compressed()+" • messageLength="+x.length()+" • payload="+new String(x.payload(),StandardCharsets.UTF_8),13,silver)); g.addView(text("This laboratory inspects framing/metadata only; it does not attack external services.",12,gold)); }

    private void showIdentityLab(){ clear(); LinearLayout j=card("JWT CLAIMS ANALYZER — decode-only"); String demo="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJsZWN0dXJlLXVzZXIiLCJleHAiOjQxMDI0NDQ4MDB9.ZGVtby1zaWduYXR1cmU"; var a=JwtAnalyzer.analyze(demo,System.currentTimeMillis()/1000); j.addView(text("Header: "+a.headerJson()+"\nPayload: "+a.payloadJson()+"\nAlgorithm: "+a.algorithm()+"\nExpired: "+a.expired()+"\nNotes: "+String.join(" | ",a.notes()),12,silver)); LinearLayout o=card("OAUTH 2.0 AUTHORIZATION CODE + PKCE VISUALIZER"); o.addView(text(OAuthFlowVisualizer.ascii(OAuthFlowVisualizer.authorizationCodePkce()),13,green)); }

    private void showContract(){ clear(); LinearLayout c=card("API CONTRACT DIFF"); LabRequest a=sampleOrder(),b=sampleOrder(); b.path="/api/order/42"; b.headers.put("Content-Type","application/problem+json"); var diff=ApiContractDiff.compare(ApiContractDiff.infer(a,null),ApiContractDiff.infer(b,null)); c.addView(text(ApiContractDiff.pretty(diff),13,silver)); LinearLayout f=card("SMART FINDING CORRELATION"); f.addView(text(SmartFindingCorrelator.pretty(SmartFindingCorrelator.correlate(history.all())),13,gold)); }

    private void showAutoDetect(){ clear(); LinearLayout c=card("PROTOCOL AUTO-DETECT"); byte[] sample=(lastRequest==null?"GET /api/demo HTTP/1.1\r\nHost: lab.nexvary.local\r\n\r\n":lastRequest.method+" "+lastRequest.path+" HTTP/1.1\r\n"+headers(lastRequest)+"\r\n"+lastRequest.body).getBytes(StandardCharsets.ISO_8859_1); var d=ProtocolAutoDetect.detect(sample,80); c.addView(text("Detected: "+d.protocol()+"\nConfidence: "+d.confidence()+"%\nReason: "+d.reason(),14,cyan)); c.addView(text("Signature-first detection is used only to classify authorized lab traffic; unknown payloads stay UNKNOWN instead of being guessed.",12,silver)); }

    private void showWaterfall(){ clear(); LinearLayout c=card("LIVE REQUEST WATERFALL"); String out=RequestWaterfall.ascii(RequestWaterfall.build(history.all())); c.addView(text(out.isBlank()?"No sessions yet. Run the localhost lecture demo first.":out,13,green)); }

    private void showDependencies(){ clear(); LinearLayout c=card("DEPENDENCY / THIRD-PARTY MAP"); String out=DependencyMap.pretty(DependencyMap.build(history.all(),"127.0.0.1:"+PORT)); c.addView(text(out.isBlank()?"No dependencies observed yet.":out,13,cyan)); c.addView(text("The map groups destinations and routes so a presenter can explain first-party vs third-party data flows without exposing secrets.",12,silver)); }

    private void showRegression(){ clear(); LinearLayout c=card("SECURITY REGRESSION GATE"); List<Finding> candidate=lastRequest==null?List.of():RiskEngine.scan(lastRequest,lastResponse); var g=SecurityRegressionGate.compare(List.of(),candidate); c.addView(text((g.pass()?"PASS":"BLOCK")+"\nBaseline risk: "+g.baselineRisk()+"\nCandidate risk: "+g.candidateRisk()+"\n"+String.join("\n",g.reasons()),14,g.pass()?green:danger)); c.addView(text("Use this gate after a demo remediation to prove the finding did not return.",12,gold)); }

    private void showBaseline(){ clear(); LinearLayout c=card("API BEHAVIOR BASELINE"); String out=ApiBehaviorBaseline.pretty(ApiBehaviorBaseline.learn(history.all())); c.addView(text(out.isBlank()?"No behavior samples yet.":out,13,silver)); }

    private void showDataJourney(){ clear(); LinearLayout c=card("SENSITIVE DATA JOURNEY MAP"); c.addView(text(SensitiveDataJourney.pretty(SensitiveDataJourney.trace(history.all())),13,gold)); c.addView(text("Categories only are shown; Presenter Mode can redact actual secret values before projection.",12,green)); }

    private void showTlsPosture(){ clear(); LinearLayout c=card("TLS POSTURE DASHBOARD"); var demo=List.of(new TlsPostureDashboard.Endpoint("lab.nexvary.local","TLSv1.3","TLS_AES_128_GCM_SHA256",90,true,List.of("Authorized lab profile"))); c.addView(text(TlsPostureDashboard.pretty(TlsPostureDashboard.summarize(demo)),14,cyan)); c.addView(text("When certificate metadata is imported from authorized sessions this dashboard can be populated with the real endpoint posture.",12,silver)); }

    private void showComposer(){ clear(); LinearLayout c=card("DEMO SCENARIO COMPOSER"); c.addView(text(DemoScenarioComposer.script(DemoScenarioComposer.standard()),13,silver)); }

    private void showRemediation(){ clear(); LinearLayout c=card("FINDING → REMEDIATION → VERIFICATION GRAPH"); List<Finding> fs=new ArrayList<>(); for(HistoryEntry e:history.all())fs.addAll(e.findings); c.addView(text(FindingRemediationGraph.pretty(FindingRemediationGraph.build(fs)),13,green)); }

    private void showCockpit(){ clear(); LinearLayout c=card("NEXVARY LECTURE COCKPIT"); var s=LectureCockpit.snapshot(history.all()); c.addView(text(LectureCockpit.pretty(s),15,s.status().equals("ATTENTION")?danger:green)); c.addView(text("Cockpit combines current demo state, risk, routes, timing and the next presenter cue in one screen.",12,gold)); }

    private void showCases(){ clear(); LinearLayout c=card("MULTI-CASE WORKSPACE"); Button create=button("Create lecture case",green); c.addView(create); create.setOnClickListener(v->{activeCaseId=cases.create("NEXVARY Authorized Lecture"); for(HistoryEntry e:history.all())cases.attach(activeCaseId,e.id); cases.note(activeCaseId,"Created from current authorized local-lab session history."); showCases();}); c.addView(text(cases.summary(),13,silver)); LinearLayout r=card("LECTURE RECORDING METADATA"); var rec=LectureRecordingMetadata.build("NEXVARY-DEMO",System.currentTimeMillis()-180000,List.of(new LectureRecordingMetadata.Marker(0,"Introduction","Authorized lab scope"),new LectureRecordingMetadata.Marker(45000,"Intercept","Pause and inspect local request"),new LectureRecordingMetadata.Marker(90000,"Modify","Change demo value"),new LectureRecordingMetadata.Marker(135000,"Remediation","Explain server-side validation"))); r.addView(text(LectureRecordingMetadata.chapterText(rec),12,cyan)); }
    private void showAbout(){ clear();
        LinearLayout hero=card("ABOUT NEXVARY"); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView brand=text("NEXVARY",34,silver); brand.setTypeface(Typeface.create("sans-serif-light",Typeface.BOLD)); brand.setLetterSpacing(.20f); brand.setGravity(Gravity.CENTER); hero.addView(brand);
        TextView inc=text("I N C",13,cyan); inc.setLetterSpacing(.45f); inc.setGravity(Gravity.CENTER); hero.addView(inc);
        TextView strap=text("EXECUTIVE TECHNOLOGY PROTECTION",10,gold); strap.setLetterSpacing(.11f); strap.setGravity(Gravity.CENTER); hero.addView(strap);
        TextView focus=text("TSCM  •  COUNTER-SURVEILLANCE  •  CYBERSECURITY",10,cyan); focus.setGravity(Gravity.CENTER); hero.addView(focus);
        LinearLayout about=card(getString(R.string.about_company_title));
        about.addView(text(getString(R.string.about_company_body),14,silver));
        LinearLayout contact=card(getString(R.string.contact_us)); addContactRow(contact,"WEBSITE","nexvary.com",R.drawable.ic_website,cyan,()->openUrl("https://nexvary.com/")); addContactRow(contact,"EMAIL","info@nexvary.com",R.drawable.ic_email,cyan,()->openEmail("info@nexvary.com")); addContactRow(contact,"FACEBOOK","NEXVARY",R.drawable.ic_facebook,cyan,()->openUrl("https://www.facebook.com/share/14p9krEn5ij/")); addContactRow(contact,"YOUTUBE","@NexvaryInc",R.drawable.ic_youtube,cyan,()->openUrl("https://www.youtube.com/@NexvaryInc")); addContactRow(contact,"X (TWITTER)","@Nexvary",R.drawable.ic_x,cyan,()->openUrl("https://x.com/Nexvary"));
        TextView slogan=text("SECURE TODAY  •  PROTECT TOMORROW",12,gold); slogan.setGravity(Gravity.CENTER); contact.addView(slogan);
        Button exit=outlineIconButton("⏻  "+getString(R.string.exit_app),danger,0); exit.setTag("app:exit"); exit.setOnClickListener(v->confirmExit()); LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(50)); ep.setMargins(0,dp(8),0,0); contact.addView(exit,ep);
    }
    private void addContactRow(LinearLayout host,String label,String value,int icon,int color,Runnable action){ LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(12),dp(8),dp(12),dp(8)); GradientDrawable g=new GradientDrawable(); g.setColor(panel2); g.setCornerRadius(dp(14)); g.setStroke(dp(1),line); row.setBackground(g); android.widget.ImageView iv=new android.widget.ImageView(this); iv.setImageResource(icon); iv.setColorFilter(color); row.addView(iv,new LinearLayout.LayoutParams(dp(30),dp(30))); TextView l=text(label,11,color); l.setTypeface(Typeface.DEFAULT_BOLD); l.setSingleLine(true); row.addView(l,new LinearLayout.LayoutParams(0,-2,.9f)); TextView val=text(value,12,silver); val.setSingleLine(true); row.addView(val,new LinearLayout.LayoutParams(0,-2,1.2f)); TextView arrow=text("›",28,silver); arrow.setGravity(Gravity.CENTER); row.addView(arrow,new LinearLayout.LayoutParams(dp(32),-2)); row.setOnClickListener(v->action.run()); row.setClickable(true); row.setFocusable(true); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58)); lp.setMargins(0,dp(4),0,dp(4)); host.addView(row,lp); }
    private void openUrl(String url){ try{ Intent i=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)); startActivity(i); }catch(Exception e){ Toast.makeText(this,getString(R.string.no_browser),Toast.LENGTH_LONG).show(); } }
    private void openEmail(String email){ try{ Intent i=new Intent(Intent.ACTION_SENDTO,android.net.Uri.parse("mailto:"+email)); startActivity(i); }catch(Exception e){ Toast.makeText(this,getString(R.string.no_email_app),Toast.LENGTH_LONG).show(); } }
    private void confirmExit(){ exitDialog = new AlertDialog.Builder(this).setTitle(getString(R.string.exit_app)).setMessage(getString(R.string.exit_confirm)).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.exit_app),(d,w)->{ exitApplication(); }).show(); }

    @Override protected void onResume(){
        super.onResume();
        if(status!=null && AuthSession.isUnlocked() && importCapturedTraffic()>0){
            if("Dashboard".equals(currentScreen))showDashboard();else if("History".equals(currentScreen))showHistory();
        }
    }
    private int importCapturedTraffic(){
        int added=0;
        for(CapturedTraffic.Exchange exchange:CapturedTraffic.all()){
            if(!importedCaptures.add(exchange.id()))continue;
            LabRequest request=CapturedTraffic.request(exchange);LabResponse response=CapturedTraffic.response(exchange);
            List<Finding> findings=RiskEngine.scan(request,response);HistoryEntry entry=history.add(request,response,findings);
            history.tag(entry.id,"REAL_HTTPS");if(exchange.prefixOnly())history.tag(entry.id,"PREFIX_ONLY");
            evidence.add("HTTPS "+request.method+" "+request.path+(exchange.prefixOnly()?" [PREFIX ONLY]":""),exchange.original()+"\n\nFORWARDED\n"+exchange.forwarded()+"\n\nRESPONSE\n"+exchange.response());
            lastRequest=request;lastResponse=response;added++;
        }
        while(importedCaptures.size()>512)importedCaptures.remove(importedCaptures.iterator().next());
        return added;
    }
    private void exitApplication(){
        AuthSession.revoke();
        stopService(new Intent(this, com.nexvary.trafficlab.vpn.LabVpnService.class));
        stopService(new Intent(this, com.nexvary.trafficlab.tls.EmbeddedProxyService.class));
        server.stop();
        CapturedTraffic.endSession();
        finishAndRemoveTask();
    }
    private String headers(LabRequest r){StringBuilder b=new StringBuilder();for(var e:r.headers.entrySet())b.append(e.getKey()).append(": ").append(e.getValue()).append('\n');return b.toString();}
    private String hex(byte[] a){StringBuilder b=new StringBuilder();for(byte x:a)b.append(String.format("%02X ",x));return b.toString();}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);} }

