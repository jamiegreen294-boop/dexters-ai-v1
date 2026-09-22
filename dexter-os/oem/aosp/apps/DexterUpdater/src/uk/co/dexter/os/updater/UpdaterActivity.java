package uk.co.dexter.os.updater;

import android.app.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;

public class UpdaterActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(34,48,34,34);
        r.setBackgroundColor(Color.rgb(3,6,8));
        TextView t=new TextView(this);t.setText("Dexter Update");t.setTextColor(Color.WHITE);t.setTextSize(30);r.addView(t);
        TextView s=new TextView(this);s.setText("Stable channel\nSigned OTA only\nRollback protection required");s.setTextColor(Color.LTGRAY);s.setTextSize(17);s.setPadding(0,18,0,18);r.addView(s);
        TextView note=new TextView(this);note.setText("The device-specific OEM build supplies the signed A/B OTA payload and update-engine metadata. Dexter Update never installs an unsigned image.");note.setTextColor(Color.WHITE);note.setTextSize(15);r.addView(note);
        setContentView(r);
    }
}
