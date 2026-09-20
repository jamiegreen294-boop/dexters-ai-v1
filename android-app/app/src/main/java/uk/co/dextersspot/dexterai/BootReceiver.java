package uk.co.dextersspot.dexterai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DeviceAgentService.hasToken(context) || DexterDeviceAdminReceiver.isDeviceOwner(context)) {
            DeviceAgentService.start(context);
        }
    }
}
