package uk.co.dexter.os.setup;

import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.concurrent.Executor;

public class SetupActivity extends Activity {
    private LinearLayout root;
    private TextView state;
    private Executor main;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        main=getMainExecutor();
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36,56,36,36);
        root.setBackgroundColor(Color.rgb(3,6,8));

        TextView title=new TextView(this);
        title.setText("Welcome to Dexter");
        title.setTextColor(Color.WHITE);
        title.setTextSize(32);
        root.addView(title);

        TextView body=new TextView(this);
        body.setText("Dexter Setup prepares the phone, assigns Dexter system roles and completes the first boot.");
        body.setTextColor(Color.LTGRAY);
        body.setTextSize(17);
        body.setPadding(0,18,0,24);
        root.addView(body);

        state=new TextView(this);
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
            state.setText("Applying Dexter defaults…");
            RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);
            if (rm!=null) {
                assign(rm,RoleManager.ROLE_DIALER,"uk.co.dexter.os.dialer");
                assign(rm,RoleManager.ROLE_SMS,"uk.co.dexter.os.messaging");
                assign(rm,RoleManager.ROLE_HOME,"uk.co.dextersspot.dexterai");
            }
            Settings.Secure.putInt(getContentResolver(),Settings.Secure.USER_SETUP_COMPLETE,1);
            Settings.Global.putInt(getContentResolver(),Settings.Global.DEVICE_PROVISIONED,1);
            state.setText("Dexter setup complete.");
            Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
        } catch(Exception e) {
            state.setText("Setup could not finish: "+e.getMessage());
        }
    }

    private void assign(RoleManager rm,String role,String pkg) {
        try {
            if(!rm.isRoleAvailable(role)) return;
            for(String holder:rm.getRoleHolders(role)) {
                if(!pkg.equals(holder)) rm.removeRoleHolder(role,holder,0,main,ok->{});
            }
            rm.addRoleHolder(role,pkg,RoleManager.MANAGE_HOLDERS_FLAG_DONT_KILL_APP,main,ok->{});
        } catch(Exception ignored) {}
    }
}
