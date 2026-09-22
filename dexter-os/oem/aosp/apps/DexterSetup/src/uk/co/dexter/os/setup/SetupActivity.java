package uk.co.dexter.os.setup;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class SetupActivity extends Activity {
    private TextView state;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36,56,36,36);
        root.setBackgroundColor(Color.rgb(3,6,8));

        TextView title=new TextView(this);
        title.setText("Welcome to Dexter");
        title.setTextColor(Color.WHITE);
        title.setTextSize(32);
        root.addView(title);

        TextView body=new TextView(this);
        body.setText("This phone was provisioned with Dexter Home, Dexter Phone and Dexter Messages as system defaults. Setup completes the customer profile and first boot.");
        body.setTextColor(Color.LTGRAY);
        body.setTextSize(17);
        body.setPadding(0,18,0,24);
        root.addView(body);

        state=new TextView(this);
        state.setText("Factory role assignment must pass before this screen is released to a customer.");
        state.setTextColor(Color.WHITE);
        state.setTextSize(16);
        root.addView(state);

        Button finish=new Button(this);
        finish.setText("Finish setup");
        finish.setOnClickListener(v->finishSetup());
        root.addView(finish);
        setContentView(root);
    }

    private void finishSetup() {
        try {
            Settings.Secure.putInt(getContentResolver(),Settings.Secure.USER_SETUP_COMPLETE,1);
            Settings.Global.putInt(getContentResolver(),Settings.Global.DEVICE_PROVISIONED,1);
            state.setText("Dexter setup complete.");
            Intent home=new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
        } catch(Exception e) {
            state.setText("Setup could not finish: "+e.getMessage());
        }
    }
}
