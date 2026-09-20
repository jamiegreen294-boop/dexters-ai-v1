package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class BusinessAppsActivity extends Activity {
    private final int bg=Color.rgb(19,18,17), card=Color.rgb(42,40,38), text=Color.WHITE, muted=Color.rgb(190,187,181);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        render();
    }

    private void render(){
        ScrollView s=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(20),dp(18),dp(30)); root.setBackgroundColor(bg);
        s.addView(root);

        TextView t=new TextView(this); t.setText("Business Apps"); t.setTextColor(text); t.setTextSize(28); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(t);
        TextView sub=new TextView(this); sub.setText("Dexter checks the core business apps. Missing Play Store apps need one owner-approved install/sign-in on this phone."); sub.setTextColor(muted); sub.setPadding(0,dp(8),0,dp(16)); root.addView(sub);

        add(root,"Gmail","com.google.android.gm","Gmail");
        add(root,"WhatsApp Business","com.whatsapp.w4b","WhatsApp Business");
        add(root,"Square","com.squareup","Square Point of Sale");
        add(root,"Google Chrome","com.android.chrome","Google Chrome");
        add(root,"Google Maps","com.google.android.apps.maps","Google Maps");
        addSearch(root,"bOnline","bOnline");
        addSearch(root,"Just Eat","Just Eat partner");
        addSearch(root,"Uber Eats","Uber Eats Orders");
        addSearch(root,"Foodhub","Foodhub");
        addWeb(root,"Dexter Loyalty","https://app.dextersspot.co.uk");
        addWeb(root,"Dexter Back Office","https://backoffice.dextersspot.co.uk/");
        addWeb(root,"Dexter POS","https://backoffice.dextersspot.co.uk/pc-pos-test/");
        addWeb(root,"Backup Loyalty Scanner","https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/");
        setContentView(s);
    }

    private void add(LinearLayout root,String label,String pkg,String search){
        boolean installed=isInstalled(pkg);
        Button b=new Button(this);
        b.setAllCaps(false); b.setText(label+" · "+(installed?"Installed":"Install"));
        b.setTextColor(text); b.setTextSize(15); b.setGravity(Gravity.CENTER_VERTICAL);
        b.setBackgroundColor(card);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58)); lp.setMargins(0,0,0,dp(8)); b.setLayoutParams(lp);
        b.setOnClickListener(v->{
            if(installed){
                Intent i=getPackageManager().getLaunchIntentForPackage(pkg); if(i!=null)startActivity(i);
            }else openMarket(pkg,search);
        });
        root.addView(b);
    }

    private void addSearch(LinearLayout root,String label,String query){
        Button b=new Button(this); b.setAllCaps(false); b.setText(label+" · Open / Install"); b.setTextColor(text); b.setBackgroundColor(card);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58)); lp.setMargins(0,0,0,dp(8)); b.setLayoutParams(lp);
        b.setOnClickListener(v->{
            try{ startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q="+Uri.encode(query)+"&c=apps"))); }
            catch(Exception e){ startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/search?q="+Uri.encode(query)+"&c=apps"))); }
        }); root.addView(b);
    }

    private void addWeb(LinearLayout root,String label,String url){
        Button b=new Button(this); b.setAllCaps(false); b.setText(label); b.setTextColor(text); b.setBackgroundColor(card);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58)); lp.setMargins(0,0,0,dp(8)); b.setLayoutParams(lp);
        b.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)))); root.addView(b);
    }

    private boolean isInstalled(String pkg){
        try{ getPackageManager().getPackageInfo(pkg,0); return true; }catch(Exception e){ return false; }
    }

    private void openMarket(String pkg,String search){
        try{ startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+pkg))); }
        catch(Exception e){ startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id="+pkg))); }
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
