package uk.co.dextersspot.dexterai;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
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
        DexterUi.applyImmersive(this);
        try { DeviceOwnerPolicy.applyCompanyShell(this); } catch(Exception ignored) {}

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
        if(DexterDeviceAdminReceiver.isDeviceOwner(this)){
            try { startLockTask(); } catch(Exception ignored) {}
        }
    }

    @Override protected void onResume(){
        super.onResume();
        DexterUi.applyImmersive(this);
        if(DexterDeviceAdminReceiver.isDeviceOwner(this)){
            try { startLockTask(); } catch(Exception ignored) {}
        }
    }

    @Override public void onBackPressed() {
        if (web != null) {
            web.evaluateJavascript("(function(){try{return dexterBack()?'handled':'home'}catch(e){return 'home'}})()", value -> {
                if ("\"home\"".equals(value)) {
                    web.evaluateJavascript("show('home')",null);
                }
            });
        }
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        DexterUi.applyImmersive(this);
        String page=intent.getStringExtra("page");
        if(web!=null && page!=null && page.matches("home|control|settings|store|about")){
            web.evaluateJavascript("show('"+page+"')",null);
        }
    }

    public final class Bridge {
        @JavascriptInterface public void openUrl(String url) {
            openDexterWeb(url);
        }
        @JavascriptInterface public void openBrowser() {
            openDexterWeb("https://www.google.com/");
        }
        @JavascriptInterface public void openCamera() {
            launchAny(new String[]{"com.android.camera2","com.android.camera","com.meizu.media.camera"});
        }
        @JavascriptInterface public void openMessages() {
            launchAny(new String[]{"com.google.android.apps.messaging","com.android.mms"});
        }
        @JavascriptInterface public void openPhone() {
            launchAny(new String[]{"com.google.android.dialer","com.android.dialer","com.android.contacts"});
        }
        @JavascriptInterface public void launchPackages(String packages) {
            launchAny(packages == null ? new String[]{} : packages.split(","));
        }
        @JavascriptInterface public void openSetting(String key) {
            runOnUiThread(() -> {
                if(web!=null)web.evaluateJavascript("openDexterSetting('"+safeSetting(key)+"')",null);
            });
        }
        @JavascriptInterface public int getBatteryLevel() {
            try {
                BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE);
                return bm==null?-1:bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            } catch(Exception e) { return -1; }
        }
        @JavascriptInterface public boolean toggleTorch() {
            try {
                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 404);
                    return false;
                }
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

    private String safeSetting(String key){
        if(key==null)return "system";
        return key.matches("network|devices|display|sound|battery|storage|security|accounts|system")?key:"system";
    }

    private void openDexterWeb(String url){
        try{
            Intent i=new Intent(this,DexterWebActivity.class);
            i.putExtra("url",url);
            startActivity(i);
        }catch(Exception ignored){}
    }

    private void launchAny(String[] packages) {
        PackageManager pm=getPackageManager();
        for(String p:packages){
            try {
                Intent i=pm.getLaunchIntentForPackage(p.trim());
                if(i!=null){ startActivity(i); return; }
            } catch(Exception ignored) {}
        }
        runOnUiThread(()->{
            if(web!=null)web.evaluateJavascript("showNotice('Dexter OS','This app is not installed or is not approved for this company phone.')",null);
        });
    }
}
