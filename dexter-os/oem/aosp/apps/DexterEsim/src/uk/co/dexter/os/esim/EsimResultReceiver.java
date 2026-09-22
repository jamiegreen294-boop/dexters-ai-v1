package uk.co.dexter.os.esim;
import android.content.*;
import android.telephony.euicc.EuiccManager;

public class EsimResultReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        int result=getResultCode();
        Intent ui=new Intent(c,EsimActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ui.putExtra("result",result);
        c.startActivity(ui);
    }
}
