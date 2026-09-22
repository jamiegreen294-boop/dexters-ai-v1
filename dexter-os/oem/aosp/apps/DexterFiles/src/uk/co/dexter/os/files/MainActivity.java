package uk.co.dexter.os.files;
import android.app.*;import android.os.*;import android.graphics.Color;import android.view.*;import android.widget.*;import java.io.*;
public class MainActivity extends Activity{
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(28,40,28,28);r.setBackgroundColor(Color.rgb(3,6,8));
 TextView t=new TextView(this);t.setText("Dexter Files");t.setTextColor(Color.WHITE);t.setTextSize(30);r.addView(t);
 File d=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);File[] fs=d.listFiles();if(fs!=null)for(File f:fs){TextView x=new TextView(this);x.setText(f.getName());x.setTextColor(Color.WHITE);x.setTextSize(18);x.setPadding(0,18,0,18);r.addView(x);}setContentView(r);}
}
