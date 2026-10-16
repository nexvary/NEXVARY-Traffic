package com.nexvary.trafficlab;
import android.app.*;
import android.content.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;

/** Real registration/unlock/lock controls and fail-closed unconfigured recovery. */
public final class AuthenticationUiTest extends InstrumentationTestCase {
 private View find(View root,String tag){if(tag.equals(root.getTag()))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),tag);if(found!=null)return found;}}return null;}
 private void set(Activity activity,String tag,String text)throws Exception{getInstrumentation().runOnMainSync(()->{View view=find(activity.getWindow().getDecorView(),tag);assertNotNull(tag,view);((EditText)view).setText(text);});}
 private void click(Activity activity,String tag)throws Exception{getInstrumentation().runOnMainSync(()->{View view=find(activity.getWindow().getDecorView(),tag);assertNotNull(tag,view);assertTrue(view.hasOnClickListeners());view.performClick();});getInstrumentation().waitForIdleSync();}
 public void testCreateRejectWrongCodeUnlockLockAndRecoveryControls()throws Exception{
  Context context=getInstrumentation().getTargetContext();LocaleController.setLanguage(context,"en");context.getSharedPreferences("nexvary_auth",0).edit().clear().commit();AuthSession.revoke();
  LoginActivity first=(LoginActivity)getInstrumentation().startActivitySync(new Intent(context,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Activity main=null;LoginActivity locked=null;
  try{
   set(first,"login:setup-password","Nexvary-Lab#1234");set(first,"login:setup-confirm","different");set(first,"login:setup-email","training@example.com");click(first,"login:create");assertFalse("Mismatched passwords registered",AuthManager.isConfigured(context));
   set(first,"login:setup-confirm","Nexvary-Lab#1234");click(first,"login:create");assertTrue(AuthManager.isConfigured(context));
   set(first,"login:password","wrong-code");click(first,"login:unlock");assertFalse(AuthSession.isUnlocked());assertFalse(((TextView)find(first.getWindow().getDecorView(),"login:message")).getText().toString().isEmpty());
   Instrumentation.ActivityMonitor opened=getInstrumentation().addMonitor(MainActivity.class.getName(),null,false);set(first,"login:password","Nexvary-Lab#1234");click(first,"login:unlock");main=getInstrumentation().waitForMonitorWithTimeout(opened,5000);getInstrumentation().removeMonitor(opened);assertNotNull(main);assertTrue(AuthSession.isUnlocked());
   Instrumentation.ActivityMonitor relock=getInstrumentation().addMonitor(LoginActivity.class.getName(),null,false);click(main,"app:lock");locked=(LoginActivity)getInstrumentation().waitForMonitorWithTimeout(relock,5000);getInstrumentation().removeMonitor(relock);assertNotNull(locked);assertFalse(AuthSession.isUnlocked());
   click(locked,"login:forgot");Field field=LoginActivity.class.getDeclaredField("recoveryDialog");field.setAccessible(true);AlertDialog dialog=(AlertDialog)field.get(locked);assertTrue(dialog.isShowing());
   getInstrumentation().runOnMainSync(()->{((EditText)find(dialog.getWindow().getDecorView(),"recovery:email")).setText("training@example.com");((EditText)find(dialog.getWindow().getDecorView(),"recovery:code")).setText("not-a-verified-code");((EditText)find(dialog.getWindow().getDecorView(),"recovery:password")).setText("Replacement#5678");dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});
   assertTrue("Unconfigured recovery changed the passcode",AuthManager.verify(context,"Nexvary-Lab#1234"));assertFalse(AuthSession.isUnlocked());
   getInstrumentation().runOnMainSync(()->dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());assertFalse(dialog.isShowing());
  }finally{if(main!=null){Activity close=main;getInstrumentation().runOnMainSync(close::finish);}if(locked!=null){Activity close=locked;getInstrumentation().runOnMainSync(close::finish);}getInstrumentation().runOnMainSync(first::finish);context.getSharedPreferences("nexvary_auth",0).edit().clear().commit();AuthSession.revoke();}
 }
}
