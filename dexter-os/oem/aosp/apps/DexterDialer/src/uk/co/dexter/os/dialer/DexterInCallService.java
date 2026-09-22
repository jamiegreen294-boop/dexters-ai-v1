package uk.co.dexter.os.dialer;
import android.content.*;import android.telecom.*;
public class DexterInCallService extends InCallService{
 static volatile Call current;
 public void onCallAdded(Call c){super.onCallAdded(c);current=c;Intent i=new Intent(this,InCallActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);}
 public void onCallRemoved(Call c){if(current==c)current=null;super.onCallRemoved(c);}
}
