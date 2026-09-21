package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

public class DexterBootSplashActivity extends Activity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        FrameLayout shell=new FrameLayout(this);
        shell.setBackgroundColor(Color.BLACK);

        LinearLayout centre=new LinearLayout(this);
        centre.setOrientation(LinearLayout.VERTICAL);
        centre.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2);
        cp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        cp.topMargin=dp(205); cp.leftMargin=dp(24); cp.rightMargin=dp(24);
        shell.addView(centre,cp);

        TextView d=text("D",104,Color.rgb(244,207,113),true);
        d.setShadowLayer(dp(12),0,0,Color.rgb(115,77,22));
        centre.addView(d,new LinearLayout.LayoutParams(-2,-2));

        TextView title=text("DEXTERS",31,Color.WHITE,true);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-2,-2); tp.topMargin=dp(-8);
        centre.addView(title,tp);

        TextView sub=text("B U S I N E S S   P H O N E",11,Color.rgb(231,199,101),true);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-2,-2); sp.topMargin=dp(5);
        centre.addView(sub,sp);

        TextView line=text("W O R K   S M A R T E R\nG O   F U R T H E R",10,Color.rgb(231,199,101),false);
        line.setGravity(Gravity.CENTER); line.setLineSpacing(dp(6),1f);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2);
        lp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL; lp.topMargin=dp(500);
        shell.addView(line,lp);

        View track=new View(this);
        GradientDrawable tg=new GradientDrawable();
        tg.setColor(Color.rgb(75,80,83)); tg.setCornerRadius(dp(3)); track.setBackground(tg);
        FrameLayout.LayoutParams trp=new FrameLayout.LayoutParams(dp(185),dp(4));
        trp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL; trp.bottomMargin=dp(135);
        shell.addView(track,trp);

        View fill=new View(this);
        GradientDrawable fg=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.WHITE,Color.rgb(218,166,46)});
        fg.setCornerRadius(dp(3)); fill.setBackground(fg);
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(108),dp(4));
        fp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL; fp.bottomMargin=dp(135);
        fp.leftMargin=-dp(77);
        shell.addView(fill,fp);

        TextView powered=text("Powered by\nDexter OS",11,Color.rgb(220,220,224),false);
        powered.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams pw=new FrameLayout.LayoutParams(-2,-2);
        pw.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL; pw.bottomMargin=dp(52);
        shell.addView(powered,pw);

        setContentView(shell);
        shell.postDelayed(()->{
            Intent home=new Intent(this,DexterHomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
        },1500);
    }

    @Override public void onBackPressed(){}

    private TextView text(String v,float size,int color,boolean bold){
        TextView x=new TextView(this); x.setText(v); x.setTextSize(size); x.setTextColor(color); x.setGravity(Gravity.CENTER);
        if(bold)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return x;
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
