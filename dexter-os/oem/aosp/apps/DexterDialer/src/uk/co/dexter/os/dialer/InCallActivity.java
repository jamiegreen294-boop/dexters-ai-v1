package uk.co.dexter.os.dialer;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.telecom.*;
import android.view.*;
import android.widget.*;

public class InCallActivity extends Activity {
    private LinearLayout root;
    private boolean muted=false;
    private boolean speaker=false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        build();
    }

    @Override public void onResume() {
        super.onResume();
        refresh();
    }

    private Button button(String text, View.OnClickListener click) {
        Button b=new Button(this);
        b.setText(text);
        b.setOnClickListener(click);
        root.addView(b,new LinearLayout.LayoutParams(-1,96));
        return b;
    }

    private void build() {
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28,36,28,36);
        root.setBackgroundColor(Color.rgb(3,6,8));

        TextView title=new TextView(this);
        title.setId(android.R.id.text1);
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,120));

        button("Answer",v->{Call c=DexterInCallService.current;if(c!=null)c.answer(0);refresh();});
        button("Hold / Resume",v->{Call c=DexterInCallService.current;if(c!=null){if(c.getState()==Call.STATE_HOLDING)c.unhold();else c.hold();}refresh();});
        button("Mute",v->{muted=!muted;DexterInCallService.mute(muted);});
        button("Speaker",v->{speaker=!speaker;DexterInCallService.speaker(speaker);});
        button("End Call",v->{Call c=DexterInCallService.current;if(c!=null)c.disconnect();finish();});
        setContentView(root);
        refresh();
    }

    private void refresh() {
        TextView t=findViewById(android.R.id.text1);
        Call c=DexterInCallService.current;
        if(t==null)return;
        if(c==null){t.setText("Dexter Phone");return;}
        String number="Call";
        try{
            if(c.getDetails()!=null&&c.getDetails().getHandle()!=null)number=c.getDetails().getHandle().getSchemeSpecificPart();
        }catch(Exception ignored){}
        t.setText("Dexter Call\n"+number);
    }
}
