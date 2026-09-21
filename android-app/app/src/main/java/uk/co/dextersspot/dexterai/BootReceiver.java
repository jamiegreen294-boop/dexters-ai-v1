package uk.co.dextersspot.dexterai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (DeviceAgentService.hasToken(context) || DexterDeviceAdminReceiver.isDeviceOwner(context)) {
            DeviceAgentService.start(context);
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            try {
                Intent splash=new Intent(context,DexterBootSplashActivity.class);
                splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NO_HISTORY);
                context.startActivity(splash);
            } catch(Exception ignored) {}
        }
    }
}
