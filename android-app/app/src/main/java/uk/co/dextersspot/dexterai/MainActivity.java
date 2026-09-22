package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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
                Intent i=new Intent(MainActivity.this,DexterWebActivity.class);
                i.putExtra("url",request.getUrl().toString());
                startActivity(i);
                return true;
            }
        });

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (savedInstanceState == null) webView.loadUrl(DEXTER_URL);
        else webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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
                j.put("appVersion", appVersion(context));
                JSONObject c = new JSONObject();
                c.put("deviceHealth", true);
                c.put("appsInventory", true);
                c.put("appLaunch", true);
                c.put("apkInstall", true);
                c.put("appUninstall", true);
                c.put("silentInstall", DexterDeviceAdminReceiver.isDeviceOwner(context));
                c.put("deviceOwner", DexterDeviceAdminReceiver.isDeviceOwner(context));
                c.put("wirelessDebugging", Build.VERSION.SDK_INT >= 30);
                c.put("localAdbDiagnostics", Build.VERSION.SDK_INT >= 30);
                j.put("capabilities", c);
                j.put("management", DeviceOwnerPolicy.status(context));
                return j.toString();
            } catch (Exception e) { return "{}"; }
        }

        private String appVersion(Context c) {
            try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; }
            catch (Exception e) { return "unknown"; }
        }

        @JavascriptInterface public boolean saveDeviceToken(String token) {
            if (token == null || token.length() < 32) return false;
            DeviceAgentService.saveToken(context, token);
            DeviceAgentService.start(context);
            return true;
        }

        @JavascriptInterface public boolean isPaired() {
            return DeviceAgentService.hasToken(context);
        }

        @JavascriptInterface public String getAgentState() {
            try {
                SharedPreferences p = context.getSharedPreferences("dexter_device", MODE_PRIVATE);
                JSONObject j = new JSONObject();
                j.put("tokenPresent", DeviceAgentService.hasToken(context));
                j.put("lastSuccessAt", p.getLong("agent_last_success_at", 0L));
                j.put("lastErrorAt", p.getLong("agent_last_error_at", 0L));
                j.put("lastError", p.getString("agent_last_error", ""));
                return j.toString();
            } catch (Exception e) { return "{}"; }
        }

        @JavascriptInterface public String getManagementStatus() {
            try { return DeviceOwnerPolicy.status(context).toString(); }
            catch (Exception e) { return "{\"deviceOwner\":false}"; }
        }

        @JavascriptInterface public String applyBusinessMode() {
            try { return DeviceOwnerPolicy.applyBusinessMode(context).toString(); }
            catch (Exception e) {
                String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                return "{\"error\":\"" + m.replace("\"", "'") + "\"}";
            }
        }

        @JavascriptInterface public String pairLocalAdb(String host, int port, String code) {
            try { return DexterLocalAdb.get(context).pairLocal(host, port, code).toString(); }
            catch (Exception e) { return errorJson(e); }
        }

        @JavascriptInterface public String connectLocalAdb(String host, int port) {
            try { return DexterLocalAdb.get(context).connectLocal(host, port).toString(); }
            catch (Exception e) { return errorJson(e); }
        }

        @JavascriptInterface public String runLocalOwnerDiagnostics(String host, int port) {
            try { return DexterLocalAdb.get(context).runOwnerDiagnostics(host, port).toString(); }
            catch (Exception e) { return errorJson(e); }
        }

        private String errorJson(Exception e) {
            try {
                JSONObject j = new JSONObject();
                String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                j.put("error", m);
                return j.toString();
            } catch (Exception ignored) { return "{\"error\":\"Local ADB failed\"}"; }
        }

        private void openDexterSettings(){
            try {
                Intent i=new Intent(context,DexterHomeActivity.class);
                i.putExtra("page","settings");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(i);
            } catch(Exception ignored){}
        }

        @JavascriptInterface public void openWirelessDebuggingSettings() { openDexterSettings(); }

        @JavascriptInterface public void openBatteryOptimizationSettings() { openDexterSettings(); }

        @JavascriptInterface public void openBusinessApps() {
            try {
                Intent i = new Intent(context, BusinessAppsActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface public void openQuickStart() {
            try {
                Intent i = new Intent(context, QuickStartActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface public void openUnknownSourcesSettings() { openDexterSettings(); }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
