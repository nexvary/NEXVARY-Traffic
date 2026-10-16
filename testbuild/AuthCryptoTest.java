import com.nexvary.trafficlab.core.AuthCrypto;
import java.security.MessageDigest;
public class AuthCryptoTest {
  public static void main(String[] args) throws Exception {
    byte[] salt=AuthCrypto.newSalt(); byte[] a=AuthCrypto.derive("Nexvary7".toCharArray(),salt);
    if(!AuthCrypto.matches("Nexvary7".toCharArray(),salt,a)) throw new AssertionError("valid password rejected");
    if(AuthCrypto.matches("Wrong77".toCharArray(),salt,a)) throw new AssertionError("wrong password accepted");
    if(!AuthCrypto.isStrongEnough("Nexvary7")||AuthCrypto.isStrongEnough("abcdef")) throw new AssertionError("strength rule");
    if(!AuthCrypto.validEmail("lab@nexvary.com")||AuthCrypto.validEmail("bad@")) throw new AssertionError("email rule");
    System.out.println("AUTH_CRYPTO_OK pbkdf2=SHA256 iterations="+AuthCrypto.ITERATIONS+" hashBytes="+a.length);
  }
}
