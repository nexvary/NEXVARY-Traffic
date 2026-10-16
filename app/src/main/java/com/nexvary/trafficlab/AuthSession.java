package com.nexvary.trafficlab;

/** Process-local unlock state. Process death intentionally requires re-authentication. */
public final class AuthSession {
    private static volatile boolean unlocked=false;
    private AuthSession() {}
    public static void grant(){unlocked=true;}
    public static void revoke(){unlocked=false;}
    public static boolean isUnlocked(){return unlocked;}
}
