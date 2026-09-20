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
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.ViewFlipper;
import java.util.List;

public class DexterHomeActivity extends Activity {
    private final int bg = Color.rgb(18,18,20);
    private final int text = Color.WHITE;
    private final int muted = Color.rgb(190,190,195);
    private final int glass = Color.argb(205,55,55,60);
    private ViewFlipper pages;
    private TextView pageDots;
    private float downX;

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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(12));
        root.setBackgroundColor(bg);

        LinearLayout clockWrap = new LinearLayout(this);
        clockWrap.setOrientation(LinearLayout.VERTICAL);
        clockWrap.setGravity(Gravity.CENTER_HORIZONTAL);
        clockWrap.setPadding(0, dp(4), 0, dp(8));
        root.addView(clockWrap, new LinearLayout.LayoutParams(-1, -2));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(text);
        clock.setTextSize(44);
        clock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        clock.setGravity(Gravity.CENTER);
        clockWrap.addView(clock, new LinearLayout.LayoutParams(-1,-2));

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE d MMMM");
        date.setFormat12Hour("EEEE d MMMM");
        date.setTextColor(muted);
        date.setTextSize(15);
        date.setGravity(Gravity.CENTER);
        clockWrap.addView(date, new LinearLayout.LayoutParams(-1,-2));

        TextView managed = new TextView(this);
        managed.setText("Dexter Business Phone");
        managed.setTextColor(muted);
        managed.setTextSize(12);
        managed.setGravity(Gravity.CENTER);
        managed.setPadding(0,dp(3),0,0);
        clockWrap.addView(managed);

        pages = new ViewFlipper(this);
        root.addView(pages, new LinearLayout.LayoutParams(-1, 0, 1f));

        GridLayout page1 = appPage();
        addTile(page1,"Dexter AI","AI",Color.rgb(50,110,255),v->openWeb("https://jamiegreen294-boop.github.io/dexters-ai-v1/?android-app=1"));
        addTile(page1,"Loyalty","★",Color.rgb(245,180,35),v->openWeb("https://app.dextersspot.co.uk"));
        addTile(page1,"Scanner","QR",Color.rgb(40,180,120),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        addTile(page1,"Back Office","BO",Color.rgb(98,80,190),v->openWeb("https://backoffice.dextersspot.co.uk/"));
        addTile(page1,"POS","POS",Color.rgb(35,35,38),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/"));
        addTile(page1,"WhatsApp","WA",Color.rgb(35,180,90),v->launchPackageOrLabel(new String[]{"com.whatsapp.w4b","com.whatsapp"},"WhatsApp"));
        addTile(page1,"bOnline","bO",Color.rgb(40,120,210),v->launchPackageOrLabel(new String[]{},"bOnline"));
        addTile(page1,"Business Apps","APP",Color.rgb(100,100,110),v->startActivity(new Intent(this,BusinessAppsActivity.class)));
        addTile(page1,"Gmail","M",Color.rgb(220,65,55),v->launchPackageOrLabel(new String[]{"com.google.android.gm"},"Gmail"));
        addTile(page1,"Square","SQ",Color.rgb(15,15,15),v->launchPackageOrLabel(new String[]{"com.squareup"},"Square"));
        addTile(page1,"Maps","MAP",Color.rgb(60,145,235),v->launchPackageOrLabel(new String[]{"com.google.android.apps.maps"},"Maps"));
        addTile(page1,"Website","WEB",Color.rgb(70,70,75),v->openWeb("https://dextersspot.co.uk"));

        GridLayout page2 = appPage();
        addTile(page2,"Camera","CAM",Color.rgb(80,80,86),v->startActivity(new Intent("android.media.action.IMAGE_CAPTURE")));
        addTile(page2,"Calculator","123",Color.rgb(240,150,35),v->launchPackageOrLabel(new String[]{"com.google.android.calculator","com.android.calculator2"},"Calculator"));
        addTile(page2,"Quick Start","i",Color.rgb(65,130,230),v->startActivity(new Intent(this,QuickStartActivity.class)));
        addTile(page2,"Wi-Fi","WiFi",Color.rgb(55,130,245),v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        addTile(page2,"Bluetooth","BT",Color.rgb(40,100,230),v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        addTile(page2,"Dexter Admin","⚙",Color.rgb(105,105,110),v->openAdmin());
        addTile(page2,"Chrome","CHR",Color.rgb(230,85,60),v->launchPackageOrLabel(new String[]{"com.android.chrome"},"Chrome"));
        addTile(page2,"Files","FILE",Color.rgb(70,140,245),v->launchPackageOrLabel(new String[]{"com.google.android.documentsui","com.android.documentsui"},"Files"));

        pages.addView(page1);
        pages.addView(page2);
        pages.setOnTouchListener((v,e)->handleSwipe(e));

        pageDots = new TextView(this);
        pageDots.setText("●  ○");
        pageDots.setTextColor(muted);
        pageDots.setTextSize(12);
        pageDots.setGravity(Gravity.CENTER);
        pageDots.setPadding(0,dp(4),0,dp(8));
        root.addView(pageDots, new LinearLayout.LayoutParams(-1,-2));

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8),dp(8),dp(8),dp(8));
        dock.setBackground(round(glass, 28));
        root.addView(dock, new LinearLayout.LayoutParams(-1,dp(86)));

        addDock(dock,"AI","Dexter",Color.rgb(50,110,255),v->openWeb("https://jamiegreen294-boop.github.io/dexters-ai-v1/?android-app=1"));
        addDock(dock,"QR","Scan",Color.rgb(40,180,120),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        addDock(dock,"WA","WhatsApp",Color.rgb(35,180,90),v->launchPackageOrLabel(new String[]{"com.whatsapp.w4b","com.whatsapp"},"WhatsApp"));
        addDock(dock,"POS","POS",Color.rgb(35,35,38),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/"));

        setContentView(root);
    }

    private GridLayout appPage(){
        GridLayout g=new GridLayout(this);
        g.setColumnCount(4);
        g.setRowCount(5);
        g.setPadding(0,dp(4),0,0);
        return g;
    }

    private boolean handleSwipe(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){
            float dx=e.getX()-downX;
            if(Math.abs(dx)>dp(60)){
                if(dx<0 && pages.getDisplayedChild()<pages.getChildCount()-1)pages.showNext();
                else if(dx>0 && pages.getDisplayedChild()>0)pages.showPrevious();
                updateDots();
            }
            return true;
        }
        return true;
    }

    private void updateDots(){
        if(pageDots!=null)pageDots.setText(pages.getDisplayedChild()==0?"●  ○":"○  ●");
    }

    private void addTile(GridLayout grid,String label,String glyph,int iconColor,View.OnClickListener click){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(2),dp(8),dp(2),dp(2));
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
        lp.width=0;lp.height=0;
        lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        lp.rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        box.setLayoutParams(lp);

        Button icon=new Button(this);
        icon.setText(glyph);
        icon.setTextSize(glyph.length()>3?10:14);
        icon.setTextColor(Color.WHITE);
        icon.setAllCaps(false);
        icon.setGravity(Gravity.CENTER);
        icon.setPadding(0,0,0,0);
        icon.setBackground(round(iconColor, 17));
        icon.setOnClickListener(click);
        box.addView(icon,new LinearLayout.LayoutParams(dp(58),dp(58)));

        TextView name=new TextView(this);
        name.setText(label);
        name.setTextColor(text);
        name.setTextSize(10);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);
        name.setPadding(0,dp(4),0,0);
        box.addView(name,new LinearLayout.LayoutParams(-1,-2));
        grid.addView(box);
    }

    private void addDock(LinearLayout dock,String glyph,String label,int iconColor,View.OnClickListener click){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1f));

        Button icon=new Button(this);
        icon.setText(glyph);icon.setTextSize(13);icon.setTextColor(Color.WHITE);icon.setAllCaps(false);
        icon.setPadding(0,0,0,0);icon.setGravity(Gravity.CENTER);icon.setBackground(round(iconColor,17));icon.setOnClickListener(click);
        box.addView(icon,new LinearLayout.LayoutParams(dp(56),dp(56)));

        TextView name=new TextView(this);
        name.setText(label);name.setTextColor(Color.WHITE);name.setTextSize(9);name.setGravity(Gravity.CENTER);
        box.addView(name,new LinearLayout.LayoutParams(-1,-2));
        dock.addView(box);
    }

    private void openAdmin(){
        KeyguardManager km=(KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        if(km!=null&&km.isDeviceSecure()){
            Intent confirm=km.createConfirmDeviceCredentialIntent("Dexter Owner","Confirm device unlock to open Dexter administration.");
            if(confirm!=null){startActivityForResult(confirm,294);return;}
        }
        startActivity(new Intent(this,MainActivity.class));
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==294&&resultCode==RESULT_OK)startActivity(new Intent(this,MainActivity.class));
    }

    private void openWeb(String url){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}

    private void launchPackageOrLabel(String[] packages,String label){
        PackageManager pm=getPackageManager();
        for(String p:packages){
            try{Intent i=pm.getLaunchIntentForPackage(p);if(i!=null){startActivity(i);return;}}catch(Exception ignored){}
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
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
