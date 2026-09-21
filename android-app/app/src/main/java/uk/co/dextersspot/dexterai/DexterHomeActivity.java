package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.content.Context;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;

public class DexterHomeActivity extends Activity {
    private WebView web;
    private boolean torchOn=false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF030506);
        getWindow().setNavigationBarColor(0xFF030506);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                String page=getIntent().getStringExtra("page");
                if(page!=null && page.matches("home|control|settings|store|about")){
                    view.evaluateJavascript("show('"+page+"')",null);
                }
            }
        });
        web.addJavascriptInterface(new Bridge(), "DexterBridge");
        web.loadUrl("file:///android_asset/launcher.html");
    }

    @Override public void onBackPressed() {
        if (web != null) {
            web.evaluateJavascript("(function(){var a=document.querySelector('.page.active');if(a&&a.id!=='home'){showPage('home');return 'home';}return 'stay';})()", null);
        }
    }

    public final class Bridge {
        @JavascriptInterface public void openUrl(String url) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch(Exception ignored) {}
        }
        @JavascriptInterface public void openCamera() {
            launchAny(new String[]{"com.android.camera2","com.android.camera","com.meizu.media.camera"});
        }
        @JavascriptInterface public void openBrowser() {
            launchAny(new String[]{"com.android.chrome","com.google.android.googlequicksearchbox"});
        }
        @JavascriptInterface public void openMessages() {
            launchAny(new String[]{"com.google.android.apps.messaging","com.android.mms"});
        }
        @JavascriptInterface public void openPhone() {
            try { startActivity(new Intent(Intent.ACTION_DIAL)); } catch(Exception ignored) {}
        }
        @JavascriptInterface public void launchPackages(String packages) {
            launchAny(packages == null ? new String[]{} : packages.split(","));
        }
        @JavascriptInterface public void openSetting(String key) {
            try {
                Intent i;
                if ("wifi".equals(key)) i=new Intent(Settings.ACTION_WIFI_SETTINGS);
                else if ("bluetooth".equals(key)) i=new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                else if ("apps".equals(key)) i=new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                else if ("battery".equals(key)) i=new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                else if ("storage".equals(key)) i=new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
                else if ("security".equals(key)) i=new Intent(Settings.ACTION_SECURITY_SETTINGS);
                else i=new Intent(Settings.ACTION_SETTINGS);
                startActivity(i);
            } catch(Exception ignored) {}
        }
        @JavascriptInterface public int getBatteryLevel() {
            try {
                BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE);
                return bm==null?-1:bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            } catch(Exception e) { return -1; }
        }
        @JavascriptInterface public boolean toggleTorch() {
            try {
                CameraManager cm=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
                if(cm==null)return false;
                String selected=null;
                for(String id:cm.getCameraIdList()){
                    Boolean flash=cm.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if(Boolean.TRUE.equals(flash)){selected=id;break;}
                }
                if(selected==null)return false;
                torchOn=!torchOn;
                cm.setTorchMode(selected,torchOn);
                return torchOn;
            } catch(Exception e) { torchOn=false; return false; }
        }
        @JavascriptInterface public String getDeviceModel() {
            return Build.MANUFACTURER+" "+Build.MODEL;
        }
        @JavascriptInterface public String getAppIcon(String packageNames) {
            try {
                String[] ps=packageNames==null?new String[]{}:packageNames.split(",");
                PackageManager pm=getPackageManager();
                Drawable d=null;
                for(String p:ps){ try { d=pm.getApplicationIcon(p.trim()); break; } catch(Exception ignored){} }
                if(d==null)return "";
                int w=Math.max(1,d.getIntrinsicWidth()), h=Math.max(1,d.getIntrinsicHeight());
                Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                Canvas c=new Canvas(bmp); d.setBounds(0,0,c.getWidth(),c.getHeight()); d.draw(c);
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG,100,out);
                return "data:image/png;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
            } catch(Exception e) { return ""; }
        }
    }

    private void launchAny(String[] packages) {
        PackageManager pm=getPackageManager();
        for(String p:packages){
            try {
                Intent i=pm.getLaunchIntentForPackage(p.trim());
                if(i!=null){ startActivity(i); return; }
            } catch(Exception ignored) {}
        }
    }
}
