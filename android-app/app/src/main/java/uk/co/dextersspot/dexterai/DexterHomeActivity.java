package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import java.util.List;

public class DexterHomeActivity extends Activity {
    private final int bg = Color.rgb(19,18,17);
    private final int card = Color.rgb(42,40,38);
    private final int text = Color.WHITE;
    private final int muted = Color.rgb(190,187,181);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (DeviceAgentService.hasToken(this)) DeviceAgentService.start(this);
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(22));
        root.setBackgroundColor(bg);
        scroll.addView(root);

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(text);
        clock.setTextSize(42);
        clock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(clock);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setTextColor(muted);
        date.setTextSize(17);
        root.addView(date);

        TextView title = new TextView(this);
        title.setText("Dexter Phone");
        title.setTextColor(text);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(22), 0, dp(12));
        root.addView(title);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        root.addView(grid);

        addTile(grid,"Dexter AI","AI",v->openWeb("https://jamiegreen294-boop.github.io/dexters-ai-v1/?android-app=1"));
        addTile(grid,"Loyalty","★",v->openWeb("https://app.dextersspot.co.uk"));
        addTile(grid,"Loyalty Scan","QR",v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        addTile(grid,"Back Office","BO",v->openWeb("https://backoffice.dextersspot.co.uk/"));
        addTile(grid,"POS","POS",v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/"));
        addTile(grid,"WhatsApp","WA",v->launchPackageOrLabel(new String[]{"com.whatsapp.w4b","com.whatsapp"},"WhatsApp"));
        addTile(grid,"bOnline","bO",v->launchPackageOrLabel(new String[]{},"bOnline"));
        addTile(grid,"Camera","CAM",v->startActivity(new Intent("android.media.action.IMAGE_CAPTURE")));
        addTile(grid,"Calculator","123",v->launchPackageOrLabel(new String[]{"com.google.android.calculator","com.android.calculator2"},"Calculator"));
        addTile(grid,"Website","WEB",v->openWeb("https://dextersspot.co.uk"));
        addTile(grid,"Business Apps","APP",v->startActivity(new Intent(this,BusinessAppsActivity.class)));
        addTile(grid,"Quick Start","i",v->startActivity(new Intent(this,QuickStartActivity.class)));
        addTile(grid,"Settings","⚙",v->openAdmin());

        TextView section = new TextView(this);
        section.setText("Dexter Control Centre");
        section.setTextColor(text);
        section.setTextSize(18);
        section.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        section.setPadding(0, dp(22), 0, dp(8));
        root.addView(section);

        LinearLayout control = new LinearLayout(this);
        control.setOrientation(LinearLayout.HORIZONTAL);
        control.setGravity(Gravity.CENTER);
        control.setPadding(dp(8),dp(8),dp(8),dp(8));
        control.setBackground(round(card, 24));
        root.addView(control);

        addControl(control,"Wi-Fi",v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        addControl(control,"Bluetooth",v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        addControl(control,"Dexter Admin",v->openAdmin());

        TextView footer = new TextView(this);
        footer.setText("Dexters Business Phone · managed by Dexter AI");
        footer.setTextColor(muted);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0,dp(24),0,dp(12));
        root.addView(footer);

        setContentView(scroll);
    }

    private void addTile(GridLayout grid,String label,String glyph,View.OnClickListener click){
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4),dp(8),dp(4),dp(8));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width=0; lp.height=dp(112); lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        box.setLayoutParams(lp);

        Button b = new Button(this);
        b.setText(glyph);
        b.setTextSize(15);
        b.setTextColor(text);
        b.setAllCaps(false);
        b.setBackground(round(card, 22));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(62),dp(62));
        b.setLayoutParams(bp);
        b.setOnClickListener(click);
        box.addView(b);

        TextView t=new TextView(this);
        t.setText(label); t.setTextColor(text); t.setTextSize(11); t.setGravity(Gravity.CENTER);
        t.setPadding(0,dp(4),0,0);
        box.addView(t);
        grid.addView(box);
    }

    private void addControl(LinearLayout host,String label,View.OnClickListener click){
        Button b=new Button(this);
        b.setText(label); b.setTextColor(text); b.setTextSize(12); b.setAllCaps(false);
        b.setBackground(round(Color.rgb(58,56,53),18));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1f);
        lp.setMargins(dp(4),0,dp(4),0); b.setLayoutParams(lp); b.setOnClickListener(click); host.addView(b);
    }

    private void openAdmin(){
        KeyguardManager km=(KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        if(km!=null && km.isDeviceSecure()){
            Intent confirm=km.createConfirmDeviceCredentialIntent("Dexter Owner","Confirm device unlock to open Dexter administration.");
            if(confirm!=null){ startActivityForResult(confirm,294); return; }
        }
        startActivity(new Intent(this,MainActivity.class));
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==294 && resultCode==RESULT_OK) startActivity(new Intent(this,MainActivity.class));
    }

    private void openWeb(String url){
        Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }

    private void launchPackageOrLabel(String[] packages,String label){
        PackageManager pm=getPackageManager();
        for(String p:packages){
            try{
                Intent i=pm.getLaunchIntentForPackage(p);
                if(i!=null){startActivity(i);return;}
            }catch(Exception ignored){}
        }
        try{
            List<ApplicationInfo> apps=pm.getInstalledApplications(0);
            for(ApplicationInfo ai:apps){
                String appLabel=String.valueOf(pm.getApplicationLabel(ai));
                if(appLabel.toLowerCase().contains(label.toLowerCase())){
                    Intent i=pm.getLaunchIntentForPackage(ai.packageName);
                    if(i!=null){startActivity(i);return;}
                }
            }
        }catch(Exception ignored){}
    }

    private GradientDrawable round(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
