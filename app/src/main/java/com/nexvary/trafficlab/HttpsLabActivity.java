package com.nexvary.trafficlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import com.nexvary.trafficlab.core.PcapdroidCapturePlan;
import java.util.*;

/** First-stage external TLS engine integration, using PCAPdroid's public API. */
public class HttpsLabActivity extends Activity {
    private static final int STATUS = 901, START = 902, STOP = 903;
    private TextView state;
    private EditText target;
    private Button start;
    private int engineVersion;
    private boolean engineRunning;

    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(LocaleController.wrap(base)); }
    private String tr(String ar, String en) { return "ar".equals(LocaleController.getLanguage(this)) ? ar : en; }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (!AuthSession.isUnlocked()) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 24, 24, 24);
        page.setBackgroundColor(Color.rgb(2, 9, 18));
        page.setLayoutDirection("ar".equals(LocaleController.getLanguage(this)) ? 1 : 0);
        ScrollView scroll = new ScrollView(this); scroll.addView(page); setContentView(scroll);
        label(page, tr("مختبر HTTPS", "HTTPS Lab"), 25);
        label(page, tr("المحرك الخارجي: PCAPdroid + mitmproxy، على هذا الهاتف. هذه المرحلة تتحكم في الالتقاط؛ عرض البيانات المفكوكة يكون داخل PCAPdroid.",
                "External engine: PCAPdroid + mitmproxy on this phone. This stage controls capture; decrypted data is viewed in PCAPdroid."), 16);
        label(page, tr("جهّز إضافة TLS وشهادة المختبر داخل PCAPdroid أولًا. بدء الالتقاط لا يثبت نجاح فك HTTPS. قد يمنع Pinning أو تشفير داخل البيانات عرض المحتوى.",
                "Configure the TLS addon and lab CA in PCAPdroid first. Starting capture does not prove HTTPS decryption. Pinning or payload encryption may prevent inspection."), 15);
        target = new EditText(this); target.setSingleLine(true); target.setTextColor(Color.WHITE);
        target.setHintTextColor(Color.LTGRAY); target.setHint("com.nexvary.training");
        target.setContentDescription(tr("اسم حزمة تطبيق التدريب", "Training application package")); page.addView(target);
        if (saved != null) target.setText(saved.getString("target", ""));
        action(page, tr("اختيار تطبيق", "Choose application"), this::chooseApp);
        state = label(page, tr("لم يتم التحقق من المحرك بعد", "Engine not checked yet"), 15);
        action(page, tr("فحص المحرك وحالة الالتقاط", "Check engine and capture status"), () -> control("get_status", STATUS));
        start = action(page, tr("بدء التقاط التطبيق وطلب فك HTTPS", "Capture application and request HTTPS decryption"), this::startCapture);
        start.setEnabled(false);
        action(page, tr("إيقاف جلسة PCAPdroid الحالية", "Stop current PCAPdroid capture"), () -> control("stop", STOP));
        action(page, tr("فتح PCAPdroid للإعداد والتحليل", "Open PCAPdroid for setup and analysis"), () -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage(PcapdroidCapturePlan.PACKAGE);
            if (launch == null) missingEngine(); else startActivity(launch);
        });
        action(page, tr("دليل إعداد TLS الرسمي", "Official TLS setup guide"), () -> startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://emanuele-f.github.io/PCAPdroid/tls_decryption.html"))));
        label(page, tr("ملف PCAP يُحفظ لدى المحرك في Downloads/PCAPdroid؛ سقف التفريغ 64 MiB. وصول التفريغ للسقف لا يعني توقف الالتقاط. لا يحتوي PCAP التقليدي تلقائيًا على النص المفكوك.",
                "PCAP is saved by the engine in Downloads/PCAPdroid, with a 64 MiB dump limit. Reaching the limit does not mean capture stopped. A standard PCAP does not automatically contain decrypted plaintext."), 14);
        action(page, tr("رجوع", "Back"), this::finish);
    }
    private TextView label(LinearLayout page, String text, int size) {
        TextView view = new TextView(this); view.setText(text); view.setTextColor(Color.WHITE);
        view.setTextSize(size); view.setPadding(0, 12, 0, 16); page.addView(view); return view;
    }
    private Button action(LinearLayout page, String text, Runnable task) {
        Button button = new Button(this); button.setText(text); button.setAllCaps(false);
        button.setOnClickListener(v -> task.run()); page.addView(button); return button;
    }
    private void chooseApp() {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Map<String, String> apps = new TreeMap<>();
        for (ResolveInfo info : getPackageManager().queryIntentActivities(launcher, 0))
            apps.put(info.activityInfo.packageName, info.loadLabel(getPackageManager()).toString());
        List<String> packages = new ArrayList<>(apps.keySet());
        String[] labels = new String[packages.size()];
        for (int i=0; i<labels.length; i++) labels[i] = apps.get(packages.get(i)) + "\n" + packages.get(i);
        new AlertDialog.Builder(this).setTitle(tr("تطبيق التدريب", "Training application"))
                .setItems(labels, (dialog, which) -> target.setText(packages.get(which))).show();
    }
    private Intent command(String action) {
        return new Intent(Intent.ACTION_VIEW).setClassName(PcapdroidCapturePlan.PACKAGE, PcapdroidCapturePlan.CONTROL).putExtra("action", action);
    }
    private void control(String action, int requestCode) { launch(command(action), requestCode); }
    private void launch(Intent intent, int requestCode) {
        try { startActivityForResult(intent, requestCode); }
        catch (ActivityNotFoundException | SecurityException error) { missingEngine(); }
    }
    private void missingEngine() {
        engineVersion = 0; start.setEnabled(false);
        state.setText(tr("PCAPdroid غير مثبت أو واجهته غير متاحة. ثبته من مصدره الرسمي ثم افحص المحرك.",
                "PCAPdroid is missing or its API is unavailable. Install it from its official source, then check the engine."));
    }
    private void startCapture() {
        if (engineVersion < PcapdroidCapturePlan.MIN_VERSION_CODE || engineRunning) return;
        try {
            Intent intent = command("start");
            for (Map.Entry<String, Object> entry : PcapdroidCapturePlan.start(target.getText().toString().trim(), System.currentTimeMillis()).entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Boolean) intent.putExtra(entry.getKey(), (Boolean)value);
                else if (value instanceof Integer) intent.putExtra(entry.getKey(), (Integer)value);
                else intent.putExtra(entry.getKey(), (String)value);
            }
            start.setEnabled(false); launch(intent, START);
        } catch (IllegalArgumentException error) { target.setError(tr("اختر تطبيقًا أو أدخل اسم حزمة صحيحًا", "Choose an app or enter a valid package")); }
    }
    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code != STATUS && code != START && code != STOP) return;
        if (result != RESULT_OK) {
            start.setEnabled(false);
            state.setText(tr("الطلب لم يكتمل. افحص المحرك وأكمل إعداد الشهادة والصلاحيات.", "Request did not complete. Check engine, certificate setup and permissions.")); return;
        }
        if (code == STATUS) {
            engineVersion = data == null ? 0 : data.getIntExtra("version_code", 0);
            engineRunning = data != null && data.getBooleanExtra("running", false);
            start.setEnabled(engineVersion >= PcapdroidCapturePlan.MIN_VERSION_CODE && !engineRunning);
            state.setText("PCAPdroid " + (data == null ? "?" : data.getStringExtra("version_name")) + "\n" +
                    (engineVersion < PcapdroidCapturePlan.MIN_VERSION_CODE ? tr("يجب تحديث المحرك لدعم إعدادات TLS المطلوبة", "Update engine to support the required TLS settings") :
                    engineRunning ? tr("توجد جلسة التقاط قيد التشغيل", "A capture session is running") : tr("المحرك متاح. جاهزية TLS تحتاج تحققًا عمليًا.", "Engine available. TLS readiness requires a live check.")));
        } else {
            start.setEnabled(false);
            state.setText(code == START ? tr("أكد المحرك بدء الالتقاط. افتحه للتحقق من محتوى HTTPS؛ فك التشفير غير مؤكد بعد.",
                    "Engine acknowledged capture start. Open it to verify HTTPS content; decryption is not yet confirmed.") : tr("أكد المحرك إيقاف الالتقاط. افحص الحالة قبل جلسة جديدة.", "Engine acknowledged capture stop. Check status before a new session."));
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) { if (target != null) out.putString("target", target.getText().toString()); super.onSaveInstanceState(out); }
}
