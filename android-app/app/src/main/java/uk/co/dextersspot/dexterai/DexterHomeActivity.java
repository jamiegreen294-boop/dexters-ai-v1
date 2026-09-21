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
import android.widget.TextView;
import android.widget.ViewFlipper;

public class DexterHomeActivity extends Activity {
    private final int bg=Color.rgb(5,9,10), text=Color.WHITE, muted=Color.rgb(182,187,190), gold=Color.rgb(218,170,78);
    private ViewFlipper pages; private TextView dots; private GestureDetector gestures; private int currentPage=0;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        gestures=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onFling(MotionEvent a,MotionEvent z,float vx,float vy){
                if(a==null||z==null||pages==null)return false;
                float dx=z.getX()-a.getX(),dy=z.getY()-a.getY();
                if(Math.abs(dx)<dp(70)||Math.abs(dx)<=Math.abs(dy))return false;
                if(dx<0&&pages.getDisplayedChild()<pages.getChildCount()-1){pages.showNext();currentPage=pages.getDisplayedChild();updateDots();return true;}
                if(dx>0&&pages.getDisplayedChild()>0){pages.showPrevious();currentPage=pages.getDisplayedChild();updateDots();return true;}
                return false;
            }
        });
        render();
    }

    @Override protected void onResume(){super.onResume();if(pages!=null){pages.setDisplayedChild(currentPage);updateDots();}}
    @Override public void onBackPressed(){}

    private void render(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(8),dp(14),dp(11));
        GradientDrawable back=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(4,8,9),Color.rgb(7,17,20),Color.rgb(5,8,9)}); root.setBackground(back);

        LinearLayout brand=new LinearLayout(this); brand.setGravity(Gravity.CENTER); brand.setPadding(0,dp(2),0,dp(8));
        ImageView logo=new ImageView(this); logo.setImageResource(R.drawable.ic_launcher); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE); brand.addView(logo,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL); words.setPadding(dp(8),0,0,0);
        words.addView(txt("DEXTERS",18,text,true)); words.addView(txt("B U S I N E S S   P H O N E",8.5f,gold,true)); brand.addView(words);
        root.addView(brand,new LinearLayout.LayoutParams(-1,-2));

        DexterHeroView hero=new DexterHeroView(this); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(154)); hp.bottomMargin=dp(7); root.addView(hero,hp);

        pages=new ViewFlipper(this); LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,0,1f); root.addView(pages,pp);

        GridLayout p1=page();
        vectorTile(p1,"Dexter AI",R.drawable.ic_dexter_ai,Color.rgb(52,48,62),v->startActivity(new Intent(this,MainActivity.class)));
        vectorTile(p1,"Loyalty",R.drawable.ic_loyalty,Color.rgb(206,151,45),v->web("https://app.dextersspot.co.uk"));
        vectorTile(p1,"POS",R.drawable.ic_pos,Color.rgb(24,156,147),v->web("https://backoffice.dextersspot.co.uk/pc-pos-test/"));
        vectorTile(p1,"Back Office",R.drawable.ic_backoffice,Color.rgb(33,103,178),v->web("https://backoffice.dextersspot.co.uk/"));
        vectorTile(p1,"Scanner",R.drawable.ic_scanner,Color.rgb(75,90,101),v->web("https://backoffice.dextersspot.co.uk/pc-pos-test/scanner/"));
        vectorTile(p1,"Internet",R.drawable.ic_internet,Color.rgb(31,108,205),v->web("https://www.google.com/"));
        vectorTile(p1,"Website",R.drawable.ic_website,Color.rgb(56,60,64),v->web("https://dextersspot.co.uk"));
        vectorTile(p1,"Business",R.drawable.ic_business,Color.rgb(62,88,104),v->startActivity(new Intent(this,BusinessAppsActivity.class)));
        dexterTile(p1,"Messages","●",Color.rgb(35,184,94),v->startActivity(new Intent(this,DexterInboxActivity.class)));
        packageTile(p1,"Phone",new String[]{"com.google.android.dialer","com.android.dialer","com.android.contacts"});
        packageTile(p1,"Email",new String[]{"com.google.android.gm"});
        dexterTile(p1,"Dexter Store","D",Color.rgb(202,139,39),v->startActivity(new Intent(this,DexterStoreActivity.class)));

        GridLayout p2=page();
        dexterTile(p2,"Control Centre","◉",Color.rgb(34,101,121),v->startActivity(new Intent(this,DexterControlCentreActivity.class)));
        dexterTile(p2,"Settings","⚙",Color.rgb(71,75,80),v->startActivity(new Intent(this,DexterSettingsActivity.class)));
        packageTile(p2,"Camera",new String[]{"com.android.camera2","com.android.camera","com.meizu.media.camera"});
        packageTile(p2,"Maps",new String[]{"com.google.android.apps.maps"});
        packageTile(p2,"Files",new String[]{"com.google.android.apps.nbu.files","com.google.android.documentsui","com.android.documentsui"});
        packageTile(p2,"Chrome",new String[]{"com.android.chrome"});
        packageTile(p2,"Calculator",new String[]{"com.google.android.calculator","com.android.calculator2"});
        dexterTile(p2,"Wi-Fi","⌁",Color.rgb(32,111,228),v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        dexterTile(p2,"Bluetooth","B",Color.rgb(52,104,226),v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        dexterTile(p2,"About","i",Color.rgb(112,82,42),v->startActivity(new Intent(this,DexterSettingsActivity.class)));
        pages.addView(p1); pages.addView(p2); pages.setDisplayedChild(currentPage);

        dots=txt("●  ○",9,Color.argb(220,255,255,255),false); dots.setGravity(Gravity.CENTER); dots.setPadding(0,dp(2),0,dp(7)); root.addView(dots);

        LinearLayout dock=new LinearLayout(this); dock.setGravity(Gravity.CENTER); dock.setPadding(dp(10),dp(9),dp(10),dp(9));
        GradientDrawable dockBg=round(Color.argb(235,27,31,34),26); dockBg.setStroke(dp(1),Color.argb(45,255,255,255)); dock.setBackground(dockBg); dock.setElevation(dp(10));
        dockPackage(dock,new String[]{"com.google.android.dialer","com.android.dialer"},"☎",Color.rgb(35,184,94));
        dockIcon(dock,"●",Color.rgb(37,149,220),v->startActivity(new Intent(this,DexterInboxActivity.class)));
        dockPackage(dock,new String[]{"com.android.camera2","com.android.camera","com.meizu.media.camera"},"◉",Color.rgb(210,212,214));
        dockIcon(dock,"⚙",Color.rgb(82,86,91),v->startActivity(new Intent(this,DexterSettingsActivity.class)));
        root.addView(dock,new LinearLayout.LayoutParams(-1,dp(76)));

        setContentView(root);
    }

    private GridLayout page(){GridLayout g=new GridLayout(this);g.setColumnCount(4);g.setRowCount(3);return g;}

    private void vectorTile(GridLayout grid,String name,int res,int color,android.view.View.OnClickListener click){
        LinearLayout box=tileShell(); LinearLayout iconBox=new LinearLayout(this); iconBox.setGravity(Gravity.CENTER); iconBox.setBackground(round(color,15)); iconBox.setElevation(dp(6)); iconBox.setOnClickListener(click);
        ImageView icon=new ImageView(this); icon.setImageResource(res); icon.setPadding(dp(13),dp(13),dp(13),dp(13)); iconBox.addView(icon,new LinearLayout.LayoutParams(-1,-1));
        box.addView(iconBox,new LinearLayout.LayoutParams(dp(54),dp(54))); addName(box,name); grid.addView(box);
    }

    private void dexterTile(GridLayout grid,String name,String glyph,int color,android.view.View.OnClickListener click){
        LinearLayout box=tileShell(); TextView icon=txt(glyph,glyph.length()>1?14:21,Color.WHITE,true); icon.setGravity(Gravity.CENTER); icon.setBackground(round(color,15)); icon.setElevation(dp(6)); icon.setOnClickListener(click);
        box.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54))); addName(box,name); grid.addView(box);
    }

    private void packageTile(GridLayout grid,String name,String[] pkgs){
        LinearLayout box=tileShell(); ImageView icon=new ImageView(this); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); icon.setPadding(dp(2),dp(2),dp(2),dp(2));
        Drawable d=findIcon(pkgs); if(d!=null)icon.setImageDrawable(d); else icon.setImageResource(R.drawable.ic_launcher); icon.setElevation(dp(6)); icon.setOnClickListener(v->launch(pkgs,name));
        box.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54))); addName(box,name); grid.addView(box);
    }

    private LinearLayout tileShell(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);box.setPadding(dp(1),dp(6),dp(1),0);GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=0;lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);box.setLayoutParams(lp);return box;}
    private void addName(LinearLayout box,String name){TextView n=txt(name,9.1f,text,false);n.setGravity(Gravity.CENTER);n.setMaxLines(1);n.setPadding(0,dp(4),0,0);box.addView(n,new LinearLayout.LayoutParams(-1,-2));}
    private void dockIcon(LinearLayout dock,String glyph,int color,android.view.View.OnClickListener click){TextView i=txt(glyph,21,Color.WHITE,true);i.setGravity(Gravity.CENTER);i.setBackground(round(color,16));i.setOnClickListener(click);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(54),dp(54),1f);lp.gravity=Gravity.CENTER;dock.addView(i,lp);}
    private void dockPackage(LinearLayout dock,String[] pkgs,String fallback,int color){Drawable d=findIcon(pkgs);if(d!=null){ImageView i=new ImageView(this);i.setImageDrawable(d);i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);i.setPadding(dp(2),dp(2),dp(2),dp(2));i.setOnClickListener(v->launch(pkgs,fallback));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(54),dp(54),1f);lp.gravity=Gravity.CENTER;dock.addView(i,lp);}else dockIcon(dock,fallback,color,v->launch(pkgs,fallback));}
    private Drawable findIcon(String[] pkgs){PackageManager pm=getPackageManager();for(String p:pkgs)try{return pm.getApplicationIcon(p);}catch(Exception ignored){}return null;}
    private void launch(String[] pkgs,String label){PackageManager pm=getPackageManager();for(String p:pkgs)try{Intent i=pm.getLaunchIntentForPackage(p);if(i!=null){startActivity(i);return;}}catch(Exception ignored){}try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q="+Uri.encode(label)+"&c=apps")));}catch(Exception e){web("https://play.google.com/store/search?q="+Uri.encode(label)+"&c=apps");}}
    private void web(String u){startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}
    private void updateDots(){if(dots!=null)dots.setText(pages.getDisplayedChild()==0?"●  ○":"○  ●");}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(gestures!=null)gestures.onTouchEvent(e);return super.dispatchTouchEvent(e);}
    private TextView txt(String v,float s,int c,boolean b){TextView x=new TextView(this);x.setText(v);x.setTextSize(s);x.setTextColor(c);if(b)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));return x;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
