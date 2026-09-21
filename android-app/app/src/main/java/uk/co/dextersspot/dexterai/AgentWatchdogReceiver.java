package uk.co.dextersspot.dexterai;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class AgentWatchdogReceiver extends BroadcastReceiver {
    private static final int REQUEST_CODE = 296;
    private static final long INTERVAL_MS = 5 * 60 * 1000L;

    @Override public void onReceive(Context context, Intent intent) {
        if (DeviceAgentService.hasToken(context) || DexterDeviceAdminReceiver.isDeviceOwner(context)) {
            try { DeviceAgentService.start(context); } catch (Exception ignored) {}
        }
        schedule(context);
    }

    public static void schedule(Context context) {
        try {
            Intent i = new Intent(context, AgentWatchdogReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                context, REQUEST_CODE, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                long at = SystemClock.elapsedRealtime() + INTERVAL_MS;
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
            }
        } catch (Exception ignored) {}
    }
}
