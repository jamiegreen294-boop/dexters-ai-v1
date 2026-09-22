package uk.co.dexter.os.messaging;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.telephony.SmsManager;

public class RespondViaMessageService extends IntentService {
    public RespondViaMessageService(){super("DexterRespondViaMessage");}

    @Override protected void onHandleIntent(Intent i) {
        if(i==null)return;
        Uri data=i.getData();
        String message=i.getStringExtra(Intent.EXTRA_TEXT);
        if(data==null||message==null)return;
        String number=data.getSchemeSpecificPart();
        if(number==null||number.isEmpty())return;
        SmsManager.getDefault().sendTextMessage(number,null,message,null,null);
    }
}
