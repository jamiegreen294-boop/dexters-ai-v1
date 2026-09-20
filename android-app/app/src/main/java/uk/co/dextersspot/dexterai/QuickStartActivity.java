package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class QuickStartActivity extends Activity {
    private int bg=Color.rgb(19,18,17), text=Color.WHITE, muted=Color.rgb(190,187,181);
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        ScrollView s=new ScrollView(this);
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(20),dp(24),dp(20),dp(32));r.setBackgroundColor(bg);s.addView(r);
        add(r,"Dexters Business Phone",28,true);
        add(r,"Company-owned and managed by Dexter AI.",16,false);
        add(r,"Use this phone for Dexters business work. Dexter may process device identity, security/compliance state, installed-app inventory, last-seen status and management-job results so the phone can be secured and supported.",16,false);
        add(r,"Normal device management does not include continuous location tracking, keystrokes, screenshots, microphone recordings, camera capture or private-message inspection.",16,false);
        add(r,"If the phone is lost, stolen, compromised or behaving suspiciously, report it immediately. Dexter can remotely lock the phone and, where necessary and authorised, wipe it.",16,false);
        add(r,"Do not remove Dexter management, root the phone, bypass security controls, install unapproved APKs or copy business data to personal accounts.",16,false);
        add(r,"The full Device Management, Internet/Communications and Monitoring Privacy policies are stored in the Dexter AI project and should be provided to staff who are issued the device.",16,false);
        setContentView(s);
    }
    private void add(LinearLayout r,String v,int size,boolean bold){
        TextView t=new TextView(this);t.setText(v);t.setTextColor(bold?text:muted);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,0,0,dp(14));r.addView(t);
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}