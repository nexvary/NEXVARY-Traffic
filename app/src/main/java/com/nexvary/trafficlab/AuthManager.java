package com.nexvary.trafficlab;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import com.nexvary.trafficlab.core.AuthCrypto;

public final class AuthManager {
    private static final String PREFS = "nexvary_auth";
    private static final String K_HASH = "pass_hash";
    private static final String K_SALT = "pass_salt";
    private static final String K_EMAIL = "recovery_email";
    private AuthManager() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isConfigured(Context c) {
        SharedPreferences p = prefs(c);
        return p.contains(K_HASH) && p.contains(K_SALT) && !p.getString(K_EMAIL, "").trim().isEmpty();
    }

    public static String recoveryEmail(Context c) {
        return prefs(c).getString(K_EMAIL, "");
    }

    public static boolean setup(Context c, String password, String email) {
        if (!AuthCrypto.isStrongEnough(password) || !AuthCrypto.validEmail(email)) return false;
        try {
            byte[] salt = AuthCrypto.newSalt();
            byte[] hash = AuthCrypto.derive(password.toCharArray(), salt);
            prefs(c).edit()
                    .putString(K_SALT, Base64.getEncoder().encodeToString(salt))
                    .putString(K_HASH, Base64.getEncoder().encodeToString(hash))
                    .putString(K_EMAIL, AuthCrypto.normalizeEmail(email))
                    .apply();
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean verify(Context c, String password) {
        try {
            SharedPreferences p = prefs(c);
            byte[] salt = Base64.getDecoder().decode(p.getString(K_SALT, ""));
            byte[] expected = Base64.getDecoder().decode(p.getString(K_HASH, ""));
            return AuthCrypto.matches(password.toCharArray(), salt, expected);
        } catch (Exception e) { return false; }
    }

    public static boolean resetPassword(Context c, String newPassword) {
        if (!AuthCrypto.isStrongEnough(newPassword)) return false;
        return setup(c, newPassword, recoveryEmail(c));
    }

    public static boolean emailMatches(Context c, String email) {
        String saved = recoveryEmail(c);
        return email != null && !saved.trim().isEmpty() && MessageDigest.isEqual(
                saved.getBytes(StandardCharsets.UTF_8), AuthCrypto.normalizeEmail(email).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isStrongEnough(String password) { return AuthCrypto.isStrongEnough(password); }

}
