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
        // Do not start foreground/background work while Setup Wizard is
        // still provisioning the device. MainActivity/BootReceiver starts
        // the agent once Android setup has completed.
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        // Device Owner has now been assigned. Apply only safe synchronous
        // policy defaults here; defer service startup until after Setup Wizard.
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
