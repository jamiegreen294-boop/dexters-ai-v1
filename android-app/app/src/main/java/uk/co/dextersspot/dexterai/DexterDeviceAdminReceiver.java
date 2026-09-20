package uk.co.dextersspot.dexterai;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class DexterDeviceAdminReceiver extends DeviceAdminReceiver {
    public static ComponentName component(Context context) {
        return new ComponentName(context, DexterDeviceAdminReceiver.class);
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        // Defer service startup until Android Setup Wizard has completed.
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        DeviceOwnerPolicy.applySafeDefaults(context);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "Dexter Business Phone management protects this company-owned device.";
    }

    public static boolean isDeviceOwner(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
    }
}
