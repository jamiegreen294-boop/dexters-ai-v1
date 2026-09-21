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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
        cp.gravity=Gravity.CENTER;
        cp.leftMargin=dp(28); cp.rightMargin=dp(28);
        shell.addView(centre,cp);

        FrameLayout logoRing=new FrameLayout(this);
        GradientDrawable ring=new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.rgb(18,18,19));
        ring.setStroke(dp(2),Color.rgb(218,170,78));
        logoRing.setBackground(ring);
        logoRing.setElevation(dp(8));
        centre.addView(logoRing,new LinearLayout.LayoutParams(dp(132),dp(132)));

        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        logo.setPadding(dp(18),dp(18),dp(18),dp(18));
        logoRing.addView(logo,new FrameLayout.LayoutParams(-1,-1));

        TextView title=text("DEXTERS",31,Color.WHITE,true);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-2,-2); tp.topMargin=dp(22);
        centre.addView(title,tp);

        TextView sub=text("B U S I N E S S   P H O N E",11,Color.rgb(218,170,78),true);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-2,-2); sp.topMargin=dp(6);
        centre.addView(sub,sp);

        TextView line=text("WORK SMARTER  •  GO FURTHER",10,Color.rgb(205,205,208),false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.topMargin=dp(34);
        centre.addView(line,lp);

        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(dp(210),dp(3));
        pp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL; pp.bottomMargin=dp(112);
        shell.addView(progress,pp);

        TextView powered=text("Powered by\nDexter OS",11,Color.rgb(210,210,214),false);
        powered.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams pw=new FrameLayout.LayoutParams(-2,-2);
        pw.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL; pw.bottomMargin=dp(44);
        shell.addView(powered,pw);

        setContentView(shell);
        shell.postDelayed(()->{
            Intent home=new Intent(this,DexterHomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
        },1450);
    }

    @Override public void onBackPressed(){}

    private TextView text(String v,float size,int color,boolean bold){
        TextView x=new TextView(this); x.setText(v); x.setTextSize(size); x.setTextColor(color); x.setGravity(Gravity.CENTER);
        if(bold)x.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
        return x;
    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
