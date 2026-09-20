package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PolicyComplianceActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Keep Android Enterprise provisioning completion deliberately minimal.
        // Returning RESULT_OK with an explicit result Intent lets Setup Wizard
        // finish provisioning before Dexter applies policies or starts services.
        Intent result = new Intent();
        setResult(RESULT_OK, result);
        finish();
    }
}
