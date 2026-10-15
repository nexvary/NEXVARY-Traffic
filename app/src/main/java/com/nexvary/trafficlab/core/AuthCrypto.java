package com.nexvary.trafficlab.core;

import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class AuthCrypto {
    public static final int ITERATIONS=120_000, KEY_BITS=256;
    private AuthCrypto() {}
    public static byte[] newSalt(){byte[] s=new byte[16];new SecureRandom().nextBytes(s);return s;}
    public static byte[] derive(char[] pass,byte[] salt) throws Exception{PBEKeySpec spec=new PBEKeySpec(pass,salt,ITERATIONS,KEY_BITS);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
    public static boolean matches(char[] pass,byte[] salt,byte[] expected){try{return MessageDigest.isEqual(expected,derive(pass,salt));}catch(Exception e){return false;}}
    public static boolean isStrongEnough(String password){if(password==null||password.length()<6||password.length()>128)return false;boolean digit=false,letter=false;for(char ch:password.toCharArray()){if(Character.isDigit(ch))digit=true;if(Character.isLetter(ch))letter=true;}return digit&&letter;}
    public static boolean validEmail(String email){return email!=null&&email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");}
    public static String normalizeEmail(String email){return email==null?"":email.trim().toLowerCase(java.util.Locale.ROOT);}
}
