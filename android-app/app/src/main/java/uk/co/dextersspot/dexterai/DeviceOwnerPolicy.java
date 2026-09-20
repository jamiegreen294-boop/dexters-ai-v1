package uk.co.dextersspot.dexterai;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;
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

    // Safe defaults only: identify the organisation and protect the DPC itself.
    public static void applySafeDefaults(Context c) {
        if(!isOwner(c)) return;
        DevicePolicyManager d=dpm(c);
        ComponentName a=admin(c);
        try { d.setOrganizationName(a, "Dexters"); } catch(Exception ignored) {}
        try { d.setUninstallBlocked(a, c.getPackageName(), true); } catch(Exception ignored) {}
        try { d.addUserRestriction(a, UserManager.DISALLOW_ADD_USER); } catch(Exception ignored) {}
        try { d.addUserRestriction(a, UserManager.DISALLOW_SAFE_BOOT); } catch(Exception ignored) {}
        try { d.setAutoTimeRequired(a, true); } catch(Exception ignored) {}
    }

    public static JSONObject status(Context c) throws Exception {
        JSONObject j=new JSONObject();
        DevicePolicyManager d=dpm(c);
        boolean owner=isOwner(c);
        j.put("deviceOwner", owner);
        j.put("activeAdmin", d!=null && d.isAdminActive(admin(c)));
        j.put("organization", owner ? String.valueOf(d.getDeviceOwnerOrganizationName()) : "");
        j.put("silentManagementAvailable", owner);
        return j;
    }

    public static JSONObject applyBusinessMode(Context c) throws Exception {
        if(!isOwner(c)) throw new IllegalStateException("Dexter is not Device Owner.");
        DevicePolicyManager d=dpm(c); ComponentName a=admin(c);
        applySafeDefaults(c);
        d.addUserRestriction(a, UserManager.DISALLOW_ADD_USER);
        d.addUserRestriction(a, UserManager.DISALLOW_REMOVE_USER);
        d.addUserRestriction(a, UserManager.DISALLOW_FACTORY_RESET);
        d.addUserRestriction(a, UserManager.DISALLOW_CONFIG_DATE_TIME);
        d.setUninstallBlocked(a, c.getPackageName(), true);
        JSONObject j=status(c);
        j.put("businessMode", true);
        return j;
    }
}
