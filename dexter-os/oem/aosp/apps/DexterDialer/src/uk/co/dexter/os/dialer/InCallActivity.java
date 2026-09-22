package uk.co.dexter.os.dialer;
import android.app.*;import android.os.*;import android.graphics.Color;import android.telecom.*;import android.view.*;import android.widget.*;
public class InCallActivity extends Activity{
 TextView status;
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER);r.setBackgroundColor(Color.rgb(3,6,8));
 status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(26);status.setText("Dexter Call");r.addView(status);
 Button a=new Button(this);a.setText("Answer");a.setOnClickListener(v->{Call c=DexterInCallService.current;if(c!=null)c.answer(0);});r.addView(a);
 Button h=new Button(this);h.setText("End Call");h.setOnClickListener(v->{Call c=DexterInCallService.current;if(c!=null)c.disconnect();});r.addView(h);setContentView(r);}
}
