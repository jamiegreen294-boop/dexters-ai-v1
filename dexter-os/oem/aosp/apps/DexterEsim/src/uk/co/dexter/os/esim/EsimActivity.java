package uk.co.dexter.os.esim;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.euicc.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class EsimActivity extends Activity {
    private EuiccManager euicc;
    private LinearLayout root;
    private TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        euicc=(EuiccManager)getSystemService(Context.EUICC_SERVICE);
        buildUi();
        refresh();
    }

    private TextView text(String s,int sp) {
        TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(Color.WHITE);v.setPadding(0,12,0,12);return v;
    }

    private void buildUi() {
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(34,48,34,40);root.setBackgroundColor(Color.rgb(3,6,8));
        root.addView(text("Dexter eSIM",30));
        status=text("",15);status.setTextColor(Color.LTGRAY);root.addView(status);

        EditText code=new EditText(this);code.setHint("Activation code / SM-DP+ code");code.setTextColor(Color.WHITE);code.setHintTextColor(Color.GRAY);code.setSingleLine(true);root.addView(code);
        Button add=new Button(this);add.setText("Add eSIM");
        add.setOnClickListener(v->download(code.getText().toString().trim()));
        root.addView(add);

        Button refresh=new Button(this);refresh.setText("Refresh profiles");refresh.setOnClickListener(v->refresh());root.addView(refresh);
        scroll.addView(root);setContentView(scroll);
    }

    private void download(String activationCode) {
        if(euicc==null || !euicc.isEnabled()){status.setText("eSIM hardware is not available or is disabled.");return;}
        if(activationCode.isEmpty()){status.setText("Enter or scan a carrier activation code.");return;}
        try {
            DownloadableSubscription sub=DownloadableSubscription.forActivationCode(activationCode);
            Intent i=new Intent(this,EsimResultReceiver.class).setAction("uk.co.dexter.os.esim.DOWNLOAD_RESULT");
            PendingIntent pi=PendingIntent.getBroadcast(this,1001,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
            euicc.downloadSubscription(sub,true,pi);
            status.setText("Downloading eSIM profile…");
        } catch(Exception e){status.setText("Unable to start eSIM download: "+e.getMessage());}
    }

    private void refresh() {
        if(euicc==null){status.setText("No eSIM service.");return;}
        boolean supported=getPackageManager().hasSystemFeature("android.hardware.telephony.euicc");
        status.setText(supported && euicc.isEnabled() ? "eSIM ready" : "eSIM unavailable on this hardware");
        if(!supported)return;
        try {
            SubscriptionManager sm=(SubscriptionManager)getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            List<SubscriptionInfo> list=sm.getAvailableSubscriptionInfoList();
            if(list!=null) for(SubscriptionInfo s:list){
                if(!s.isEmbedded()) continue;
                String label=String.valueOf(s.getDisplayName());
                TextView row=text("eSIM • "+label+" • "+(s.getNumber()==null?"":s.getNumber()),17);
                root.addView(row);
            }
        } catch(Exception ignored){}
    }
}
