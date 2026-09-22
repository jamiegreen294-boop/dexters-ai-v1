package uk.co.dexter.os.camera;

import android.app.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;

/*
 * Retail camera shell.
 *
 * The final Camera2/CameraX pipeline is device-specific because lens IDs,
 * stream combinations, stabilisation, flash behaviour and vendor extensions
 * must be validated against the selected OEM camera HAL. This activity is the
 * Dexter-owned customer surface and deliberately refuses to fall through to a
 * stock Android camera.
 */
public class CameraActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout r=new LinearLayout(this);
        r.setGravity(Gravity.CENTER);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(32,32,32,32);
        r.setBackgroundColor(Color.BLACK);
        TextView t=new TextView(this);
        t.setText("Dexter Camera");
        t.setTextColor(Color.WHITE);
        t.setTextSize(28);
        r.addView(t);
        TextView s=new TextView(this);
        s.setText("Camera hardware is being initialised by the Dexter device profile.");
        s.setTextColor(Color.LTGRAY);
        s.setTextSize(16);
        s.setPadding(0,18,0,0);
        r.addView(s);
        setContentView(r);
    }
}
