package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DexterStoreActivity extends Activity {
    private final int bg=Color.rgb(6,10,11), card=Color.rgb(20,27,30);
    private final int text=Color.WHITE, muted=Color.rgb(175,181,185), gold=Color.rgb(218,170,78);

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);render();}

    private void render(){
        LinearLayout shell=new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(bg);

        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(14),dp(16),dp(18)); scroll.addView(root);
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        root.addView(txt("Dexter Store",27,text,true));

        EditText search=new EditText(this); search.setHint("Search apps, tools and updates"); search.setSingleLine(true);
        search.setTextColor(text); search.setHintTextColor(muted); search.setTextSize(13); search.setPadding(dp(14),0,dp(14),0);
        search.setBackground(round(Color.rgb(28,35,40),22));
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(46)); slp.topMargin=dp(10); slp.bottomMargin=dp(12); root.addView(search,slp);

        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.HORIZONTAL); hero.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable heroBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(11,32,51),Color.rgb(15,48,74),Color.rgb(8,18,26)});
        heroBg.setCornerRadius(dp(20)); hero.setBackground(heroBg); hero.setPadding(dp(16),dp(16),dp(16),dp(16));
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.ic_launcher); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        hero.addView(logo,new LinearLayout.LayoutParams(dp(62),dp(62)));
        LinearLayout heroText=new LinearLayout(this); heroText.setOrientation(LinearLayout.VERTICAL); heroText.setPadding(dp(12),0,0,0);
        heroText.addView(txt("DEXTERS",22,text,true)); heroText.addView(txt("TOOLS FOR A STRONGER TOMORROW",10,gold,true));
        hero.addView(heroText,new LinearLayout.LayoutParams(0,-2,1f));
        root.addView(hero,new LinearLayout.LayoutParams(-1,dp(116)));

        LinearLayout tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); tabs.setPadding(0,dp(12),0,dp(10));
        tab(tabs,"Featured",true); tab(tabs,"Business",false); tab(tabs,"Tools",false); tab(tabs,"Updates",false); root.addView(tabs);

        app(root,"Dexter AI","Your AI business assistant","AI",Color.rgb(117,56,215),v->startActivity(new Intent(this,MainActivity.class)));
        app(root,"Loyalty","Rewards and customer loyalty","★",Color.rgb(202,148,33),v->openWeb("https://app.dextersspot.co.uk"));
        app(root,"POS","Point of sale made simple","£",Color.rgb(15,152,143),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/"));
        app(root,"Back Office","Manage your business","BO",Color.rgb(9,163,171),v->openWeb("https://backoffice.dextersspot.co.uk/"));
        app(root,"Scanner","Scan and store documents","⌁",Color.rgb(70,117,145),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        app(root,"Website","Manage your online presence","◎",Color.rgb(112,48,224),v->openWeb("https://dextersspot.co.uk"));

        TextView section=txt("APP LIBRARY",11,gold,true); section.setPadding(0,dp(12),0,dp(8)); root.addView(section);
        packageApp(root,"WhatsApp Business","Messaging","com.whatsapp.w4b","WhatsApp Business");
        packageApp(root,"Gmail","Email","com.google.android.gm","Gmail");
        packageApp(root,"Google Maps","Navigation","com.google.android.apps.maps","Google Maps");
        packageApp(root,"Chrome","Web browser","com.android.chrome","Google Chrome");

        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(10),dp(8),dp(10),dp(8)); nav.setBackgroundColor(Color.rgb(10,14,16));
        navItem(nav,"⌂","Home",v->finish()); navItem(nav,"▦","Apps",v->{}); navItem(nav,"●","Updates",v->{}); navItem(nav,"○","Account",v->{});
        shell.addView(nav,new LinearLayout.LayoutParams(-1,dp(64)));
        setContentView(shell);
    }

    private void app(LinearLayout root,String name,String sub,String glyph,int color,android.view.View.OnClickListener open){
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(10),dp(10),dp(10),dp(10)); row.setBackground(round(card,17));
        TextView icon=txt(glyph,glyph.length()>1?13:22,Color.WHITE,true); icon.setGravity(Gravity.CENTER); icon.setBackground(round(color,13));
        row.addView(icon,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL); words.setPadding(dp(11),0,dp(8),0);
        words.addView(txt(name,14,text,true)); words.addView(txt(sub,11,muted,false)); row.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
        Button b=new Button(this); b.setAllCaps(false); b.setText("Open"); b.setTextColor(gold); b.setTextSize(12); b.setBackground(round(Color.rgb(31,32,32),18)); b.setOnClickListener(open);
        row.addView(b,new LinearLayout.LayoutParams(dp(76),dp(40)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(8)); root.addView(row,lp);
    }

    private void packageApp(LinearLayout root,String name,String sub,String pkg,String search){
        app(root,name,sub,name.substring(0,1),Color.rgb(60,75,85),v->{
            try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg); if(i!=null){startActivity(i);return;}}catch(Exception ignored){}
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q="+Uri.encode(search)+"&c=apps")));}catch(Exception e){openWeb("https://play.google.com/store/search?q="+Uri.encode(search)+"&c=apps");}
        });
    }

    private void tab(LinearLayout p,String name,boolean active){
        TextView t=txt(name,11,active?Color.rgb(22,18,9):text,true); t.setGravity(Gravity.CENTER); t.setBackground(round(active?gold:Color.rgb(26,31,34),18));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(34),1f); lp.setMargins(dp(3),0,dp(3),0); p.addView(t,lp);
    }
    private void navItem(LinearLayout p,String glyph,String label,android.view.View.OnClickListener click){
        LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setOnClickListener(click);
        x.addView(txt(glyph,17,gold,true));x.addView(txt(label,9,Color.rgb(220,220,222),false));p.addView(x,new LinearLayout.LayoutParams(0,-1,1f));
    }
    private void openWeb(String u){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}
    private TextView txt(String v,float s,int c,boolean b){TextView x=new TextView(this);x.setText(v);x.setTextSize(s);x.setTextColor(c);if(b)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));return x;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
