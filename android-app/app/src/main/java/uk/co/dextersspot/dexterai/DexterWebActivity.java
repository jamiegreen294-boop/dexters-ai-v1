package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DexterWebActivity extends Activity {
    private WebView web;
    private EditText address;
    private final int bg=Color.rgb(5,8,10), card=Color.rgb(18,25,30), gold=Color.rgb(244,207,113);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DexterUi.applyImmersive(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout bar=new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8),dp(6),dp(8),dp(6));
        bar.setBackgroundColor(card);

        TextView back=button("‹");
        back.setOnClickListener(v->{if(web.canGoBack())web.goBack();else finish();});
        bar.addView(back,new LinearLayout.LayoutParams(dp(44),dp(44)));

        TextView home=button("⌂");
        home.setOnClickListener(v->{
            startActivity(new Intent(this,DexterHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        bar.addView(home,new LinearLayout.LayoutParams(dp(44),dp(44)));

        address=new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.rgb(140,150,156));
        address.setTextSize(12);
        address.setHint("Search or enter web address");
        address.setPadding(dp(12),0,dp(12),0);
        address.setBackgroundColor(Color.rgb(29,38,44));
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setOnEditorActionListener((v,action,event)->{
            if(action==EditorInfo.IME_ACTION_GO){load(normalize(address.getText().toString()));return true;}
            return false;
        });
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(42),1f);
        ap.setMargins(dp(6),0,dp(6),0);
        bar.addView(address,ap);

        TextView close=button("×");
        close.setOnClickListener(v->finish());
        bar.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44)));
        root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));

        web=new WebView(this);
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                String scheme=request.getUrl().getScheme();
                if("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme))return false;
                return true;
            }
            @Override public void onPageFinished(WebView view,String url){
                address.setText(url);
            }
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);

        String start=getIntent().getStringExtra("url");
        if(start==null||start.trim().isEmpty())start="https://www.google.com/";
        load(normalize(start));
    }

    private void load(String u){address.setText(u);web.loadUrl(u);}
    private String normalize(String raw){
        String x=raw==null?"":raw.trim();
        if(x.isEmpty())return "https://www.google.com/";
        if(x.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*"))return x;
        if(x.contains(".")&&!x.contains(" "))return "https://"+x;
        return "https://www.google.com/search?q="+android.net.Uri.encode(x);
    }
    private TextView button(String t){
        TextView x=new TextView(this);x.setText(t);x.setTextSize(24);x.setTextColor(gold);x.setGravity(Gravity.CENTER);return x;
    }
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onResume(){super.onResume();DexterUi.applyImmersive(this);}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
