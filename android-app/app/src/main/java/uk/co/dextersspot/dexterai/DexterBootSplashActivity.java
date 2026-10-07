package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.io.File;

public class DexterBootSplashActivity extends Activity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        FrameLayout shell=new FrameLayout(this);
        shell.setBackgroundColor(Color.BLACK);

        ImageView hero=new ImageView(this);
        hero.setBackgroundColor(Color.BLACK);
        hero.setScaleType(ImageView.ScaleType.FIT_CENTER);

        File exactArtwork=new File("/sdcard/Pictures/dexters_gold_dog.png");
        Bitmap bitmap=BitmapFactory.decodeFile(exactArtwork.getAbsolutePath());
        if(bitmap!=null){
            hero.setImageBitmap(bitmap);
        }else{
            hero.setImageResource(R.drawable.ic_launcher);
            hero.setPadding(dp(72),dp(160),dp(72),dp(160));
        }

        shell.addView(hero,new FrameLayout.LayoutParams(-1,-1));
        setContentView(shell);

        shell.postDelayed(()->{
            Intent home=new Intent(this,DexterHomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
        },1800);
    }

    @Override public void onBackPressed(){}

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
