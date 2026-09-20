package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String DEXTER_URL =
        "https://jamiegreen294-boop.github.io/dexters-ai-v1/?android-app=1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(0xFF11100F);
        getWindow().setNavigationBarColor(0xFF11100F);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.addJavascriptInterface(new DexterDeviceBridge(this), "DexterDevice");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if ("jamiegreen294-boop.github.io".equalsIgnoreCase(host)) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                return true;
            }
        });

        if (savedInstanceState == null) webView.loadUrl(DEXTER_URL);
        else webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DeviceAgentService.hasToken(this)) DeviceAgentService.start(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    public static class DexterDeviceBridge {
        private final Context context;
        DexterDeviceBridge(Context context) { this.context = context; }

        @JavascriptInterface public String getDeviceId() {
            String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return id == null ? "android-unknown" : id;
        }

        @JavascriptInterface public String getDeviceInfo() {
            try {
                JSONObject j = new JSONObject();
                j.put("manufacturer", Build.MANUFACTURER);
                j.put("model", Build.MODEL);
                j.put("osVersion", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
                j.put("appVersion", BuildConfig.VERSION_NAME);
                JSONObject c = new JSONObject();
                c.put("deviceHealth", true);
                c.put("appsInventory", true);
                c.put("appLaunch", true);
                c.put("apkInstall", true);
                c.put("appUninstall", true);
                c.put("silentInstall", false);
                c.put("wirelessDebugging", Build.VERSION.SDK_INT >= 30);
                j.put("capabilities", c);
                return j.toString();
            } catch (Exception e) { return "{}"; }
        }

        @JavascriptInterface public boolean saveDeviceToken(String token) {
            if (token == null || token.length() < 32) return false;
            SharedPreferences p = context.getSharedPreferences("dexter_device", MODE_PRIVATE);
            p.edit().putString("device_token", token).apply();
            DeviceAgentService.start(context);
            return true;
        }

        @JavascriptInterface public boolean isPaired() {
            return DeviceAgentService.hasToken(context);
        }

        @JavascriptInterface public void openUnknownSourcesSettings() {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:" + context.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
