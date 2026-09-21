package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DexterSettingsActivity extends Activity {
    private final int bg=Color.rgb(6,10,11), card=Color.rgb(19,25,29), text=Color.WHITE, muted=Color.rgb(175,181,185), gold=Color.rgb(218,170,78);

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);render();}

    private void render(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(14),dp(16),dp(24)); root.setBackgroundColor(bg); scroll.addView(root);

        root.addView(txt("Settings",28,text,true));

        EditText search=new EditText(this); search.setHint("Search settings"); search.setSingleLine(true); search.setTextColor(text); search.setHintTextColor(muted); search.setTextSize(13);
        search.setPadding(dp(14),0,dp(14),0); search.setBackground(round(Color.rgb(28,35,40),22));
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(46)); slp.topMargin=dp(10); slp.bottomMargin=dp(10); root.addView(search,slp);

        LinearLayout owner=new LinearLayout(this); owner.setGravity(Gravity.CENTER_VERTICAL); owner.setPadding(dp(12),dp(11),dp(12),dp(11)); owner.setBackground(round(card,18));
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.ic_launcher); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE); owner.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout ow=new LinearLayout(this); ow.setOrientation(LinearLayout.VERTICAL); ow.setPadding(dp(10),0,0,0); ow.addView(txt("Dexters Business",15,text,true)); ow.addView(txt("Owner",11,muted,false)); owner.addView(ow,new LinearLayout.LayoutParams(0,-2,1f));
        owner.addView(txt("›",25,text,false)); root.addView(owner);

        section(root,"Device");
        row(root,"⌁","Network & Internet","Wi-Fi, mobile data and hotspot",v->startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
        row(root,"▣","Connected Devices","Bluetooth and accessories",v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        row(root,"☀","Display","Brightness, timeout and rotation",v->startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
        row(root,"♪","Sound & Vibration","Volume and notifications",v->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)));
        row(root,"▥","Battery","Power and battery saver",v->startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)));
        row(root,"≡","Storage","Files and device storage",v->startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
        row(root,"◆","Security & Privacy","Lock screen and permissions",v->startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)));

        section(root,"Dexter");
        row(root,"AI","Dexter AI","Business assistant",v->startActivity(new Intent(this,MainActivity.class)));
        row(root,"▦","Dexter Apps","Approved business apps",v->startActivity(new Intent(this,DexterStoreActivity.class)));
        row(root,"▤","Business Tools","POS, scanner and back office",v->startActivity(new Intent(this,BusinessAppsActivity.class)));
        row(root,"D","Dexter Store","Apps, tools and updates",v->startActivity(new Intent(this,DexterStoreActivity.class)));
        row(root,"i","About Dexter OS","Team Preview",v->{});

        section(root,"Owner");
        row(root,"○","Accounts","Managed business accounts",v->startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS)));
        row(root,"⚙","System","Android maintenance settings",v->startActivity(new Intent(Settings.ACTION_SETTINGS)));

        setContentView(scroll);
    }

    private void section(LinearLayout root,String name){TextView s=txt(name,13,text,true); s.setPadding(0,dp(14),0,dp(7)); root.addView(s);}
    private void row(LinearLayout root,String glyph,String name,String sub,android.view.View.OnClickListener click){
        LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(10),dp(10),dp(10),dp(10)); r.setBackground(round(card,14)); r.setOnClickListener(click);
        TextView i=txt(glyph,glyph.length()>1?11:18,gold,true); i.setGravity(Gravity.CENTER); r.addView(i,new LinearLayout.LayoutParams(dp(34),dp(34)));
        LinearLayout w=new LinearLayout(this); w.setOrientation(LinearLayout.VERTICAL); w.setPadding(dp(7),0,0,0); w.addView(txt(name,13.5f,text,true)); w.addView(txt(sub,10.5f,muted,false)); r.addView(w,new LinearLayout.LayoutParams(0,-2,1f));
        r.addView(txt("›",22,text,false));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(6)); root.addView(r,lp);
    }

    private TextView txt(String v,float s,int c,boolean b){TextView x=new TextView(this);x.setText(v);x.setTextSize(s);x.setTextColor(c);if(b)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));return x;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
