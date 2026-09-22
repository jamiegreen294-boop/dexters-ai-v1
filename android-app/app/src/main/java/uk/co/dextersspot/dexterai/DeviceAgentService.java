package uk.co.dextersspot.dexterai;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class DeviceAgentService extends Service {
    private static final String CHANNEL = "dexter_device_agent";
    private static final int NOTIFICATION_ID = 294;
    private static final String API = "https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/ai-command-centre";
    private static final String SEND_API = "https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/dexter-send-preview";
    private static final String SEND_CHANNEL = "dexter_send";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;

    public static boolean hasToken(Context c) {
        return readToken(c).length() >= 32;
    }

    public static String getDeviceToken(Context c) { return readToken(c); }

    public static void saveToken(Context c, String token) {
        if (token == null || token.length() < 32) return;
        c.getSharedPreferences("dexter_device", MODE_PRIVATE).edit().putString("device_token", token).apply();
        try {
            Context dps = c.createDeviceProtectedStorageContext();
            dps.getSharedPreferences("dexter_device", MODE_PRIVATE).edit().putString("device_token", token).apply();
        } catch (Exception ignored) {}
    }

    private static String readToken(Context c) {
        String t = c.getSharedPreferences("dexter_device", MODE_PRIVATE).getString("device_token", "");
        if (t != null && t.length() >= 32) return t;
        try {
            Context dps = c.createDeviceProtectedStorageContext();
            t = dps.getSharedPreferences("dexter_device", MODE_PRIVATE).getString("device_token", "");
            if (t != null && t.length() >= 32) {
                c.getSharedPreferences("dexter_device", MODE_PRIVATE).edit().putString("device_token", t).apply();
                return t;
            }
        } catch (Exception ignored) {}
        return "";
    }
    public static void start(Context c) {
        Intent i = new Intent(c, DeviceAgentService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Dexter device agent connected"));
        AgentWatchdogReceiver.schedule(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) { running = true; new Thread(this::loop, "dexter-phone-agent").start(); }
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        try {
            Intent restart=new Intent(getApplicationContext(),DeviceAgentService.class);
            PendingIntent pi=PendingIntent.getService(getApplicationContext(),295,restart,PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if(am!=null) am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+5000,pi);
        } catch(Exception ignored){}
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() { running = false; super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }

    private void loop() {
        while (running) {
            try { DeviceAccessClient.enforceExpiry(this); } catch(Exception ignored) {}
            try {
                pollJobs();
            } catch (Exception e) {
                recordAgentError("pollJobs: " + String.valueOf(e.getMessage()));
            }
            try {
                heartbeat();
            } catch (Exception e) {
                recordAgentError("heartbeat: " + String.valueOf(e.getMessage()));
            }
            try {
                pollDexterSend();
            } catch (Exception e) {
                recordAgentError("dexterSend: " + String.valueOf(e.getMessage()));
            }
            try { Thread.sleep(30000); } catch (InterruptedException e) { return; }
        }
    }

    private String token() {
        return readToken(this);
    }

    private void recordAgentError(String message) {
        getSharedPreferences("dexter_device",MODE_PRIVATE).edit()
            .putString("agent_last_error",message == null ? "unknown" : message)
            .putLong("agent_last_error_at",System.currentTimeMillis()).apply();
    }

    private JSONObject post(JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(API).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(20000); c.setRequestMethod("POST");
        c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("x-dexter-device-token", token());
        try(OutputStream os=c.getOutputStream()){ os.write(body.toString().getBytes("UTF-8")); }
        InputStream is=(c.getResponseCode()>=200&&c.getResponseCode()<300)?c.getInputStream():c.getErrorStream();
        String text=readAll(is);
        if(c.getResponseCode()<200||c.getResponseCode()>=300)throw new IOException("HTTP "+c.getResponseCode()+": "+text);
        return new JSONObject(text.length()==0?"{}":text);
    }

    private void heartbeat() throws Exception {
        JSONObject info=new JSONObject();
        info.put("manufacturer",Build.MANUFACTURER);info.put("model",Build.MODEL);
        info.put("osVersion",Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")");
        info.put("appVersion",appVersion());
        info.put("securityPatch",Build.VERSION.SECURITY_PATCH);
        android.content.SharedPreferences agentPrefs=getSharedPreferences("dexter_device",MODE_PRIVATE);
        info.put("agentLastSuccessAt",agentPrefs.getLong("agent_last_success_at",0));
        info.put("agentLastErrorAt",agentPrefs.getLong("agent_last_error_at",0));
        info.put("agentLastError",agentPrefs.getString("agent_last_error",""));
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        info.put("batteryOptimisationIgnored",pm!=null && pm.isIgnoringBatteryOptimizations(getPackageName()));
        JSONObject cap=new JSONObject();
        cap.put("deviceHealth",true);cap.put("appsInventory",true);cap.put("appLaunch",true);
        cap.put("apkInstall",true);cap.put("appUninstall",true);cap.put("silentInstall",DexterDeviceAdminReceiver.isDeviceOwner(this));cap.put("deviceOwner",DexterDeviceAdminReceiver.isDeviceOwner(this));
        cap.put("canRequestPackageInstalls",getPackageManager().canRequestPackageInstalls());
        cap.put("wirelessDebugging",Build.VERSION.SDK_INT>=30);
        cap.put("remoteLock",DexterDeviceAdminReceiver.isDeviceOwner(this));
        cap.put("remoteWipe",DexterDeviceAdminReceiver.isDeviceOwner(this));
        cap.put("internetPolicy",DexterDeviceAdminReceiver.isDeviceOwner(this));
        cap.put("configSnapshot",true);
        cap.put("remoteScreen",true);
        cap.put("remoteInput",true);
        cap.put("uiInspection",true);
        cap.put("persistentRemoteControl",DexterRemoteAccessibilityService.isReady());
        info.put("capabilities",cap);
        try {
            info.put("management",DeviceOwnerPolicy.status(this));
        } catch (Exception e) {
            info.put("management",new JSONObject()
                .put("deviceOwner",DexterDeviceAdminReceiver.isDeviceOwner(this))
                .put("statusError",String.valueOf(e.getMessage())));
        }
        post(new JSONObject().put("action","device_heartbeat").put("sessionId",UUID.randomUUID().toString()).put("info",info));
        getSharedPreferences("dexter_device",MODE_PRIVATE).edit()
            .putLong("agent_last_success_at",System.currentTimeMillis())
            .remove("agent_last_error").apply();
    }

    private void pollJobs() throws Exception {
        JSONObject d=post(new JSONObject().put("action","device_jobs_poll").put("sessionId",UUID.randomUUID().toString()));
        JSONArray jobs=d.optJSONArray("jobs"); if(jobs==null)return;
        for(int i=0;i<jobs.length();i++){
            JSONObject job=jobs.getJSONObject(i);
            String id=job.getString("id"),type=job.getString("job_type");
            JSONObject req=job.optJSONObject("request"); if(req==null)req=new JSONObject();
            try{
                JSONObject result=execute(type,req);
                complete(id,true,result,null);
            }catch(Exception e){complete(id,false,null,e.getMessage());}
        }
    }

    private JSONObject execute(String type, JSONObject req) throws Exception {
        switch(type){
            case "device.health": return deviceHealth();
            case "device.policy.status": return DeviceOwnerPolicy.status(this);
            case "device.policy.apply_business": return DeviceOwnerPolicy.applyBusinessMode(this);
            case "device.role.set": return DeviceOwnerPolicy.setAccessRole(this,req.optString("role","staff"));
            case "device.lock": return DeviceOwnerPolicy.lockNow(this);
            case "device.wipe": return DeviceOwnerPolicy.wipeDevice(this,req.optString("reason","Owner-authorised remote wipe"));
            case "device.internet.protect": return DeviceOwnerPolicy.setInternetProtection(this,req.optString("hostname","security.cloudflare-dns.com"));
            case "device.internet.clear": return DeviceOwnerPolicy.clearInternetProtection(this);
            case "device.launcher.release": return DeviceOwnerPolicy.releaseLauncher(this);
            case "device.apps.protect": return DeviceOwnerPolicy.protectApps(this,req.optJSONArray("packages"));
            case "device.config.snapshot": return DeviceOwnerPolicy.configurationSnapshot(this);
            case "device.config.restore": return DeviceOwnerPolicy.restoreStandardConfiguration(this);
            case "device.pos.configure": return configurePos(req);
            case "device.pos.register": return registerPos();
            case "apps.inventory": return appInventory();
            case "device.local_adb.shell": return DexterLocalAdb.get(this).runShellCommand(req.getString("command"));
            case "device.screen.capture": return remoteCapture();
            case "device.screen.ui_dump": return remoteUiDump();
            case "device.input.tap": return remoteTap(req.getInt("x"),req.getInt("y"));
            case "device.input.swipe": return remoteSwipe(req.getInt("x1"),req.getInt("y1"),req.getInt("x2"),req.getInt("y2"),req.optInt("durationMs",300));
            case "device.input.key": return remoteKey(req.getString("key"));
            case "device.input.text": return remoteText(req.getString("text"));
            case "app.launch": return launchApp(req.getString("packageName"));
            case "app.install": return installApk(req,false);
            case "dexter.self_update": return installApk(req,true);
            case "app.uninstall": return requestUninstall(req.getString("packageName"));
            default: throw new IllegalArgumentException("Unsupported job: "+type);
        }
    }

    private JSONObject remoteCapture() throws Exception {
        DexterRemoteAccessibilityService s=DexterRemoteAccessibilityService.get();
        if(s!=null){
            JSONObject r=s.captureScreen();
            if(r.optBoolean("connected",false)) return r;
        }
        return DexterLocalAdb.get(this).captureScreen();
    }

    private JSONObject remoteUiDump() throws Exception {
        DexterRemoteAccessibilityService s=DexterRemoteAccessibilityService.get();
        if(s!=null){
            JSONObject r=s.dumpUi();
            if(r.optBoolean("connected",false)) return r;
        }
        return DexterLocalAdb.get(this).dumpUi();
    }

    private JSONObject remoteTap(int x,int y) throws Exception {
        DexterRemoteAccessibilityService s=DexterRemoteAccessibilityService.get();
        if(s!=null){
            JSONObject r=s.tap(x,y);
            if(r.optBoolean("connected",false)) return r;
        }
        return DexterLocalAdb.get(this).inputTap(x,y);
    }

    private JSONObject remoteSwipe(int x1,int y1,int x2,int y2,int durationMs) throws Exception {
        DexterRemoteAccessibilityService s=DexterRemoteAccessibilityService.get();
        if(s!=null){
            JSONObject r=s.swipe(x1,y1,x2,y2,durationMs);
            if(r.optBoolean("connected",false)) return r;
        }
        return DexterLocalAdb.get(this).inputSwipe(x1,y1,x2,y2,durationMs);
    }

    private JSONObject remoteKey(String key) throws Exception {
        DexterRemoteAccessibilityService s=DexterRemoteAccessibilityService.get();
        if(s!=null){
            JSONObject r=s.key(key);
            if(r.optBoolean("connected",false)) return r;
        }
        return DexterLocalAdb.get(this).inputKey(key);
    }

    private JSONObject remoteText(String text) throws Exception {
        DexterRemoteAccessibilityService s=DexterRemoteAccessibilityService.get();
        if(s!=null){
            JSONObject r=s.inputText(text);
            if(r.optBoolean("connected",false)) return r;
        }
        return DexterLocalAdb.get(this).inputText(text);
    }

    private JSONObject registerPos() throws Exception {
        String token=readToken(this);
        if(token==null||token.length()<32) throw new Exception("Dexter managed device token is unavailable.");
        HttpURLConnection c=(HttpURLConnection)new URL("https://bpnkouymdvcogeaqjmxl.supabase.co/functions/v1/pc-pos-managed-register").openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestMethod("POST");c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");
        c.setRequestProperty("x-dexter-device-token",token);
        try(OutputStream os=c.getOutputStream()){os.write("{}".getBytes("UTF-8"));}
        InputStream is=(c.getResponseCode()>=200&&c.getResponseCode()<300)?c.getInputStream():c.getErrorStream();
        String raw=readAll(is);
        if(c.getResponseCode()<200||c.getResponseCode()>=300) throw new IOException("POS register HTTP "+c.getResponseCode()+": "+raw);
        JSONObject x=new JSONObject(raw.length()==0?"{}":raw);
        String deviceId=x.optString("device_id","");
        String deviceSecret=x.optString("device_secret","");
        if(deviceId.isEmpty()||deviceSecret.length()<32) throw new Exception("POS registration did not return valid credentials.");
        getSharedPreferences("dexter_pos",MODE_PRIVATE).edit()
            .putString("device_id",deviceId)
            .putString("device_secret",deviceSecret)
            .apply();
        return new JSONObject().put("registered",true).put("deviceId",deviceId);
    }

    private JSONObject configurePos(JSONObject req) throws Exception {
        String deviceId=req.optString("deviceId","");
        String deviceSecret=req.optString("deviceSecret","");
        if(deviceId.isEmpty() || deviceSecret.length()<32) throw new Exception("POS device credentials are incomplete.");
        getSharedPreferences("dexter_pos",MODE_PRIVATE).edit()
            .putString("device_id",deviceId)
            .putString("device_secret",deviceSecret)
            .apply();
        return new JSONObject().put("configured",true).put("deviceId",deviceId);
    }

    private JSONObject deviceHealth() throws Exception {
        JSONObject j=new JSONObject();
        j.put("manufacturer",Build.MANUFACTURER);j.put("model",Build.MODEL);
        j.put("osVersion",Build.VERSION.RELEASE);j.put("api",Build.VERSION.SDK_INT);
        File data=getFilesDir();
        j.put("storageFreeBytes",data.getFreeSpace());j.put("storageTotalBytes",data.getTotalSpace());
        j.put("canRequestPackageInstalls",getPackageManager().canRequestPackageInstalls());
        j.put("appVersion",appVersion());
        j.put("securityPatch",Build.VERSION.SECURITY_PATCH);
        j.put("buildDisplay",Build.DISPLAY);
        try {
            j.put("management",DeviceOwnerPolicy.status(this));
        } catch (Exception e) {
            j.put("management",new JSONObject()
                .put("deviceOwner",DexterDeviceAdminReceiver.isDeviceOwner(this))
                .put("statusError",String.valueOf(e.getMessage())));
        }
        return j;
    }

    private String appVersion() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { return "unknown"; }
    }

    private JSONObject appInventory() throws Exception {
        JSONArray a=new JSONArray();
        PackageManager pm=getPackageManager();
        List<ApplicationInfo> apps=pm.getInstalledApplications(0);
        for(ApplicationInfo ai:apps){
            JSONObject j=new JSONObject();
            j.put("packageName",ai.packageName);
            j.put("label",String.valueOf(pm.getApplicationLabel(ai)));
            j.put("system",(ai.flags&ApplicationInfo.FLAG_SYSTEM)!=0);
            a.put(j);
            if(a.length()>=500)break;
        }
        return new JSONObject().put("apps",a).put("count",a.length());
    }

    private JSONObject launchApp(String pkg) throws Exception {
        Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null)throw new Exception("App is not launchable: "+pkg);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);
        return new JSONObject().put("launched",pkg);
    }

    private JSONObject installApk(JSONObject req, boolean selfUpdate) throws Exception {
        String url=req.optString("url","");
        if(url.length()==0)throw new Exception("APK URL is required.");
        boolean deviceOwner = DexterDeviceAdminReceiver.isDeviceOwner(this);
        if(!deviceOwner){
            throw new Exception("Dexter commercial shell requires Device Owner for managed installs.");
        }
        File apk=new File(getExternalCacheDir(),"dexter-install-"+System.currentTimeMillis()+".apk");
        download(url,apk);
        if(selfUpdate) verifySelfUpdateApk(apk);
        PackageInstaller installer=getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int sid=installer.createSession(params);
        PackageInstaller.Session session=installer.openSession(sid);
        try {
            try(OutputStream out=session.openWrite("base.apk",0,apk.length());
                InputStream in=new FileInputStream(apk)){
                byte[] buf=new byte[65536];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
                session.fsync(out);
            }
            Intent callback=new Intent(this,MainActivity.class);
            PendingIntent pi=PendingIntent.getActivity(this,sid,callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            session.commit(pi.getIntentSender());
        } finally {
            session.close();
        }
        return new JSONObject()
            .put("installSessionId",sid)
            .put("deviceOwnerPath",deviceOwner)
            .put("confirmationMayBeRequired",!deviceOwner);
    }

    private void verifySelfUpdateApk(File apk) throws Exception {
        PackageManager pm=getPackageManager();
        PackageInfo incoming;
        if(Build.VERSION.SDK_INT>=28) incoming=pm.getPackageArchiveInfo(apk.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
        else incoming=pm.getPackageArchiveInfo(apk.getAbsolutePath(),PackageManager.GET_SIGNATURES);
        if(incoming==null) throw new Exception("Downloaded APK could not be verified.");
        if(!getPackageName().equals(incoming.packageName)) throw new Exception("Self-update package name mismatch.");
        PackageInfo current;
        if(Build.VERSION.SDK_INT>=28) current=pm.getPackageInfo(getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        else current=pm.getPackageInfo(getPackageName(),PackageManager.GET_SIGNATURES);
        android.content.pm.Signature[] a;
        android.content.pm.Signature[] b;
        if(Build.VERSION.SDK_INT>=28){
            a=current.signingInfo.getApkContentsSigners();
            b=incoming.signingInfo.getApkContentsSigners();
        }else{
            a=current.signatures;
            b=incoming.signatures;
        }
        if(a==null||b==null||a.length!=b.length) throw new Exception("Self-update signing certificate mismatch.");
        java.util.HashSet<String> sa=new java.util.HashSet<>(),sb=new java.util.HashSet<>();
        for(android.content.pm.Signature s:a)sa.add(s.toCharsString());
        for(android.content.pm.Signature s:b)sb.add(s.toCharsString());
        if(!sa.equals(sb)) throw new Exception("Self-update signing certificate mismatch.");
        long incomingCode=Build.VERSION.SDK_INT>=28?incoming.getLongVersionCode():incoming.versionCode;
        long currentCode=Build.VERSION.SDK_INT>=28?current.getLongVersionCode():current.versionCode;
        if(incomingCode<=currentCode) throw new Exception("Self-update is not newer than the installed Dexter build.");
    }

    private JSONObject requestUninstall(String pkg) throws Exception {
        if(!DexterDeviceAdminReceiver.isDeviceOwner(this)) throw new Exception("Managed uninstall requires Device Owner.");
        PackageInstaller installer=getPackageManager().getPackageInstaller();
        Intent callback=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,pkg.hashCode(),callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        installer.uninstall(pkg,pi.getIntentSender());
        return new JSONObject().put("uninstallRequested",pkg).put("confirmationRequired",false);
    }

    private void complete(String id,boolean ok,JSONObject result,String error){
        try{
            JSONObject b=new JSONObject().put("action","device_job_complete").put("sessionId",UUID.randomUUID().toString())
                .put("jobId",id).put("status",ok?"completed":"failed");
            if(result!=null)b.put("result",result);if(error!=null)b.put("error",error);
            post(b);
        }catch(Exception ignored){}
    }

    private static String readAll(InputStream in)throws Exception{
        if(in==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))>0)out.write(b,0,n);return out.toString("UTF-8");
    }
    private static void download(String u,File f)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(60000);
        if(c.getResponseCode()<200||c.getResponseCode()>=300)throw new IOException("Download HTTP "+c.getResponseCode());
        try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))>0)out.write(b,0,n);}
    }

    private void pollDexterSend() throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(SEND_API).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(20000); c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");
        c.setRequestProperty("x-dexter-device-token",token());
        try(OutputStream os=c.getOutputStream()){os.write("{\"action\":\"list\"}".getBytes("UTF-8"));}
        InputStream is=(c.getResponseCode()>=200&&c.getResponseCode()<300)?c.getInputStream():c.getErrorStream();
        String raw=readAll(is);
        if(c.getResponseCode()<200||c.getResponseCode()>=300) throw new IOException("Dexter Send HTTP "+c.getResponseCode());
        JSONObject data=new JSONObject(raw.length()==0?"{}":raw);
        JSONArray messages=data.optJSONArray("messages"); if(messages==null)return;
        SharedPreferences prefs=getSharedPreferences("dexter_send",MODE_PRIVATE);
        for(int i=messages.length()-1;i>=0;i--){
            JSONObject m=messages.optJSONObject(i); if(m==null)continue;
            String id=m.optString("id",""); if(id.isEmpty()||!m.isNull("read_at"))continue;
            if(prefs.getBoolean("seen_"+id,false))continue;
            showDexterNotice(id,m.optString("body","New message from Dexters"));
            prefs.edit().putBoolean("seen_"+id,true).apply();
        }
    }

    private void showDexterNotice(String id,String body){
        Intent open=new Intent(this,DexterInboxActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,id.hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,SEND_CHANNEL):new Notification.Builder(this);
        b.setContentTitle("Dexters")
            .setContentText(body)
            .setStyle(new Notification.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pi);
        getSystemService(NotificationManager.class).notify(Math.abs(id.hashCode()),b.build());
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Dexter Business Phone",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Keeps the Dexter device-management agent connected.");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);

            NotificationChannel send=new NotificationChannel(SEND_CHANNEL,"Dexters Messages",NotificationManager.IMPORTANCE_HIGH);
            send.setDescription("Order updates and offers from Dexters.");
            getSystemService(NotificationManager.class).createNotificationChannel(send);
        }
    }
    private Notification buildNotification(String text){
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setContentTitle("Dexter Business Phone").setContentText(text).setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true);
        return b.build();
    }
}
