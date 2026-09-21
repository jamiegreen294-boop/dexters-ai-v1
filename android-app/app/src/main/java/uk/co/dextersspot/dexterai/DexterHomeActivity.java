package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.ViewFlipper;
import java.util.List;

public class DexterHomeActivity extends Activity {
    private final int text = Color.WHITE;
    private final int muted = Color.rgb(205,205,210);
    private ViewFlipper pages;
    private TextView pageDots;
    private float downX;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(13,18,17));
        getWindow().setNavigationBarColor(Color.rgb(13,18,17));
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (DeviceAgentService.hasToken(this)) DeviceAgentService.start(this);
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(12));

        GradientDrawable wallpaper = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(11,19,18), Color.rgb(22,29,26), Color.rgb(16,16,18)}
        );
        root.setBackground(wallpaper);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setGravity(Gravity.CENTER_HORIZONTAL);
        top.setPadding(dp(4), dp(6), dp(4), dp(10));
        root.addView(top, new LinearLayout.LayoutParams(-1,-2));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(text);
        clock.setTextSize(46);
        clock.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        clock.setGravity(Gravity.CENTER);
        top.addView(clock,new LinearLayout.LayoutParams(-1,-2));

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM");
        date.setFormat12Hour("EEEE, d MMMM");
        date.setTextColor(muted);
        date.setTextSize(14);
        date.setGravity(Gravity.CENTER);
        top.addView(date,new LinearLayout.LayoutParams(-1,-2));

        TextView badge = new TextView(this);
        badge.setText("DEXTERS");
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(10);
        badge.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(12),dp(5),dp(12),dp(5));
        badge.setBackground(round(Color.argb(150,255,255,255),14));
        LinearLayout.LayoutParams badgeLp=new LinearLayout.LayoutParams(-2,-2);
        badgeLp.topMargin=dp(7);
        top.addView(badge,badgeLp);

        pages = new ViewFlipper(this);
        root.addView(pages,new LinearLayout.LayoutParams(-1,0,1f));

        GridLayout p1=appPage();
        addWebTile(p1,"Dexter AI","D",Color.rgb(34,115,255),v->openWeb("https://jamiegreen294-boop.github.io/dexters-ai-v1/?android-app=1"));
        addWebTile(p1,"Loyalty","★",Color.rgb(237,177,42),v->openWeb("https://app.dextersspot.co.uk"));
        addWebTile(p1,"Scanner","⌁",Color.rgb(35,188,117),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        addWebTile(p1,"Back Office","B",Color.rgb(118,87,201),v->openWeb("https://backoffice.dextersspot.co.uk/"));
        addWebTile(p1,"POS","£",Color.rgb(35,35,39),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/"));
        addPackageTile(p1,"WhatsApp",new String[]{"com.whatsapp.w4b","com.whatsapp"},"WhatsApp");
        addPackageTile(p1,"Gmail",new String[]{"com.google.android.gm"},"Gmail");
        addPackageTile(p1,"Maps",new String[]{"com.google.android.apps.maps"},"Maps");
        addPackageTile(p1,"Chrome",new String[]{"com.android.chrome"},"Chrome");
        addPackageTile(p1,"Square",new String[]{"com.squareup","com.squareup.pos"},"Square");
        addWebTile(p1,"Website","D",Color.rgb(47,94,79),v->openWeb("https://dextersspot.co.uk"));
        addWebTile(p1,"Business","▦",Color.rgb(83,83,89),v->startActivity(new Intent(this,BusinessAppsActivity.class)));

        GridLayout p2=appPage();
        addPackageTile(p2,"Camera",new String[]{"com.android.camera","com.meizu.media.camera"},"Camera");
        addPackageTile(p2,"Calculator",new String[]{"com.google.android.calculator","com.android.calculator2"},"Calculator");
        addWebTile(p2,"Quick Start","i",Color.rgb(69,132,239),v->startActivity(new Intent(this,QuickStartActivity.class)));
        addWebTile(p2,"Wi-Fi","⌁",Color.rgb(57,133,246),v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        addWebTile(p2,"Bluetooth","ᛒ",Color.rgb(63,103,232),v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        addWebTile(p2,"Dexter Admin","⚙",Color.rgb(112,112,118),v->openAdmin());
        addPackageTile(p2,"Files",new String[]{"com.google.android.documentsui","com.android.documentsui"},"Files");
        addWebTile(p2,"bOnline","b",Color.rgb(45,121,212),v->launchPackageOrLabel(new String[]{},"bOnline"));

        pages.addView(p1); pages.addView(p2);
        pages.setOnTouchListener((v,e)->handleSwipe(e));

        pageDots=new TextView(this);
        pageDots.setText("●  ○");
        pageDots.setTextColor(Color.argb(210,255,255,255));
        pageDots.setTextSize(11);
        pageDots.setGravity(Gravity.CENTER);
        pageDots.setPadding(0,dp(4),0,dp(9));
        root.addView(pageDots,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout dock=new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(9),dp(9),dp(9),dp(9));
        dock.setElevation(dp(10));
        dock.setBackground(round(Color.argb(175,105,110,108),30));
        root.addView(dock,new LinearLayout.LayoutParams(-1,dp(84)));

        addDockWeb(dock,"D",Color.rgb(34,115,255),v->openWeb("https://jamiegreen294-boop.github.io/dexters-ai-v1/?android-app=1"));
        addDockWeb(dock,"⌁",Color.rgb(35,188,117),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        addDockPackage(dock,new String[]{"com.whatsapp.w4b","com.whatsapp"},"WhatsApp",Color.rgb(35,188,95),"W");
        addDockWeb(dock,"£",Color.rgb(35,35,39),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/"));

        setContentView(root);
    }

    private GridLayout appPage(){
        GridLayout g=new GridLayout(this);
        g.setColumnCount(4);
        g.setRowCount(4);
        g.setPadding(0,dp(3),0,0);
        return g;
    }

    private void addWebTile(GridLayout grid,String label,String glyph,int color,View.OnClickListener click){
        LinearLayout box=tileShell();
        TextView icon=new TextView(this);
        icon.setText(glyph); icon.setTextColor(Color.WHITE); icon.setTextSize(glyph.length()>1?19:25);
        icon.setGravity(Gravity.CENTER); icon.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        icon.setBackground(round(color,18)); icon.setElevation(dp(5)); icon.setOnClickListener(click);
        box.addView(icon,new LinearLayout.LayoutParams(dp(60),dp(60)));
        addName(box,label);
        grid.addView(box);
    }

    private void addPackageTile(GridLayout grid,String label,String[] packages,String fallbackLabel){
        LinearLayout box=tileShell();
        ImageView icon=new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setPadding(dp(4),dp(4),dp(4),dp(4));
        icon.setElevation(dp(5));
        Drawable d=findIcon(packages,fallbackLabel);
        if(d!=null) icon.setImageDrawable(d);
        else icon.setImageDrawable(letterDrawable(label.substring(0,1)));
        icon.setOnClickListener(v->launchPackageOrLabel(packages,fallbackLabel));
        box.addView(icon,new LinearLayout.LayoutParams(dp(60),dp(60)));
        addName(box,label);
        grid.addView(box);
    }

    private LinearLayout tileShell(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(2),dp(8),dp(2),dp(2));
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
        lp.width=0;lp.height=0;
        lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        lp.rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        box.setLayoutParams(lp);
        return box;
    }

    private void addName(LinearLayout box,String label){
        TextView name=new TextView(this);
        name.setText(label); name.setTextColor(Color.WHITE); name.setTextSize(10.5f);
        name.setGravity(Gravity.CENTER); name.setMaxLines(1);
        name.setShadowLayer(2,0,1,Color.BLACK);
        name.setPadding(0,dp(5),0,0);
        box.addView(name,new LinearLayout.LayoutParams(-1,-2));
    }

    private void addDockWeb(LinearLayout dock,String glyph,int color,View.OnClickListener click){
        TextView icon=new TextView(this);
        icon.setText(glyph);icon.setTextColor(Color.WHITE);icon.setTextSize(22);icon.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);icon.setBackground(round(color,18));icon.setElevation(dp(6));icon.setOnClickListener(click);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(60),dp(60));
        lp.weight=1;lp.gravity=Gravity.CENTER;
        dock.addView(icon,lp);
    }

    private void addDockPackage(LinearLayout dock,String[] pkgs,String label,int fallbackColor,String fallbackGlyph){
        ImageView icon=new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);icon.setPadding(dp(4),dp(4),dp(4),dp(4));icon.setElevation(dp(6));
        Drawable d=findIcon(pkgs,label);
        if(d!=null)icon.setImageDrawable(d); else icon.setImageDrawable(letterDrawable(fallbackGlyph));
        icon.setOnClickListener(v->launchPackageOrLabel(pkgs,label));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(60),dp(60));lp.weight=1;lp.gravity=Gravity.CENTER;
        dock.addView(icon,lp);
    }

    private Drawable findIcon(String[] packages,String label){
        PackageManager pm=getPackageManager();
        for(String p:packages){
            try{return pm.getApplicationIcon(p);}catch(Exception ignored){}
        }
        try{
            for(ApplicationInfo ai:pm.getInstalledApplications(0)){
                String n=String.valueOf(pm.getApplicationLabel(ai));
                if(n.toLowerCase().contains(label.toLowerCase()))return pm.getApplicationIcon(ai);
            }
        }catch(Exception ignored){}
        return null;
    }

    private Drawable letterDrawable(String glyph){
        GradientDrawable d=round(Color.rgb(90,90,95),18);
        return d;
    }

    private boolean handleSwipe(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();return false;}
        if(e.getAction()==MotionEvent.ACTION_UP){
            float dx=e.getX()-downX;
            if(Math.abs(dx)>dp(65)){
                if(dx<0&&pages.getDisplayedChild()<pages.getChildCount()-1){
                    pages.setInAnimation(this,android.R.anim.fade_in);pages.setOutAnimation(this,android.R.anim.fade_out);pages.showNext();
                }else if(dx>0&&pages.getDisplayedChild()>0){
                    pages.setInAnimation(this,android.R.anim.fade_in);pages.setOutAnimation(this,android.R.anim.fade_out);pages.showPrevious();
                }
                updateDots();
            }
            return false;
        }
        return false;
    }

    private void updateDots(){if(pageDots!=null)pageDots.setText(pages.getDisplayedChild()==0?"●  ○":"○  ●");}

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
