package uk.co.dexter.os.dialer;

import android.content.*;
import android.telecom.*;

public class DexterInCallService extends InCallService {
    static volatile Call current;
    static volatile DexterInCallService instance;

    @Override public void onCreate() {
        super.onCreate();
        instance=this;
    }

    @Override public void onDestroy() {
        if(instance==this) instance=null;
        super.onDestroy();
    }

    @Override public void onCallAdded(Call c) {
        super.onCallAdded(c);
        current=c;
        Intent i=new Intent(this,InCallActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    @Override public void onCallRemoved(Call c) {
        if(current==c) current=null;
        super.onCallRemoved(c);
    }

    static void mute(boolean value) {
        DexterInCallService s=instance;
        if(s!=null) s.setMuted(value);
    }

    @SuppressWarnings("deprecation")
    static void speaker(boolean value) {
        DexterInCallService s=instance;
        if(s!=null) s.setAudioRoute(value ? CallAudioState.ROUTE_SPEAKER : CallAudioState.ROUTE_EARPIECE);
    }
}
