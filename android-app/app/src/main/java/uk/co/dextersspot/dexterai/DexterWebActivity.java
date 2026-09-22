package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DexterWebActivity extends Activity {
    private WebView web;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        immersive();

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(2,4,5));

        LinearLayout bar=new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10),dp(8),dp(10),dp(8));
        bar.setBackgroundColor(Color.rgb(8,13,17));

        Button back=new Button(this); back.setText("‹"); back.setTextSize(26); back.setTextColor(Color.WHITE);
        back.setBackgroundColor(Color.TRANSPARENT); back.setOnClickListener(v->{if(web!=null&&web.canGoBack())web.goBack();else finish();});
        bar.addView(back,new LinearLayout.LayoutParams(dp(52),dp(48)));

        TextView title=new TextView(this); title.setText("DEXTER OS"); title.setTextColor(Color.WHITE); title.setTextSize(16); title.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(title,new LinearLayout.LayoutParams(0,dp(48),1f));

        Button close=new Button(this); close.setText("⌂"); close.setTextSize(20); close.setTextColor(Color.WHITE);
        close.setBackgroundColor(Color.TRANSPARENT); close.setOnClickListener(v->finish());
        bar.addView(close,new LinearLayout.LayoutParams(dp(52),dp(48)));
        root.addView(bar,new LinearLayout.LayoutParams(-1,dp(64)));

        web=new WebView(this);
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);

        String url=getIntent().getStringExtra("url");
        if(url==null || !(url.startsWith("https://")||url.startsWith("http://")))url="https://www.google.com/";
        web.loadUrl(url);
    }
    @Override protected void onResume(){super.onResume();immersive();}
    @Override public void onWindowFocusChanged(boolean h){super.onWindowFocusChanged(h);if(h)immersive();}
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else finish();}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
