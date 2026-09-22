package uk.co.dexter.os.dialer;
import android.app.*;import android.os.*;import android.net.*;import android.telecom.*;import android.graphics.Color;import android.view.*;import android.widget.*;
public class MainActivity extends Activity{
 public void onCreate(Bundle b){super.onCreate(b); LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(32,48,32,32);r.setBackgroundColor(Color.rgb(3,6,8));
 TextView t=new TextView(this);t.setText("Dexter Phone");t.setTextColor(Color.WHITE);t.setTextSize(30);r.addView(t);
 EditText n=new EditText(this);n.setTextColor(Color.WHITE);n.setHintTextColor(Color.GRAY);n.setHint("Phone number");n.setTextSize(24);n.setInputType(3);
 Uri d=getIntent().getData();if(d!=null&&"tel".equals(d.getScheme()))n.setText(d.getSchemeSpecificPart());r.addView(n,new LinearLayout.LayoutParams(-1,100));
 Button c=new Button(this);c.setText("Call");c.setOnClickListener(v->{String s=n.getText().toString().trim();if(!s.isEmpty())((TelecomManager)getSystemService(TELECOM_SERVICE)).placeCall(Uri.parse("tel:"+Uri.encode(s)),new Bundle());});r.addView(c);
 setContentView(r);}
}
