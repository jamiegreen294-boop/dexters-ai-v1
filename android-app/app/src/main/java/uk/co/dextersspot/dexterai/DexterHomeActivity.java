package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "DexterBridge");
        web.loadUrl("file:///android_asset/launcher.html");
    }

    @Override public void onBackPressed() {
        if (web != null) {
            web.evaluateJavascript("(function(){var a=document.querySelector('.page.active');if(a&&a.id!=='home'){if(window.dexterBack){dexterBack()}else if(window.show){show('home')}return 'home';}return 'stay';})()", null);
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
        @JavascriptInterface public boolean toggleTorch(boolean on) {
            try {
                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 401);
                    return false;
                }
                CameraManager cm=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
                if(cm==null)return false;
                for(String id:cm.getCameraIdList()){
                    android.hardware.camera2.CameraCharacteristics cc=cm.getCameraCharacteristics(id);
                    Boolean flash=cc.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer facing=cc.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                    if(Boolean.TRUE.equals(flash) && (facing==null || facing==android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK)){
                        cm.setTorchMode(id,on); return true;
                    }
                }
            } catch(Exception ignored) {}
            return false;
        }
        @JavascriptInterface public boolean toggleWifi(boolean on) {
            try {
                WifiManager wm=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                return wm!=null && wm.setWifiEnabled(on);
            } catch(Exception ignored){ return false; }
        }
        @JavascriptInterface public boolean toggleBluetooth(boolean on) {
            try {
                BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();
                if(a==null)return false;
                return on ? a.enable() : a.disable();
            } catch(Exception ignored){ return false; }
        }
        @JavascriptInterface public void setMediaVolume(int percent) {
            try {
                AudioManager am=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
                if(am==null)return;
                int max=am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int v=Math.max(0,Math.min(max,Math.round(max*(percent/100f))));
                am.setStreamVolume(AudioManager.STREAM_MUSIC,v,0);
            } catch(Exception ignored){}
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
