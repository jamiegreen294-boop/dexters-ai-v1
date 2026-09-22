package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;

public class DexterHomeActivity extends Activity {
    private WebView web;
    private boolean torchOn=false;
    private boolean rotationEnabled=false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF030506);
        getWindow().setNavigationBarColor(0xFF030506);
        enterImmersive();
        try { DeviceOwnerPolicy.applyBusinessMode(this); } catch(Exception ignored) {}
        try { if(DeviceAgentService.hasToken(this) || DexterDeviceAdminReceiver.isDeviceOwner(this)) DeviceAgentService.start(this); } catch(Exception ignored) {}

        web=new WebView(this);
        setContentView(web);
        WebSettings s=web.getSettings();
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
        web.addJavascriptInterface(new Bridge(),"DexterBridge");
        web.loadUrl("file:///android_asset/launcher.html");
    }

    @Override protected void onResume(){
        super.onResume();
        enterImmersive();
        try { if(DeviceAgentService.hasToken(this) || DexterDeviceAdminReceiver.isDeviceOwner(this)) DeviceAgentService.start(this); } catch(Exception ignored) {}
        try {
            DevicePolicyManager d=(DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
            if(d!=null && d.isLockTaskPermitted(getPackageName())) startLockTask();
        } catch(Exception ignored) {}
    }

    @Override public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) enterImmersive();
    }

    private void enterImmersive(){
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override public void onBackPressed() {
        if(web!=null){
            web.evaluateJavascript("(function(){try{return dexterBack()?'handled':'home'}catch(e){return 'home'}})()",value->{
                if("\"home\"".equals(value)) {
                    web.evaluateJavascript("show('home')",null);
                }
            });
        }
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        String page=intent.getStringExtra("page");
        if(web!=null && page!=null && page.matches("home|control|settings|store|about")){
            web.evaluateJavascript("show('"+page+"')",null);
        }
    }

    public final class Bridge {
        @JavascriptInterface public void openUrl(String url){
            try {
                if(url==null || !(url.startsWith("https://")||url.startsWith("http://"))) return;
                Intent i=new Intent(DexterHomeActivity.this,DexterWebActivity.class);
                i.putExtra("url",url);
                startActivity(i);
            } catch(Exception ignored) {}
        }
        @JavascriptInterface public void openCamera(){
            launchAny(new String[]{"com.android.camera2","com.android.camera","com.meizu.media.camera"});
        }
        @JavascriptInterface public void openBrowser(){
            openUrl("https://www.google.com/");
        }
        @JavascriptInterface public void openMessages(){
            launchAny(new String[]{"com.google.android.apps.messaging","com.android.mms"});
        }
        @JavascriptInterface public void openPhone(){
            try { startActivity(new Intent(Intent.ACTION_DIAL)); } catch(Exception ignored) {}
        }
        @JavascriptInterface public void launchPackages(String packages){
            launchAny(packages==null?new String[]{}:packages.split(","));
        }
        @JavascriptInterface public void openSetting(String key){
            // Commercial shell never opens Android Settings. All visible settings
            // are handled inside Dexter OS or through Device Owner policy.
        }
        @JavascriptInterface public int getBatteryLevel(){
            try {
                BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE);
                return bm==null?-1:bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            } catch(Exception e){ return -1; }
        }
        @JavascriptInterface public boolean toggleTorch(){
            try {
                if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) return false;
                CameraManager cm=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
                if(cm==null)return false;
                String selected=null;
                for(String id:cm.getCameraIdList()){
                    Boolean flash=cm.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if(Boolean.TRUE.equals(flash)){selected=id;break;}
                }
                if(selected==null)return false;
                torchOn=!torchOn; cm.setTorchMode(selected,torchOn); return torchOn;
            } catch(Exception e){ torchOn=false; return false; }
        }
        @JavascriptInterface public boolean toggleWifi(){
            try {
                WifiManager wm=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if(wm==null)return false;
                boolean next=!wm.isWifiEnabled();
                boolean accepted=wm.setWifiEnabled(next);
                return accepted?next:wm.isWifiEnabled();
            } catch(Exception e){ return false; }
        }
        @JavascriptInterface public boolean isWifiEnabled(){
            try {
                WifiManager wm=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                return wm!=null && wm.isWifiEnabled();
            } catch(Exception e){ return false; }
        }
        @JavascriptInterface public boolean toggleLocation(){
            try {
                DevicePolicyManager d=(DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
                if(d==null || !DexterDeviceAdminReceiver.isDeviceOwner(DexterHomeActivity.this))return false;
                boolean now=isLocationEnabled();
                d.setLocationEnabled(DexterDeviceAdminReceiver.component(DexterHomeActivity.this),!now);
                return !now;
            } catch(Exception e){ return false; }
        }
        @JavascriptInterface public boolean isLocationEnabled(){
            try {
                android.location.LocationManager lm=(android.location.LocationManager)getSystemService(LOCATION_SERVICE);
                return lm!=null && lm.isLocationEnabled();
            } catch(Exception e){ return false; }
        }
        @JavascriptInterface public int setBrightness(int percent){
            int p=Math.max(5,Math.min(100,percent));
            runOnUiThread(()->{
                WindowManager.LayoutParams lp=getWindow().getAttributes();
                lp.screenBrightness=p/100f;
                getWindow().setAttributes(lp);
            });
            return p;
        }
        @JavascriptInterface public int setMediaVolume(int percent){
            try {
                AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
                if(am==null)return -1;
                int max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int v=Math.max(0,Math.min(max,Math.round(max*Math.max(0,Math.min(100,percent))/100f)));
                am.setStreamVolume(AudioManager.STREAM_MUSIC,v,0);
                return Math.round(v*100f/Math.max(1,max));
            } catch(Exception e){ return -1; }
        }
        @JavascriptInterface public int getMediaVolume(){
            try {
                AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);
                int max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                return Math.round(am.getStreamVolume(AudioManager.STREAM_MUSIC)*100f/Math.max(1,max));
            } catch(Exception e){ return -1; }
        }
        @JavascriptInterface public boolean setRotationEnabled(boolean enabled){
            rotationEnabled=enabled;
            runOnUiThread(()->setRequestedOrientation(enabled?ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            return rotationEnabled;
        }
        @JavascriptInterface public String getStorageSummary(){
            try {
                StatFs s=new StatFs(Environment.getDataDirectory().getAbsolutePath());
                long total=s.getTotalBytes(),free=s.getAvailableBytes(),used=total-free;
                return human(used)+" used · "+human(free)+" free · "+human(total)+" total";
            } catch(Exception e){ return "Storage unavailable"; }
        }
        @JavascriptInterface public boolean isDeviceOwner(){
            return DexterDeviceAdminReceiver.isDeviceOwner(DexterHomeActivity.this);
        }
        @JavascriptInterface public String getDeviceModel(){ return Build.MANUFACTURER+" "+Build.MODEL; }
        @JavascriptInterface public String getAppIcon(String packageNames){
            try {
                String[] ps=packageNames==null?new String[]{}:packageNames.split(",");
                PackageManager pm=getPackageManager(); Drawable d=null;
                for(String p:ps){try{d=pm.getApplicationIcon(p.trim());break;}catch(Exception ignored){}}
                if(d==null)return "";
                int w=Math.max(1,d.getIntrinsicWidth()),h=Math.max(1,d.getIntrinsicHeight());
                Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                Canvas c=new Canvas(bmp); d.setBounds(0,0,c.getWidth(),c.getHeight()); d.draw(c);
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG,100,out);
                return "data:image/png;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
            } catch(Exception e){return "";}
        }
    }

    private static String human(long bytes){
        double v=bytes; String[] u={"B","KB","MB","GB","TB"}; int i=0;
        while(v>=1024 && i<u.length-1){v/=1024;i++;}
        return String.format(java.util.Locale.UK,i==0?"%.0f %s":"%.1f %s",v,u[i]);
    }

    private void launchAny(String[] packages){
        PackageManager pm=getPackageManager();
        for(String p:packages){
            try {
                Intent i=pm.getLaunchIntentForPackage(p.trim());
                if(i!=null){startActivity(i);return;}
            } catch(Exception ignored){}
        }
        if(web!=null) web.post(()->web.evaluateJavascript("showNotice('App unavailable','This app is not installed. Use Dexter Store or Dexter AI to add an approved app.')",null));
    }
}
