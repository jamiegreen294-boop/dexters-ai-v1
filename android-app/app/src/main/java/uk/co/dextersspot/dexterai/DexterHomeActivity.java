package uk.co.dextersspot.dexterai;

import android.app.Activity;
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
    private final int text=Color.WHITE, muted=Color.rgb(185,190,194), accent=Color.rgb(239,139,34);
    private ViewFlipper pages;
    private TextView dots;
    private GestureDetector gestures;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(7,11,11));
        getWindow().setNavigationBarColor(Color.rgb(7,11,11));
        gestures=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){
                if(a==null||b==null||pages==null)return false;
                float dx=b.getX()-a.getX(),dy=b.getY()-a.getY();
                if(Math.abs(dx)<dp(70)||Math.abs(dx)<=Math.abs(dy))return false;
                if(dx<0&&pages.getDisplayedChild()<pages.getChildCount()-1){pages.showNext();updateDots();return true;}
                if(dx>0&&pages.getDisplayedChild()>0){pages.showPrevious();updateDots();return true;}
                return false;
            }
        });
        render();
    }

    @Override public void onBackPressed(){}

    private void render(){
        FrameLayout shell=new FrameLayout(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(10),dp(18),dp(14));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(6,13,12),Color.rgb(17,28,24),Color.rgb(10,10,12)});
        root.setBackground(bg);

        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(4),dp(6),dp(4),dp(12));
        root.addView(hero,new LinearLayout.LayoutParams(-1,-2));

        TextClock clock=new TextClock(this);
        clock.setFormat24Hour("HH:mm"); clock.setFormat12Hour("HH:mm");
        clock.setTextColor(text); clock.setTextSize(52); clock.setGravity(Gravity.CENTER);
        clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        hero.addView(clock,new LinearLayout.LayoutParams(-1,-2));

        TextClock date=new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM"); date.setFormat12Hour("EEEE, d MMMM");
        date.setTextColor(muted); date.setTextSize(13); date.setGravity(Gravity.CENTER);
        hero.addView(date,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout brand=glassRow();
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        brand.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));

        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL); words.setPadding(dp(10),0,0,0);
        TextView t=label("DEXTER OS",15,text,true); words.addView(t);
        TextView s=label("Team Preview",10,muted,false); words.addView(s);
        brand.addView(words);
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-2,-2); blp.topMargin=dp(10);
        hero.addView(brand,blp);

        pages=new ViewFlipper(this);
        root.addView(pages,new LinearLayout.LayoutParams(-1,0,1f));

        GridLayout p1=page();
        dexterTile(p1,"Dexter AI","AI",accent,v->startActivity(new Intent(this,MainActivity.class)));
        dexterTile(p1,"Loyalty","★",Color.rgb(220,161,28),v->web("https://app.dextersspot.co.uk"));
        dexterTile(p1,"POS","£",Color.rgb(42,45,49),v->web("https://backoffice.dextersspot.co.uk/pc-pos-test/"));
        dexterTile(p1,"Back Office","BO",Color.rgb(103,83,183),v->web("https://backoffice.dextersspot.co.uk/"));
        dexterTile(p1,"Messages","✉",Color.rgb(43,126,110),v->startActivity(new Intent(this,DexterInboxActivity.class)));
        dexterTile(p1,"Dexter Store","D",accent,v->startActivity(new Intent(this,DexterStoreActivity.class)));
        packageTile(p1,"WhatsApp",new String[]{"com.whatsapp.w4b","com.whatsapp"});
        packageTile(p1,"Gmail",new String[]{"com.google.android.gm"});
        dexterTile(p1,"Internet","◎",Color.rgb(50,105,190),v->web("https://www.google.com/"));
        packageTile(p1,"Maps",new String[]{"com.google.android.apps.maps"});
        packageTile(p1,"Camera",new String[]{"com.android.camera2","com.android.camera"});
        packageTile(p1,"Files",new String[]{"com.google.android.apps.nbu.files","com.google.android.documentsui"});

        GridLayout p2=page();
        dexterTile(p2,"Control Centre","◉",Color.rgb(53,115,105),v->startActivity(new Intent(this,DexterControlCentreActivity.class)));
        dexterTile(p2,"Dexter Settings","⚙",Color.rgb(78,83,89),v->startActivity(new Intent(this,DexterSettingsActivity.class)));
        dexterTile(p2,"Scanner","⌁",Color.rgb(35,177,112),v->web("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        packageTile(p2,"Chrome",new String[]{"com.android.chrome"});
        packageTile(p2,"Calculator",new String[]{"com.google.android.calculator","com.android.calculator2"});
        dexterTile(p2,"Wi-Fi","⌁",Color.rgb(47,118,220),v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        dexterTile(p2,"Bluetooth","B",Color.rgb(63,103,232),v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        dexterTile(p2,"Website","D",Color.rgb(47,94,79),v->web("https://dextersspot.co.uk"));
        dexterTile(p2,"Business Apps","▦",Color.rgb(72,77,82),v->startActivity(new Intent(this,BusinessAppsActivity.class)));
        pages.addView(p1); pages.addView(p2);

        dots=label("●  ○",9,Color.argb(210,255,255,255),false);
        dots.setGravity(Gravity.CENTER); dots.setPadding(0,dp(4),0,dp(9));
        root.addView(dots,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout dock=glassRow();
        dock.setGravity(Gravity.CENTER); dock.setPadding(dp(9),dp(9),dp(9),dp(9));
        root.addView(dock,new LinearLayout.LayoutParams(-1,dp(78)));
        dockIcon(dock,"AI",accent,v->startActivity(new Intent(this,MainActivity.class)));
        dockIcon(dock,"✉",Color.rgb(43,126,110),v->startActivity(new Intent(this,DexterInboxActivity.class)));
        dockPackage(dock,new String[]{"com.whatsapp.w4b","com.whatsapp"},"W");
        dockIcon(dock,"⚙",Color.rgb(78,83,89),v->startActivity(new Intent(this,DexterSettingsActivity.class)));

        shell.addView(root,new FrameLayout.LayoutParams(-1,-1));
        setContentView(shell);
    }

    private GridLayout page(){
        GridLayout g=new GridLayout(this); g.setColumnCount(4); g.setRowCount(3); g.setPadding(0,dp(2),0,0); return g;
    }

    private void dexterTile(GridLayout grid,String name,String glyph,int color,View.OnClickListener click){
        LinearLayout box=tileShell();
        TextView icon=new TextView(this);
        icon.setText(glyph); icon.setTextColor(Color.WHITE); icon.setGravity(Gravity.CENTER);
        icon.setTextSize(glyph.length()>1?16:24); icon.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        icon.setBackground(round(color,20)); icon.setElevation(dp(8)); icon.setOnClickListener(click);
        box.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
        addName(box,name); grid.addView(box);
    }

    private void packageTile(GridLayout grid,String name,String[] pkgs){
        LinearLayout box=tileShell();
        ImageView icon=new ImageView(this); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); icon.setPadding(dp(3),dp(3),dp(3),dp(3));
        Drawable d=findIcon(pkgs); if(d!=null)icon.setImageDrawable(d); else icon.setImageResource(R.drawable.ic_launcher);
        icon.setElevation(dp(8)); icon.setOnClickListener(v->launch(pkgs,name));
        box.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64))); addName(box,name); grid.addView(box);
    }

    private LinearLayout tileShell(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL); box.setPadding(dp(2),dp(8),dp(2),dp(2));
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=0;
        lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); lp.rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); box.setLayoutParams(lp); return box;
    }

    private void addName(LinearLayout box,String value){
        TextView n=label(value,10.2f,Color.rgb(245,245,247),false); n.setGravity(Gravity.CENTER); n.setMaxLines(1); n.setPadding(0,dp(6),0,0);
        box.addView(n,new LinearLayout.LayoutParams(-1,-2));
    }

    private LinearLayout glassRow(){
        LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.HORIZONTAL); x.setPadding(dp(12),dp(8),dp(12),dp(8));
        x.setBackground(round(Color.argb(42,255,255,255),24)); x.setElevation(dp(3)); return x;
    }

    private TextView label(String v,float size,int color,boolean bold){
        TextView x=new TextView(this); x.setText(v); x.setTextSize(size); x.setTextColor(color);
        if(bold)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD)); return x;
    }

    private void dockIcon(LinearLayout dock,String glyph,int color,View.OnClickListener click){
        TextView i=new TextView(this); i.setText(glyph); i.setTextColor(Color.WHITE); i.setTextSize(glyph.length()>1?15:22); i.setGravity(Gravity.CENTER);
        i.setTypeface(Typeface.DEFAULT_BOLD); i.setBackground(round(color,19)); i.setOnClickListener(click);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(58),dp(58),1f); lp.gravity=Gravity.CENTER; dock.addView(i,lp);
    }

    private void dockPackage(LinearLayout dock,String[] pkgs,String fallback){
        ImageView i=new ImageView(this); i.setScaleType(ImageView.ScaleType.CENTER_INSIDE); i.setPadding(dp(3),dp(3),dp(3),dp(3));
        Drawable d=findIcon(pkgs); if(d!=null)i.setImageDrawable(d); else i.setImageResource(R.drawable.ic_launcher);
        i.setOnClickListener(v->launch(pkgs,fallback));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(58),dp(58),1f); lp.gravity=Gravity.CENTER; dock.addView(i,lp);
    }

    private Drawable findIcon(String[] pkgs){
        PackageManager pm=getPackageManager();
        for(String p:pkgs)try{return pm.getApplicationIcon(p);}catch(Exception ignored){}
        return null;
    }

    private void launch(String[] pkgs,String label){
        PackageManager pm=getPackageManager();
        for(String p:pkgs)try{Intent i=pm.getLaunchIntentForPackage(p); if(i!=null){startActivity(i);return;}}catch(Exception ignored){}
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q="+Uri.encode(label)+"&c=apps")));}catch(Exception e){web("https://play.google.com/store/search?q="+Uri.encode(label)+"&c=apps");}
    }

    private void web(String url){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
    private void updateDots(){if(dots!=null)dots.setText(pages.getDisplayedChild()==0?"●  ○":"○  ●");}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(gestures!=null)gestures.onTouchEvent(e);return super.dispatchTouchEvent(e);}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
