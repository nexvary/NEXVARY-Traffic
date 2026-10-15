package com.nexvary.trafficlab;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.util.Locale;

public final class LocaleController {
    private static final String PREFS = "nexvary_ui";
    private static final String KEY = "language";
    private LocaleController() {}

    public static Context wrap(Context base) {
        String code = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "en");
        Locale locale = Locale.forLanguageTag(code);
        Locale.setDefault(locale);
        Configuration c = new Configuration(base.getResources().getConfiguration());
        c.setLocale(locale);
        c.setLayoutDirection(locale);
        return base.createConfigurationContext(c);
    }
    public static void setLanguage(Context context, String code) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(KEY, code).apply();
    }
    public static String getLanguage(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "en");
    }
}
