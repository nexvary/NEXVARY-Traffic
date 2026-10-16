package com.nexvary.trafficlab;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class LoginActivity extends Activity {
    private final int bg=Color.rgb(4,16,12), panel=Color.rgb(8,29,22), gold=Color.rgb(255,209,102), electric=Color.rgb(57,255,136), emerald=Color.rgb(0,200,151), silver=Color.rgb(218,226,222), danger=Color.rgb(255,88,111);
    private AlertDialog recoveryDialog;
    private int failedAttempts=0; private long lockedUntil=0;

    @Override protected void attachBaseContext(Context base){ super.attachBaseContext(LocaleController.wrap(base)); }
    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg); render(); }
    @Override public void onBackPressed(){ Toast.makeText(this,getString(R.string.back_disabled_login),Toast.LENGTH_SHORT).show(); }

    private void render(){ if(AuthManager.isConfigured(this)) showLogin(); else showSetup(); }

    private LinearLayout base(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg); root.setPadding(dp(22),dp(22),dp(22),dp(22)); root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setLayoutDirection(isRtl()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        if(Build.VERSION.SDK_INT>=30) root.setOnApplyWindowInsetsListener((v,i)->{ android.graphics.Insets s=i.getInsets(WindowInsets.Type.systemBars()); v.setPadding(dp(22)+s.left,dp(22)+s.top,dp(22)+s.right,dp(22)+s.bottom); return i; });
        TextView logo=t("NEXVARY",32,gold,true); logo.setGravity(Gravity.CENTER); root.addView(logo,new LinearLayout.LayoutParams(-1,-2));
        TextView sub=t(getString(R.string.secure_access),16,electric,true); sub.setGravity(Gravity.CENTER); root.addView(sub);
        return root;
    }
    private void showSetup(){
        LinearLayout root=base(); LinearLayout card=card();
        card.addView(t(getString(R.string.create_passcode_title),22,gold,true)); card.addView(t(getString(R.string.create_passcode_desc),14,silver,false));
        EditText p1=input(getString(R.string.passcode),true); EditText p2=input(getString(R.string.confirm_passcode),true); EditText email=input(getString(R.string.recovery_email),false);
        p1.setTag("login:setup-password");p2.setTag("login:setup-confirm");email.setTag("login:setup-email");email.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); card.addView(p1); card.addView(p2); card.addView(email);
        Button create=button(getString(R.string.create_secure_access),electric,R.drawable.ic_lock); create.setTag("login:create");card.addView(create);
        TextView msg=t("",13,danger,false);msg.setTag("login:message"); card.addView(msg);
        create.setOnClickListener(v->{String a=p1.getText().toString(),b=p2.getText().toString(),e=email.getText().toString(); if(!a.equals(b)){msg.setText(getString(R.string.passcodes_not_match));return;} if(!AuthManager.setup(this,a,e)){msg.setText(getString(R.string.passcode_requirements));return;} showLogin();});
        root.addView(card,new LinearLayout.LayoutParams(-1,-2)); setContentView(root);
    }
    private void showLogin(){
        LinearLayout root=base(); LinearLayout card=card(); card.addView(t(getString(R.string.unlock_title),22,gold,true)); card.addView(t(getString(R.string.unlock_desc),14,silver,false));
        EditText pass=input(getString(R.string.passcode),true);pass.setTag("login:password"); card.addView(pass); Button unlock=button(getString(R.string.unlock),electric,R.drawable.ic_shield); unlock.setTag("login:unlock");card.addView(unlock);
        Button forgot=button(getString(R.string.forgot_passcode),gold,R.drawable.ic_language); forgot.setTag("login:forgot");card.addView(forgot); TextView msg=t("",13,danger,false);msg.setTag("login:message"); card.addView(msg);
        unlock.setOnClickListener(v->{long now=System.currentTimeMillis(); if(now<lockedUntil){msg.setText(getString(R.string.try_again_later));return;} if(AuthManager.verify(this,pass.getText().toString())){failedAttempts=0; AuthSession.grant(); startActivity(new Intent(this,MainActivity.class)); pass.setText(""); finish();} else {failedAttempts++; msg.setText(getString(R.string.invalid_passcode)); if(failedAttempts>=5){lockedUntil=now+30_000;failedAttempts=0;msg.setText(getString(R.string.too_many_attempts));}}});
        forgot.setOnClickListener(v->showRecoveryDialog()); root.addView(card,new LinearLayout.LayoutParams(-1,-2)); setContentView(root);
    }
    private void showRecoveryDialog(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(14),dp(6),dp(14),0); EditText email=input(getString(R.string.recovery_email),false); EditText code=input(getString(R.string.recovery_code),false); EditText np=input(getString(R.string.new_passcode),true); email.setTag("recovery:email");code.setTag("recovery:code");np.setTag("recovery:password");box.addView(email); box.addView(code); box.addView(np);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(getString(R.string.email_recovery)).setView(box).setNegativeButton(getString(R.string.cancel),null).setNeutralButton(getString(R.string.send_code),null).setPositiveButton(getString(R.string.reset_passcode),null).create();
        recoveryDialog=d;
        d.setOnShowListener(x->{d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{String e=email.getText().toString(); if(!AuthManager.emailMatches(this,e)){toast(R.string.recovery_email_mismatch);return;} if(!RecoveryClient.isConfigured()){toast(R.string.recovery_service_not_configured);return;} new Thread(()->{RecoveryClient.Result r=RecoveryClient.request(e);runOnUiThread(()->toast(r.ok()?R.string.recovery_code_sent:R.string.recovery_failed));}).start();});
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String e=email.getText().toString(),c=code.getText().toString(),n=np.getText().toString(); if(!AuthManager.emailMatches(this,e)){toast(R.string.recovery_email_mismatch);return;} if(!AuthManager.isStrongEnough(n)){toast(R.string.passcode_requirements);return;} if(!RecoveryClient.isConfigured()){toast(R.string.recovery_service_not_configured);return;} new Thread(()->{RecoveryClient.Result r=RecoveryClient.verify(e,c);runOnUiThread(()->{if(r.ok()&&AuthManager.resetPassword(this,n)){toast(R.string.passcode_reset_done);d.dismiss();}else toast(R.string.recovery_failed);});}).start();});}); d.show();
    }
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(18),dp(18),dp(18));GradientDrawable g=new GradientDrawable();g.setColor(panel);g.setCornerRadius(dp(20));g.setStroke(dp(1),emerald);c.setBackground(g);return c;}
    private EditText input(String hint,boolean password){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(130,155,145));e.setTextColor(silver);e.setSingleLine(true);e.setPadding(dp(14),dp(12),dp(14),dp(12));GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(5,22,17));g.setCornerRadius(dp(12));g.setStroke(dp(1),Color.rgb(31,92,70));e.setBackground(g);if(password)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(10),0,0);e.setLayoutParams(lp);e.setGravity(isRtl()?Gravity.RIGHT:Gravity.LEFT);return e;}
    private Button button(String s,int color,int icon){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(4,16,12));b.setTextSize(14);b.setAllCaps(false);b.setCompoundDrawablePadding(dp(8));LabIcons.button(b,icon);GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(14));b.setBackground(g);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(12),0,0);b.setLayoutParams(lp);return b;}
    private TextView t(String s,int sp,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);if(bold)v.setTypeface(null,Typeface.BOLD);v.setGravity(isRtl()?Gravity.RIGHT:Gravity.LEFT);v.setPadding(0,dp(5),0,dp(5));return v;}
    private boolean isRtl(){return "ar".equals(LocaleController.getLanguage(this));} private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);} private void toast(int id){Toast.makeText(this,getString(id),Toast.LENGTH_LONG).show();}
}
