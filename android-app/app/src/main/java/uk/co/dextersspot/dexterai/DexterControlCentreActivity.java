package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DexterControlCentreActivity extends Activity {
    private final int bg=Color.rgb(8,13,12), card=Color.rgb(29,35,32), text=Color.WHITE, muted=Color.rgb(185,190,194), accent=Color.rgb(239,139,34);

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);render();}

    private void render(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(24)); root.setBackgroundColor(bg);
        root.addView(title("Control Centre",30,text,true));
        TextView sub=title("Dexter OS quick controls",13,muted,false); sub.setPadding(0,dp(4),0,dp(18)); root.addView(sub);

        GridLayout grid=new GridLayout(this); grid.setColumnCount(2); root.addView(grid,new LinearLayout.LayoutParams(-1,0,1f));
        tile(grid,"Wi-Fi","⌁",Color.rgb(47,118,220),v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        tile(grid,"Bluetooth","B",Color.rgb(63,103,232),v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        tile(grid,"Notifications","●",Color.rgb(43,126,110),v->startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName())));
        tile(grid,"Display","☀",Color.rgb(225,160,45),v->startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
        tile(grid,"Sound","♪",Color.rgb(155,87,190),v->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)));
        tile(grid,"Battery","⚡",Color.rgb(45,160,93),v->startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)));
        tile(grid,"Dexter Store","D",accent,v->startActivity(new Intent(this,DexterStoreActivity.class)));
        tile(grid,"Settings","⚙",Color.rgb(78,83,89),v->startActivity(new Intent(this,DexterSettingsActivity.class)));
        setContentView(root);
    }

    private void tile(GridLayout g,String label,String glyph,int color,android.view.View.OnClickListener click){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(12),dp(18),dp(12),dp(18)); box.setBackground(round(card,22)); box.setOnClickListener(click);
        TextView icon=title(glyph,26,Color.WHITE,true); icon.setGravity(Gravity.CENTER); icon.setBackground(round(color,18)); box.addView(icon,new LinearLayout.LayoutParams(dp(58),dp(58)));
        TextView name=title(label,13,text,true); name.setGravity(Gravity.CENTER); name.setPadding(0,dp(10),0,0); box.addView(name);
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=dp(126); lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); lp.setMargins(dp(5),dp(5),dp(5),dp(5)); box.setLayoutParams(lp); g.addView(box);
    }

    private TextView title(String v,float size,int color,boolean bold){TextView x=new TextView(this);x.setText(v);x.setTextSize(size);x.setTextColor(color);if(bold)x.setTypeface(Typeface.DEFAULT_BOLD);return x;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
