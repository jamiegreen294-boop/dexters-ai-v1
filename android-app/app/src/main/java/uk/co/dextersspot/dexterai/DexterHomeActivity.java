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
import android.view.GestureDetector;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
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
    private GestureDetector gestureDetector;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(13,18,17));
        getWindow().setNavigationBarColor(Color.rgb(13,18,17));
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null || pages == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) < dp(70) || Math.abs(dx) <= Math.abs(dy)) return false;
                if (dx < 0 && pages.getDisplayedChild() < pages.getChildCount() - 1) {
                    pages.setInAnimation(DexterHomeActivity.this, android.R.anim.fade_in);
                    pages.setOutAnimation(DexterHomeActivity.this, android.R.anim.fade_out);
                    pages.showNext();
                    updateDots();
                    return true;
                }
                if (dx > 0 && pages.getDisplayedChild() > 0) {
                    pages.setInAnimation(DexterHomeActivity.this, android.R.anim.fade_in);
                    pages.setOutAnimation(DexterHomeActivity.this, android.R.anim.fade_out);
                    pages.showPrevious();
                    updateDots();
                    return true;
                }
                return false;
            }
        });
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (DeviceAgentService.hasToken(this)) DeviceAgentService.start(this);
    }

    @Override public void onBackPressed() {
        // Dexter Home is the device home screen. Back should never dismiss,
        // slide, recreate or refresh the launcher.
    }

    private void render() {
        FrameLayout shell = new FrameLayout(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(14));

        GradientDrawable wallpaper = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(7,14,13), Color.rgb(18,27,24), Color.rgb(12,13,15)}
        );
        root.setBackground(wallpaper);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setGravity(Gravity.CENTER_HORIZONTAL);
        top.setPadding(dp(4), dp(10), dp(4), dp(14));
        root.addView(top, new LinearLayout.LayoutParams(-1,-2));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(text);
        clock.setTextSize(50);
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

        LinearLayout brandCard=new LinearLayout(this);
        brandCard.setOrientation(LinearLayout.HORIZONTAL);
        brandCard.setGravity(Gravity.CENTER_VERTICAL);
        brandCard.setPadding(dp(12),dp(8),dp(14),dp(8));
        brandCard.setBackground(round(Color.argb(36,255,255,255),22));
        brandCard.setElevation(dp(3));

        ImageView brandLogo = new ImageView(this);
        brandLogo.setImageResource(uk.co.dextersspot.dexterai.R.drawable.ic_launcher);
        brandLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        brandCard.addView(brandLogo,new LinearLayout.LayoutParams(dp(38),dp(38)));

        LinearLayout brandWords=new LinearLayout(this);
        brandWords.setOrientation(LinearLayout.VERTICAL);
        brandWords.setPadding(dp(9),0,0,0);

        TextView brandTitle=new TextView(this);
        brandTitle.setText("DEXTERS");
        brandTitle.setTextColor(Color.WHITE);
        brandTitle.setTextSize(14);
        brandTitle.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        brandWords.addView(brandTitle,new LinearLayout.LayoutParams(-2,-2));

        TextView brandSub=new TextView(this);
        brandSub.setText("Business Phone");
        brandSub.setTextColor(Color.rgb(196,199,202));
        brandSub.setTextSize(10);
        brandWords.addView(brandSub,new LinearLayout.LayoutParams(-2,-2));

        brandCard.addView(brandWords,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams brandLp=new LinearLayout.LayoutParams(-2,-2);
        brandLp.topMargin=dp(10);
        top.addView(brandCard,brandLp);

        pages = new ViewFlipper(this);
        root.addView(pages,new LinearLayout.LayoutParams(-1,0,1f));

        GridLayout p1=appPage();
        addPremiumTile(p1,"Dexter AI",R.drawable.ic_dexter_ai,Color.rgb(239,139,34),v->startActivity(new Intent(this,MainActivity.class)));
        addPremiumTile(p1,"Loyalty",R.drawable.ic_loyalty,Color.rgb(227,166,30),v->openWeb("https://app.dextersspot.co.uk"));
        addPremiumTile(p1,"POS",R.drawable.ic_pos,Color.rgb(39,42,46),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/"));
        addPremiumTile(p1,"Back Office",R.drawable.ic_backoffice,Color.rgb(103,83,183),v->openWeb("https://backoffice.dextersspot.co.uk/"));
        addPremiumTile(p1,"Scanner",R.drawable.ic_scanner,Color.rgb(35,188,117),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        addPackageTile(p1,"WhatsApp",new String[]{"com.whatsapp.w4b","com.whatsapp"},"WhatsApp");
        addPackageTile(p1,"Gmail",new String[]{"com.google.android.gm"},"Gmail");
        addPackageTile(p1,"Maps",new String[]{"com.google.android.apps.maps"},"Maps");
        addPremiumTile(p1,"Internet",R.drawable.ic_internet,Color.rgb(58,116,201),v->openWeb("https://www.google.com/"));
        addPackageTile(p1,"Square",new String[]{"com.squareup","com.squareup.pos"},"Square");
        addPremiumTile(p1,"Website",R.drawable.ic_website,Color.rgb(47,94,79),v->openWeb("https://dextersspot.co.uk"));
        addPremiumTile(p1,"Business",R.drawable.ic_business,Color.rgb(71,76,80),v->startActivity(new Intent(this,BusinessAppsActivity.class)));

        GridLayout p2=appPage();
        addPackageTile(p2,"Camera",new String[]{"com.android.camera2","com.android.camera","com.meizu.media.camera"},"Camera");
        addPackageTile(p2,"Calculator",new String[]{"com.google.android.calculator","com.android.calculator2"},"Calculator");
        addWebTile(p2,"Quick Start","i",Color.rgb(69,132,239),v->startActivity(new Intent(this,QuickStartActivity.class)));
        addWebTile(p2,"Wi-Fi","⌁",Color.rgb(57,133,246),v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        addWebTile(p2,"Bluetooth","ᛒ",Color.rgb(63,103,232),v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        addWebTile(p2,"Dexter Admin","⚙",Color.rgb(92,97,104),v->openAdmin());
        addPackageTile(p2,"Files",new String[]{"com.google.android.documentsui","com.android.documentsui"},"Files");
        addPackageTile(p2,"Chrome",new String[]{"com.android.chrome"},"Chrome");
        addWebTile(p2,"bOnline","b",Color.rgb(45,121,212),v->launchPackageOrLabel(new String[]{},"bOnline"));

        pages.addView(p1); pages.addView(p2);

        pageDots=new TextView(this);
        pageDots.setText("●  ○");
        pageDots.setTextColor(Color.argb(210,255,255,255));
        pageDots.setTextSize(9);
        pageDots.setGravity(Gravity.CENTER);
        pageDots.setPadding(0,dp(3),0,dp(10));
        root.addView(pageDots,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout dock=new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(10),dp(10),dp(10),dp(10));
        dock.setElevation(dp(12));
        dock.setBackground(round(Color.argb(46,255,255,255),30));
        root.addView(dock,new LinearLayout.LayoutParams(-1,dp(86)));

        addDockWeb(dock,"D",Color.rgb(239,139,34),v->startActivity(new Intent(this,MainActivity.class)));
        addDockWeb(dock,"⌁",Color.rgb(35,188,117),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        addDockPackage(dock,new String[]{"com.whatsapp.w4b","com.whatsapp"},"WhatsApp",Color.rgb(35,188,95),"W");
        addDockWeb(dock,"£",Color.rgb(35,35,39),v->openWeb("https://backoffice.dextersspot.co.uk/pc-pos-test/"));

        shell.addView(root,new FrameLayout.LayoutParams(-1,-1));
        setContentView(shell);
        showStartupBrand(shell);
    }

    private void showStartupBrand(FrameLayout shell){
        if(getIntent()!=null && getIntent().getBooleanExtra("skipDexterSplash",false)) return;
        LinearLayout splash=new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(Color.rgb(9,9,9));
        splash.setAlpha(1f);

        ImageView logo=new ImageView(this);
        logo.setImageResource(uk.co.dextersspot.dexterai.R.drawable.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        splash.addView(logo,new LinearLayout.LayoutParams(dp(144),dp(144)));

        TextView brand=new TextView(this);
        brand.setText("DEXTERS");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(28);
        brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        brand.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams brandLp=new LinearLayout.LayoutParams(-1,-2);
        brandLp.topMargin=dp(18);
        splash.addView(brand,brandLp);

        TextView sub=new TextView(this);
        sub.setText("Dexter Business Phone");
        sub.setTextColor(Color.rgb(190,190,195));
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp=new LinearLayout.LayoutParams(-1,-2);
        subLp.topMargin=dp(4);
        splash.addView(sub,subLp);

        shell.addView(splash,new FrameLayout.LayoutParams(-1,-1));
        splash.bringToFront();
        splash.postDelayed(()->splash.animate().alpha(0f).setDuration(320).withEndAction(()->shell.removeView(splash)).start(),1500);
    }

    private GridLayout appPage(){
        GridLayout g=new GridLayout(this);
        g.setColumnCount(4);
        g.setRowCount(4);
        g.setPadding(0,dp(3),0,0);
        return g;
    }

    private void addPremiumTile(GridLayout grid,String label,int drawableRes,int color,View.OnClickListener click){
        LinearLayout box=tileShell();
        FrameLayout iconShell=new FrameLayout(this);
        iconShell.setBackground(round(color,20));
        iconShell.setElevation(dp(8));
        iconShell.setOnClickListener(click);

        ImageView icon=new ImageView(this);
        icon.setImageResource(drawableRes);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setPadding(dp(15),dp(15),dp(15),dp(15));
        FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(-1,-1);
        ip.gravity=Gravity.CENTER;
        iconShell.addView(icon,ip);

        box.addView(iconShell,new LinearLayout.LayoutParams(dp(64),dp(64)));
        addName(box,label);
        grid.addView(box);
    }

    private void addWebTile(GridLayout grid,String label,String glyph,int color,View.OnClickListener click){
        LinearLayout box=tileShell();
        TextView icon=new TextView(this);
        icon.setText(glyph); icon.setTextColor(Color.WHITE); icon.setTextSize(glyph.length()>2?13:glyph.length()>1?17:25);
        icon.setGravity(Gravity.CENTER); icon.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        icon.setBackground(round(color,20)); icon.setElevation(dp(7)); icon.setOnClickListener(click);
        box.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
        addName(box,label);
        grid.addView(box);
    }

    private void addPackageTile(GridLayout grid,String label,String[] packages,String fallbackLabel){
        LinearLayout box=tileShell();
        ImageView icon=new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setPadding(dp(3),dp(3),dp(3),dp(3));
        icon.setElevation(dp(7));
        Drawable d=findIcon(packages,fallbackLabel);
        if(d!=null) icon.setImageDrawable(d);
        else icon.setImageDrawable(letterDrawable(label.substring(0,1)));
        icon.setOnClickListener(v->launchPackageOrLabel(packages,fallbackLabel));
        box.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
        addName(box,label);
        grid.addView(box);
    }

    private LinearLayout tileShell(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(2),dp(7),dp(2),dp(3));
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
        lp.width=0;lp.height=0;
        lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        lp.rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
        box.setLayoutParams(lp);
        return box;
    }

    private void addName(LinearLayout box,String label){
        TextView name=new TextView(this);
        name.setText(label); name.setTextColor(Color.rgb(245,245,247)); name.setTextSize(10.2f);
        name.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        name.setGravity(Gravity.CENTER); name.setMaxLines(1);
        name.setShadowLayer(2.4f,0,1,Color.BLACK);
        name.setPadding(0,dp(6),0,0);
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

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
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
        try{
            Intent market=new Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q="+Uri.encode(label)+"&c=apps"));
            startActivity(market);
        }catch(Exception ignored){
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/search?q="+Uri.encode(label)+"&c=apps")));}catch(Exception ignoredToo){}
        }
    }

    private GradientDrawable round(int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
