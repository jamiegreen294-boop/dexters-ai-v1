package uk.co.dexter.os.messaging;
import android.app.*;import android.os.*;import android.graphics.Color;import android.telephony.SmsManager;import android.view.*;import android.widget.*;
public class MainActivity extends Activity{
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(30,44,30,30);r.setBackgroundColor(Color.rgb(3,6,8));
 TextView t=new TextView(this);t.setText("Dexter Messages");t.setTextColor(Color.WHITE);t.setTextSize(30);r.addView(t);
 EditText n=new EditText(this);n.setHint("Mobile number");n.setTextColor(Color.WHITE);n.setHintTextColor(Color.GRAY);n.setInputType(3);r.addView(n);
 EditText m=new EditText(this);m.setHint("Message");m.setTextColor(Color.WHITE);m.setHintTextColor(Color.GRAY);m.setMinLines(4);r.addView(m);
 Button s=new Button(this);s.setText("Send");s.setOnClickListener(v->{String to=n.getText().toString().trim(),body=m.getText().toString();if(!to.isEmpty()&&!body.isEmpty())SmsManager.getDefault().sendTextMessage(to,null,body,null,null);});r.addView(s);setContentView(r);}
}
