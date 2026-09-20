package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.os.Bundle;

public class PolicyComplianceActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        DeviceOwnerPolicy.applySafeDefaults(this);
        DeviceAgentService.start(this);
        setResult(RESULT_OK);
        finish();
    }
}
