package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DexterBootSplashActivity extends Activity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);

        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        root.addView(logo,new LinearLayout.LayoutParams(dp(168),dp(168)));

        TextView title=new TextView(this);
        title.setText("DEXTERS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,-2);
        tlp.topMargin=dp(20);
        root.addView(title,tlp);

        TextView sub=new TextView(this);
        sub.setText("Business Phone");
        sub.setTextColor(Color.rgb(190,190,195));
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2);
        slp.topMargin=dp(5);
        root.addView(sub,slp);

        setContentView(root);

        root.postDelayed(()->{
            Intent home=new Intent(this,DexterHomeActivity.class);
            home.putExtra("skipDexterSplash",true);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
        },1800);
    }

    @Override public void onBackPressed(){}

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
