package uk.co.dexter.os.messaging;

import android.content.*;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import java.util.*;

public class SmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        try {
            SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(i);
            if(messages==null)return;
            for(SmsMessage m:messages) {
                ContentValues v=new ContentValues();
                v.put(Telephony.Sms.ADDRESS,m.getOriginatingAddress());
                v.put(Telephony.Sms.BODY,m.getMessageBody());
                v.put(Telephony.Sms.DATE,m.getTimestampMillis());
                v.put(Telephony.Sms.READ,0);
                v.put(Telephony.Sms.SEEN,0);
                c.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI,v);
            }
        } catch(Exception ignored) {}
    }
}
