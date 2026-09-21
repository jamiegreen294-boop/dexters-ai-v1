package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(0xFF050708);
        getWindow().setNavigationBarColor(0xFF050708);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setTextZoom(100);

        webView.addJavascriptInterface(new DexterBridge(), "Dexter");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    public class DexterBridge {
        @JavascriptInterface public void openUrl(String url) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface public void launch(String packageName) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
                if (i != null) startActivity(i);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface public void dial() {
            try { startActivity(new Intent(Intent.ACTION_DIAL)); } catch (Exception ignored) {}
        }

        @JavascriptInterface public void messages() {
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_APP_MESSAGING);
                startActivity(i);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface public void camera() {
            try { startActivity(new Intent("android.media.action.IMAGE_CAPTURE")); } catch (Exception ignored) {}
        }

        @JavascriptInterface public void systemSettings() {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception ignored) {}
        }

        @JavascriptInterface public void wifiSettings() {
            try { startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); } catch (Exception ignored) {}
        }

        @JavascriptInterface public void bluetoothSettings() {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); } catch (Exception ignored) {}
        }

        @JavascriptInterface public void batterySettings() {
            try { startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)); } catch (Exception ignored) {}
        }

        @JavascriptInterface public void securitySettings() {
            try { startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)); } catch (Exception ignored) {}
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript("window.dexterBack ? dexterBack() : false", null);
        } else {
            super.onBackPressed();
        }
    }
}
