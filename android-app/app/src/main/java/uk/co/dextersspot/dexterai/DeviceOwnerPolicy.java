package uk.co.dextersspot.dexterai;

import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.UserManager;
import org.json.JSONArray;
import org.json.JSONObject;

public final class DeviceOwnerPolicy {
    private DeviceOwnerPolicy() {}

    private static DevicePolicyManager dpm(Context c) {
        return (DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }
    private static ComponentName admin(Context c) {
        return DexterDeviceAdminReceiver.component(c);
    }
    public static boolean isOwner(Context c) {
        DevicePolicyManager d=dpm(c);
        return d!=null && d.isDeviceOwnerApp(c.getPackageName());
    }

    // Safe company-device defaults. High-impact actions such as wipe are never
    // performed here; they require a separately queued, approval-gated command.
    public static void applySafeDefaults(Context c) {
        if(!isOwner(c)) return;
        DevicePolicyManager d=dpm(c);
        ComponentName a=admin(c);
        try { d.setOrganizationName(a, "Dexters"); } catch(Exception ignored) {}
        try { d.setDeviceOwnerLockScreenInfo(a, "Dexters Business Phone · Managed by Dexter AI · Return to Dexters if found"); } catch(Exception ignored) {}
        try { d.setUninstallBlocked(a, c.getPackageName(), true); } catch(Exception ignored) {}
        try { d.addUserRestriction(a, UserManager.DISALLOW_ADD_USER); } catch(Exception ignored) {}
        try { d.addUserRestriction(a, UserManager.DISALLOW_SAFE_BOOT); } catch(Exception ignored) {}
        try { d.addUserRestriction(a, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES); } catch(Exception ignored) {}
        if(Build.VERSION.SDK_INT>=26){
            try { d.addUserRestriction(a, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY); } catch(Exception ignored) {}
        }
        try { d.setAutoTimeRequired(a, true); } catch(Exception ignored) {}
        try { d.setPasswordQuality(a, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX); } catch(Exception ignored) {}
        try { d.setPasswordMinimumLength(a, 6); } catch(Exception ignored) {}
        try { d.setMaximumTimeToLock(a, 120000L); } catch(Exception ignored) {}
    }

    public static JSONObject status(Context c) throws Exception {
        JSONObject j=new JSONObject();
        DevicePolicyManager d=dpm(c);
        boolean owner=isOwner(c);
        j.put("deviceOwner", owner);
        j.put("activeAdmin", d!=null && d.isAdminActive(admin(c)));
        j.put("organization", owner ? String.valueOf(d.getOrganizationName(admin(c))) : "");
        j.put("silentManagementAvailable", owner);
        j.put("securityPatch", Build.VERSION.SECURITY_PATCH);
        j.put("sdk", Build.VERSION.SDK_INT);
        j.put("build", Build.DISPLAY);
        j.put("maxIdleLockMs", 120000);
        j.put("minimumPinLength", 6);
        j.put("recoveryRoute", "Dexter Home > Dexter Admin (device credential protected)");
        return j;
    }

    public static JSONObject applyBusinessMode(Context c) throws Exception {
        if(!isOwner(c)) throw new IllegalStateException("Dexter is not Device Owner.");
        DevicePolicyManager d=dpm(c); ComponentName a=admin(c);
        applySafeDefaults(c);

        JSONObject applied=new JSONObject();
        JSONObject skipped=new JSONObject();

        applyRestriction(d,a,UserManager.DISALLOW_ADD_USER,"disallowAddUser",applied,skipped);
        applyRestriction(d,a,UserManager.DISALLOW_REMOVE_USER,"disallowRemoveUser",applied,skipped);
        applyRestriction(d,a,UserManager.DISALLOW_FACTORY_RESET,"disallowFactoryReset",applied,skipped);
        applyRestriction(d,a,UserManager.DISALLOW_CONFIG_DATE_TIME,"disallowConfigDateTime",applied,skipped);
        applyRestriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,"disallowUnknownSources",applied,skipped);
        if(Build.VERSION.SDK_INT>=26)
            applyRestriction(d,a,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,"disallowUnknownSourcesGlobally",applied,skipped);

        try { d.setUninstallBlocked(a,c.getPackageName(),true); applied.put("protectDexterApp",true); }
        catch(Exception e){ skipped.put("protectDexterApp",safeMessage(e)); }

        try {
            d.setSystemUpdatePolicy(a,SystemUpdatePolicy.createWindowedInstallPolicy(120,300));
            applied.put("systemUpdates","02:00-05:00");
        } catch(Exception e){ skipped.put("systemUpdates",safeMessage(e)); }

        try {
            IntentFilter home=new IntentFilter(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addCategory(Intent.CATEGORY_DEFAULT);
            d.addPersistentPreferredActivity(a,home,new ComponentName(c,DexterHomeActivity.class));
            applied.put("dexterHomePreferred",true);
        } catch(Exception e){ skipped.put("dexterHomePreferred",safeMessage(e)); }

        JSONObject j=new JSONObject();
        j.put("deviceOwner",true);
        j.put("businessMode",true);
        j.put("applied",applied);
        j.put("skipped",skipped);
        j.put("partial",skipped.length()>0);
        j.put("status",status(c));
        return j;
    }

    private static void applyRestriction(DevicePolicyManager d,ComponentName a,String restriction,String label,JSONObject applied,JSONObject skipped){
        try { d.addUserRestriction(a,restriction); applied.put(label,true); }
        catch(Exception e){ try { skipped.put(label,safeMessage(e)); } catch(Exception ignored){} }
    }

    private static String safeMessage(Exception e){
        String m=e.getMessage();
        return m==null?e.getClass().getSimpleName():m;
    }

    public static JSONObject lockNow(Context c) throws Exception {
        if(!isOwner(c)) throw new IllegalStateException("Dexter is not Device Owner.");
        dpm(c).lockNow();
        return new JSONObject().put("locked",true);
    }

    public static JSONObject wipeDevice(Context c, String reason) throws Exception {
        if(!isOwner(c)) throw new IllegalStateException("Dexter is not Device Owner.");
        // This is intentionally only reachable through an explicit approval-gated job.
        JSONObject j=new JSONObject().put("wipeRequested",true).put("reason",reason==null?"":reason);
        dpm(c).wipeData(0);
        return j;
    }

    public static JSONObject setInternetProtection(Context c, String hostname) throws Exception {
        if(!isOwner(c)) throw new IllegalStateException("Dexter is not Device Owner.");
        DevicePolicyManager d=dpm(c); ComponentName a=admin(c);
        String h=(hostname==null||hostname.trim().isEmpty())?"security.cloudflare-dns.com":hostname.trim();
        if(Build.VERSION.SDK_INT>=29){
            int result=d.setGlobalPrivateDnsModeSpecifiedHost(a,h);
            return new JSONObject().put("privateDns","specified").put("hostname",h).put("result",result);
        }
        throw new UnsupportedOperationException("Private DNS policy requires Android 10+.");
    }

    public static JSONObject clearInternetProtection(Context c) throws Exception {
        if(!isOwner(c)) throw new IllegalStateException("Dexter is not Device Owner.");
        DevicePolicyManager d=dpm(c); ComponentName a=admin(c);
        if(Build.VERSION.SDK_INT>=29){
            int result=d.setGlobalPrivateDnsModeOpportunistic(a);
            return new JSONObject().put("privateDns","opportunistic").put("result",result);
        }
        throw new UnsupportedOperationException("Private DNS policy requires Android 10+.");
    }

    public static JSONObject releaseLauncher(Context c) throws Exception {
        if(!isOwner(c)) throw new IllegalStateException("Dexter is not Device Owner.");
        dpm(c).clearPackagePersistentPreferredActivities(admin(c), c.getPackageName());
        return new JSONObject().put("launcherReleased",true);
    }

    public static JSONObject protectApps(Context c, JSONArray packages) throws Exception {
        if(!isOwner(c)) throw new IllegalStateException("Dexter is not Device Owner.");
        DevicePolicyManager d=dpm(c); ComponentName a=admin(c);
        JSONArray done=new JSONArray();
        if(packages!=null){
            for(int i=0;i<packages.length();i++){
                String pkg=packages.optString(i,"").trim();
                if(pkg.isEmpty()) continue;
                try { d.setUninstallBlocked(a,pkg,true); done.put(pkg); } catch(Exception ignored) {}
            }
        }
        return new JSONObject().put("uninstallProtected",done);
    }

    public static JSONObject restoreStandardConfiguration(Context c) throws Exception {
        JSONObject business=applyBusinessMode(c);
        JSONObject internet;
        try { internet=setInternetProtection(c,"security.cloudflare-dns.com"); }
        catch(Exception e){ internet=new JSONObject().put("error",e.getMessage()); }
        JSONArray packages=new JSONArray();
        packages.put(c.getPackageName());
        packages.put("com.google.android.gm");
        packages.put("com.whatsapp.w4b");
        packages.put("com.android.chrome");
        packages.put("com.google.android.apps.maps");
        JSONObject protectedApps=protectApps(c,packages);
        return new JSONObject()
            .put("restored",true)
            .put("businessMode",business)
            .put("internet",internet)
            .put("apps",protectedApps);
    }

    public static JSONObject configurationSnapshot(Context c) throws Exception {
        JSONObject j=status(c);
        j.put("packageName",c.getPackageName());
        j.put("businessModeCapable",isOwner(c));
        j.put("recommendedPrivateDns","security.cloudflare-dns.com");
        j.put("launcher","DexterHomeActivity");
        j.put("systemUpdateWindow","02:00-05:00");
        j.put("retention","phone jobs 90 days; audit logs 365 days");
        return j;
    }
}
