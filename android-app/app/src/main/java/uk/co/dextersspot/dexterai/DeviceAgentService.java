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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;

    public static boolean hasToken(Context c) {
        return c.getSharedPreferences("dexter_device", MODE_PRIVATE).getString("device_token", "").length() >= 32;
    }
    public static void start(Context c) {
        Intent i = new Intent(c, DeviceAgentService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Dexter device agent connected"));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) { running = true; new Thread(this::loop, "dexter-phone-agent").start(); }
        return START_STICKY;
    }

    @Override public void onDestroy() { running = false; super.onDestroy(); }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }

    private void loop() {
        while (running) {
            try {
                heartbeat();
                pollJobs();
            } catch (Exception ignored) {}
            try { Thread.sleep(30000); } catch (InterruptedException e) { return; }
        }
    }

    private String token() {
        return getSharedPreferences("dexter_device", MODE_PRIVATE).getString("device_token", "");
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
        info.put("appVersion",BuildConfig.VERSION_NAME);
        JSONObject cap=new JSONObject();
        cap.put("deviceHealth",true);cap.put("appsInventory",true);cap.put("appLaunch",true);
        cap.put("apkInstall",true);cap.put("appUninstall",true);cap.put("silentInstall",false);
        cap.put("canRequestPackageInstalls",getPackageManager().canRequestPackageInstalls());
        cap.put("wirelessDebugging",Build.VERSION.SDK_INT>=30);
        info.put("capabilities",cap);
        post(new JSONObject().put("action","device_heartbeat").put("sessionId",UUID.randomUUID().toString()).put("info",info));
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
            case "apps.inventory": return appInventory();
            case "app.launch": return launchApp(req.getString("packageName"));
            case "app.install":
            case "dexter.self_update": return installApk(req);
            case "app.uninstall": return requestUninstall(req.getString("packageName"));
            default: throw new IllegalArgumentException("Unsupported job: "+type);
        }
    }

    private JSONObject deviceHealth() throws Exception {
        JSONObject j=new JSONObject();
        j.put("manufacturer",Build.MANUFACTURER);j.put("model",Build.MODEL);
        j.put("osVersion",Build.VERSION.RELEASE);j.put("api",Build.VERSION.SDK_INT);
        File data=getFilesDir();
        j.put("storageFreeBytes",data.getFreeSpace());j.put("storageTotalBytes",data.getTotalSpace());
        j.put("canRequestPackageInstalls",getPackageManager().canRequestPackageInstalls());
        j.put("appVersion",BuildConfig.VERSION_NAME);
        return j;
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

    private JSONObject installApk(JSONObject req) throws Exception {
        String url=req.optString("url","");
        if(url.length()==0)throw new Exception("APK URL is required.");
        if(!getPackageManager().canRequestPackageInstalls()){
            Intent settings=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:"+getPackageName()));
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(settings);
            throw new Exception("Allow Dexter Business Phone to install unknown apps, then retry this job.");
        }
        File apk=new File(getExternalCacheDir(),"dexter-install-"+System.currentTimeMillis()+".apk");
        download(url,apk);
        PackageInstaller installer=getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int sid=installer.createSession(params);
        try(PackageInstaller.Session session=installer.openSession(sid);
            OutputStream out=session.openWrite("base.apk",0,apk.length());
            InputStream in=new FileInputStream(apk)){
            byte[] buf=new byte[65536];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
            session.fsync(out);
            Intent callback=new Intent(this,MainActivity.class);
            PendingIntent pi=PendingIntent.getActivity(this,sid,callback,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            session.commit(pi.getIntentSender());
        }
        return new JSONObject().put("installSessionId",sid).put("confirmationMayBeRequired",true);
    }

    private JSONObject requestUninstall(String pkg) {
        Intent i=new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+pkg));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);
        return new JSONObject().put("uninstallRequested",pkg).put("confirmationRequired",true);
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

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Dexter Business Phone",NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Keeps the Dexter device-management agent connected.");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
    private Notification buildNotification(String text){
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setContentTitle("Dexter Business Phone").setContentText(text).setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true);
        return b.build();
    }
}
