package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DexterSettingsActivity extends Activity {
    private final int bg=Color.rgb(8,13,12), card=Color.rgb(27,33,30), text=Color.WHITE, muted=Color.rgb(185,190,194), accent=Color.rgb(239,139,34);

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);render();}

    private void render(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(26)); root.setBackgroundColor(bg); scroll.addView(root);
        root.addView(label("Dexter Settings",30,text,true));
        TextView sub=label("Team device settings",13,muted,false); sub.setPadding(0,dp(4),0,dp(18)); root.addView(sub);

        section(root,"DEVICE");
        row(root,"Wi-Fi","Network and internet",v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        row(root,"Bluetooth","Accessories and scanners",v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        row(root,"Display","Brightness and screen",v->startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
        row(root,"Sound","Volume and alerts",v->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)));

        section(root,"DEXTER");
        row(root,"Dexter Store","Approved apps",v->startActivity(new Intent(this,DexterStoreActivity.class)));
        row(root,"Dexter Messages","Orders and offers",v->startActivity(new Intent(this,DexterInboxActivity.class)));
        row(root,"Control Centre","Quick settings",v->startActivity(new Intent(this,DexterControlCentreActivity.class)));
        row(root,"Dexter AI","Business assistant",v->startActivity(new Intent(this,MainActivity.class)));

        section(root,"OWNER");
        row(root,"System settings","Android maintenance settings",v->startActivity(new Intent(Settings.ACTION_SETTINGS)));
        row(root,"About Dexter OS","Preview channel · Team edition",v->{});
        setContentView(scroll);
    }

    private void section(LinearLayout root,String name){TextView x=label(name,11,accent,true);x.setPadding(0,dp(14),0,dp(8));root.addView(x);}
    private void row(LinearLayout root,String name,String sub,android.view.View.OnClickListener click){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(16),dp(13),dp(16),dp(13));r.setBackground(round(card,18));r.setOnClickListener(click);
        r.addView(label(name,15,text,true));TextView s=label(sub,12,muted,false);s.setPadding(0,dp(3),0,0);r.addView(s);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(9));root.addView(r,lp);
    }
    private TextView label(String v,float size,int color,boolean bold){TextView x=new TextView(this);x.setText(v);x.setTextSize(size);x.setTextColor(color);if(bold)x.setTypeface(Typeface.DEFAULT_BOLD);return x;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
