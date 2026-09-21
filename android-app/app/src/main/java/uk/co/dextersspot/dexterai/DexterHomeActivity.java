package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class DexterHomeActivity extends Activity {
    private final int bg=Color.rgb(6,10,11), text=Color.WHITE, muted=Color.rgb(176,181,185), gold=Color.rgb(218,170,78);
    private ViewFlipper pages;
    private TextView dots;
    private GestureDetector gestures;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        gestures=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onFling(MotionEvent a,MotionEvent b,float vx,float vy){
                if(a==null||b==null||pages==null)return false;
                float dx=b.getX()-a.getX(), dy=b.getY()-a.getY();
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
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15),dp(8),dp(15),dp(12));
        GradientDrawable backdrop=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(4,9,10),Color.rgb(7,18,22),Color.rgb(7,10,11)});
        root.setBackground(backdrop);

        LinearLayout brand=new LinearLayout(this); brand.setGravity(Gravity.CENTER); brand.setPadding(0,dp(2),0,dp(8));
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.ic_launcher); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        brand.addView(logo,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout bw=new LinearLayout(this); bw.setOrientation(LinearLayout.VERTICAL); bw.setPadding(dp(8),0,0,0);
        bw.addView(txt("DEXTERS",17,text,true)); bw.addView(txt("B U S I N E S S   P H O N E",8.5f,gold,true)); brand.addView(bw);
        root.addView(brand,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(dp(15),dp(13),dp(15),dp(12));
        GradientDrawable heroBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(16,31,42),Color.rgb(23,47,64),Color.rgb(10,20,27)});
        heroBg.setCornerRadius(dp(21)); hero.setBackground(heroBg); hero.setElevation(dp(7));

        hero.addView(txt("Hello,",12,text,false));
        TextView line=txt("Let's make\nbusiness happen.",19,text,true); line.setLineSpacing(0,1.0f); hero.addView(line);

        LinearLayout weather=new LinearLayout(this); weather.setGravity(Gravity.CENTER_VERTICAL);
        TextView w=txt("☀  Glasgow\n12°C",10,Color.rgb(230,230,232),false); weather.addView(w,new LinearLayout.LayoutParams(0,-2,1f));
        TextClock date=new TextClock(this); date.setFormat24Hour("EEE d MMM"); date.setFormat12Hour("EEE d MMM"); date.setTextColor(text); date.setTextSize(12); date.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        weather.addView(date,new LinearLayout.LayoutParams(-2,-1)); hero.addView(weather,new LinearLayout.LayoutParams(-1,dp(44)));
        root.addView(hero,new LinearLayout.LayoutParams(-1,dp(150)));

        pages=new ViewFlipper(this);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,0,1f); pp.topMargin=dp(8); root.addView(pages,pp);

        GridLayout p1=page();
        dexterTile(p1,"Dexter AI","AI",Color.rgb(48,48,53),v->startActivity(new Intent(this,MainActivity.class)));
        dexterTile(p1,"Loyalty","♢",Color.rgb(210,155,47),v->web("https://app.dextersspot.co.uk"));
        dexterTile(p1,"POS","▤",Color.rgb(24,162,153),v->web("https://backoffice.dextersspot.co.uk/pc-pos-test/"));
        dexterTile(p1,"Back Office","♟",Color.rgb(37,111,193),v->web("https://backoffice.dextersspot.co.uk/"));
        dexterTile(p1,"Scanner","⌁",Color.rgb(72,91,102),v->web("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        dexterTile(p1,"Internet","◎",Color.rgb(30,112,211),v->web("https://www.google.com/"));
        dexterTile(p1,"Website","◉",Color.rgb(57,63,68),v->web("https://dextersspot.co.uk"));
        dexterTile(p1,"Business","▣",Color.rgb(63,92,108),v->startActivity(new Intent(this,BusinessAppsActivity.class)));
        dexterTile(p1,"Messages","●",Color.rgb(34,186,94),v->startActivity(new Intent(this,DexterInboxActivity.class)));
        packageTile(p1,"Phone",new String[]{"com.google.android.dialer","com.android.dialer","com.android.contacts"});
        packageTile(p1,"Email",new String[]{"com.google.android.gm"});
        dexterTile(p1,"Dexter Store","D",Color.rgb(205,139,36),v->startActivity(new Intent(this,DexterStoreActivity.class)));

        GridLayout p2=page();
        dexterTile(p2,"Control Centre","◉",Color.rgb(36,106,124),v->startActivity(new Intent(this,DexterControlCentreActivity.class)));
        dexterTile(p2,"Settings","⚙",Color.rgb(73,78,83),v->startActivity(new Intent(this,DexterSettingsActivity.class)));
        packageTile(p2,"Camera",new String[]{"com.android.camera2","com.android.camera","com.meizu.media.camera"});
        packageTile(p2,"Maps",new String[]{"com.google.android.apps.maps"});
        packageTile(p2,"Files",new String[]{"com.google.android.apps.nbu.files","com.google.android.documentsui","com.android.documentsui"});
        packageTile(p2,"Chrome",new String[]{"com.android.chrome"});
        packageTile(p2,"Calculator",new String[]{"com.google.android.calculator","com.android.calculator2"});
        dexterTile(p2,"Wi-Fi","⌁",Color.rgb(32,111,228),v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        dexterTile(p2,"Bluetooth","B",Color.rgb(52,104,226),v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        dexterTile(p2,"About Dexter OS","i",Color.rgb(112,82,42),v->startActivity(new Intent(this,DexterSettingsActivity.class)));
        pages.addView(p1); pages.addView(p2);

        dots=txt("●  ○",9,Color.argb(210,255,255,255),false); dots.setGravity(Gravity.CENTER); dots.setPadding(0,dp(2),0,dp(7)); root.addView(dots);

        LinearLayout dock=new LinearLayout(this); dock.setGravity(Gravity.CENTER); dock.setPadding(dp(9),dp(8),dp(9),dp(8)); dock.setBackground(round(Color.rgb(30,34,37),26)); dock.setElevation(dp(10));
        dockPackage(dock,new String[]{"com.google.android.dialer","com.android.dialer"},"☎",Color.rgb(34,186,94));
        dockIcon(dock,"●",Color.rgb(37,150,220),v->startActivity(new Intent(this,DexterInboxActivity.class)));
        dockPackage(dock,new String[]{"com.android.camera2","com.android.camera","com.meizu.media.camera"},"◉",Color.rgb(213,215,217));
        dockIcon(dock,"⚙",Color.rgb(83,87,91),v->startActivity(new Intent(this,DexterSettingsActivity.class)));
        root.addView(dock,new LinearLayout.LayoutParams(-1,dp(74)));

        setContentView(root);
    }

    private GridLayout page(){GridLayout g=new GridLayout(this); g.setColumnCount(4); g.setRowCount(3); return g;}

    private void dexterTile(GridLayout grid,String name,String glyph,int color,android.view.View.OnClickListener click){
        LinearLayout box=tileShell();
        TextView icon=txt(glyph,glyph.length()>1?14:21,Color.WHITE,true); icon.setGravity(Gravity.CENTER); icon.setBackground(round(color,14)); icon.setElevation(dp(5)); icon.setOnClickListener(click);
        box.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52))); addName(box,name); grid.addView(box);
    }

    private void packageTile(GridLayout grid,String name,String[] pkgs){
        LinearLayout box=tileShell();
        ImageView icon=new ImageView(this); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); icon.setPadding(dp(2),dp(2),dp(2),dp(2));
        Drawable d=findIcon(pkgs); if(d!=null)icon.setImageDrawable(d); else icon.setImageResource(R.drawable.ic_launcher);
        icon.setElevation(dp(5)); icon.setOnClickListener(v->launch(pkgs,name));
        box.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52))); addName(box,name); grid.addView(box);
    }

    private LinearLayout tileShell(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL); box.setPadding(dp(1),dp(6),dp(1),0);
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=0; lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); lp.rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); box.setLayoutParams(lp); return box;
    }

    private void addName(LinearLayout box,String name){TextView n=txt(name,9.2f,text,false); n.setGravity(Gravity.CENTER); n.setMaxLines(1); n.setPadding(0,dp(4),0,0); box.addView(n,new LinearLayout.LayoutParams(-1,-2));}

    private void dockIcon(LinearLayout dock,String glyph,int color,android.view.View.OnClickListener click){
        TextView i=txt(glyph,21,Color.WHITE,true); i.setGravity(Gravity.CENTER); i.setBackground(round(color,15)); i.setOnClickListener(click);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(52),dp(52),1f); lp.gravity=Gravity.CENTER; dock.addView(i,lp);
    }

    private void dockPackage(LinearLayout dock,String[] pkgs,String fallback,int color){
        Drawable d=findIcon(pkgs);
        if(d!=null){
            ImageView i=new ImageView(this); i.setImageDrawable(d); i.setScaleType(ImageView.ScaleType.CENTER_INSIDE); i.setPadding(dp(2),dp(2),dp(2),dp(2)); i.setOnClickListener(v->launch(pkgs,fallback));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(52),dp(52),1f); lp.gravity=Gravity.CENTER; dock.addView(i,lp);
        } else dockIcon(dock,fallback,color,v->launch(pkgs,fallback));
    }

    private Drawable findIcon(String[] pkgs){PackageManager pm=getPackageManager(); for(String p:pkgs)try{return pm.getApplicationIcon(p);}catch(Exception ignored){} return null;}

    private void launch(String[] pkgs,String label){
        PackageManager pm=getPackageManager();
        for(String p:pkgs)try{Intent i=pm.getLaunchIntentForPackage(p); if(i!=null){startActivity(i);return;}}catch(Exception ignored){}
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q="+Uri.encode(label)+"&c=apps")));}catch(Exception e){web("https://play.google.com/store/search?q="+Uri.encode(label)+"&c=apps");}
    }

    private void web(String u){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}
    private void updateDots(){dots.setText(pages.getDisplayedChild()==0?"●  ○":"○  ●");}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(gestures!=null)gestures.onTouchEvent(e);return super.dispatchTouchEvent(e);}
    private TextView txt(String v,float s,int c,boolean b){TextView x=new TextView(this);x.setText(v);x.setTextSize(s);x.setTextColor(c);if(b)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));return x;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
