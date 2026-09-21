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
import android.widget.SeekBar;
import android.widget.TextView;

public class DexterControlCentreActivity extends Activity {
    private final int bg=Color.rgb(6,10,11), panel=Color.rgb(19,27,31), text=Color.WHITE, muted=Color.rgb(176,182,186), gold=Color.rgb(218,170,78);

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        render();
    }

    private void render(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(14),dp(16),dp(18));
        GradientDrawable back=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(7,14,18),Color.rgb(7,28,42),Color.rgb(6,10,11)});
        root.setBackground(back);

        root.addView(txt("Dexter OS",26,text,true));
        TextView sub=txt("Control Centre",17,Color.rgb(224,224,226),false); sub.setPadding(0,0,0,dp(14)); root.addView(sub);

        GridLayout top=new GridLayout(this); top.setColumnCount(2); root.addView(top,new LinearLayout.LayoutParams(-1,-2));
        quick(top,"Wi-Fi","Connected","⌁",Color.rgb(26,115,231),v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        quick(top,"Bluetooth","On","B",Color.rgb(33,107,238),v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        quick(top,"Mobile Data","On","▮",Color.rgb(25,181,92),v->startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)));
        quick(top,"Battery","86%","▣",Color.rgb(34,177,95),v->startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)));

        slider(root,"☀",72);
        slider(root,"♪",66);

        GridLayout roundGrid=new GridLayout(this); roundGrid.setColumnCount(4);
        LinearLayout.LayoutParams rgp=new LinearLayout.LayoutParams(-1,-2); rgp.topMargin=dp(10); root.addView(roundGrid,rgp);
        roundQuick(roundGrid,"Dark Mode","◐",v->{});
        roundQuick(roundGrid,"Do Not Disturb","☾",v->startActivity(new Intent("android.settings.ZEN_MODE_SETTINGS")));
        roundQuick(roundGrid,"Auto Rotate","↻",v->startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));
        roundQuick(roundGrid,"Torch","✦",v->{});
        roundQuick(roundGrid,"Location","●",v->startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        roundQuick(roundGrid,"Hotspot","⌁",v->startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
        roundQuick(roundGrid,"Airplane Mode","✈",v->startActivity(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)));
        roundQuick(roundGrid,"Screen Record","◉",v->{});

        LinearLayout store=new LinearLayout(this); store.setGravity(Gravity.CENTER_VERTICAL); store.setPadding(dp(14),dp(13),dp(14),dp(13)); store.setBackground(round(panel,18));
        TextView icon=txt("D",20,Color.WHITE,true); icon.setGravity(Gravity.CENTER); icon.setBackground(round(Color.rgb(216,115,75),13));
        store.addView(icon,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL); words.setPadding(dp(10),0,0,0);
        words.addView(txt("Dexter Store",15,text,true)); words.addView(txt("Apps, tools and updates",11,muted,false)); store.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
        TextView arrow=txt("›",25,text,false); store.addView(arrow); store.setOnClickListener(v->startActivity(new Intent(this,DexterStoreActivity.class)));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(14); root.addView(store,sp);

        setContentView(root);
    }

    private void quick(GridLayout g,String name,String status,String glyph,int color,android.view.View.OnClickListener click){
        LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(10),dp(10),dp(10),dp(10)); r.setBackground(round(panel,17)); r.setOnClickListener(click);
        TextView i=txt(glyph,21,Color.WHITE,true); i.setGravity(Gravity.CENTER); i.setBackground(round(color,30)); r.addView(i,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout w=new LinearLayout(this); w.setOrientation(LinearLayout.VERTICAL); w.setPadding(dp(8),0,0,0); w.addView(txt(name,13,text,true)); w.addView(txt(status,11,muted,false)); r.addView(w);
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=dp(78); lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); lp.setMargins(dp(4),dp(4),dp(4),dp(4)); r.setLayoutParams(lp); g.addView(r);
    }

    private void slider(LinearLayout root,String glyph,int progress){
        LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(10),dp(7),dp(10),dp(7)); r.setBackground(round(panel,20));
        TextView i=txt(glyph,20,text,false); i.setGravity(Gravity.CENTER); r.addView(i,new LinearLayout.LayoutParams(dp(38),dp(38)));
        SeekBar bar=new SeekBar(this); bar.setMax(100); bar.setProgress(progress); r.addView(bar,new LinearLayout.LayoutParams(0,dp(44),1f));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56)); lp.topMargin=dp(8); root.addView(r,lp);
    }

    private void roundQuick(GridLayout g,String name,String glyph,android.view.View.OnClickListener click){
        LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL); r.setPadding(dp(2),dp(7),dp(2),dp(5)); r.setOnClickListener(click);
        TextView i=txt(glyph,19,text,true); i.setGravity(Gravity.CENTER); i.setBackground(round(Color.rgb(28,34,38),30)); r.addView(i,new LinearLayout.LayoutParams(dp(52),dp(52)));
        TextView n=txt(name,8.5f,text,false); n.setGravity(Gravity.CENTER); n.setMaxLines(1); n.setPadding(0,dp(5),0,0); r.addView(n,new LinearLayout.LayoutParams(-1,-2));
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=dp(84); lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); r.setLayoutParams(lp); g.addView(r);
    }

    private TextView txt(String v,float s,int c,boolean b){TextView x=new TextView(this);x.setText(v);x.setTextSize(s);x.setTextColor(c);if(b)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));return x;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
