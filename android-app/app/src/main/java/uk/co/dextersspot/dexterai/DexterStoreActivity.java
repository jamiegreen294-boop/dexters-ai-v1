package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DexterStoreActivity extends Activity {
    private final int bg=Color.rgb(10,14,13);
    private final int card=Color.rgb(28,33,31);
    private final int text=Color.WHITE;
    private final int muted=Color.rgb(185,191,188);
    private final int accent=Color.rgb(239,139,34);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        render();
    }

    private void render(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(28));
        root.setBackgroundColor(bg);
        scroll.addView(root);

        TextView title=new TextView(this);
        title.setText("Dexter Store");
        title.setTextColor(text);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        root.addView(title);

        TextView sub=new TextView(this);
        sub.setText("Approved apps for Dexter team phones. Installed apps open here; missing apps open their Google Play listing or search.");
        sub.setTextColor(muted);
        sub.setTextSize(14);
        sub.setPadding(0,dp(6),0,dp(18));
        root.addView(sub);

        section(root,"Core");
        app(root,"WhatsApp Business","com.whatsapp.w4b","WhatsApp Business");
        app(root,"Gmail","com.google.android.gm","Gmail");
        app(root,"Chrome","com.android.chrome","Google Chrome");
        app(root,"Google Maps","com.google.android.apps.maps","Google Maps");
        app(root,"Google Drive","com.google.android.apps.docs","Google Drive");
        app(root,"Google Photos","com.google.android.apps.photos","Google Photos");
        app(root,"Files by Google","com.google.android.apps.nbu.files","Files by Google");
        app(root,"Google Calendar","com.google.android.calendar","Google Calendar");

        section(root,"Payments");
        search(root,"Square Point of Sale","Square Point of Sale");

        section(root,"Delivery & Orders");
        search(root,"Just Eat Partner","Just Eat Partner");
        search(root,"Uber Eats Orders","Uber Eats Orders");
        search(root,"Foodhub","Foodhub");

        section(root,"Business");
        search(root,"bOnline","bOnline");
        search(root,"Microsoft Authenticator","Microsoft Authenticator");
        search(root,"Google Authenticator","Google Authenticator");

        section(root,"Dexter");
        web(root,"Dexter AI","https://jamiegreen294-boop.github.io/dexters-ai-v1/");
        web(root,"Dexter Loyalty","https://app.dextersspot.co.uk");
        web(root,"Dexter Back Office","https://backoffice.dextersspot.co.uk/");
        web(root,"Dexter POS","https://backoffice.dextersspot.co.uk/pc-pos-test/");

        setContentView(scroll);
    }

    private void section(LinearLayout root,String label){
        TextView h=new TextView(this);
        h.setText(label.toUpperCase());
        h.setTextColor(accent);
        h.setTextSize(12);
        h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        h.setPadding(0,dp(14),0,dp(8));
        root.addView(h);
    }

    private void app(LinearLayout root,String label,String pkg,String search){
        boolean installed=isInstalled(pkg);
        Button b=button(label+(installed?"  ·  OPEN":"  ·  INSTALL"));
        b.setOnClickListener(v->{
            if(installed){
                Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
                if(i!=null){ startActivity(i); return; }
            }
            openMarket(pkg,search);
        });
        root.addView(b,lp());
    }

    private void search(LinearLayout root,String label,String query){
        Button b=button(label+"  ·  VIEW");
        b.setOnClickListener(v->openSearch(query));
        root.addView(b,lp());
    }

    private void web(LinearLayout root,String label,String url){
        Button b=button(label+"  ·  OPEN");
        b.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))));
        root.addView(b,lp());
    }

    private Button button(String value){
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(value);
        b.setTextColor(text);
        b.setTextSize(15);
        b.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable d=new GradientDrawable();
        d.setColor(card);
        d.setCornerRadius(dp(16));
        b.setBackground(d);
        b.setPadding(dp(16),0,dp(16),0);
        return b;
    }

    private LinearLayout.LayoutParams lp(){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(60));
        lp.setMargins(0,0,0,dp(9));
        return lp;
    }

    private boolean isInstalled(String pkg){
        try{
            PackageInfo ignored=getPackageManager().getPackageInfo(pkg,0);
            return true;
        }catch(Exception e){ return false; }
    }

    private void openMarket(String pkg,String query){
        try{
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+pkg)));
        }catch(Exception e){
            openSearch(query);
        }
    }

    private void openSearch(String query){
        try{
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q="+Uri.encode(query)+"&c=apps")));
        }catch(Exception e){
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/search?q="+Uri.encode(query)+"&c=apps")));
        }
    }

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
