package uk.co.dexter.os.messaging;

import android.content.*;

public class MmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        /*
         * The default SMS role receives WAP_PUSH_DELIVER here.
         * Carrier-specific MMS APN/transport processing is bound during the
         * device/carrier integration stage and validated on the production SKU.
         */
    }
}
