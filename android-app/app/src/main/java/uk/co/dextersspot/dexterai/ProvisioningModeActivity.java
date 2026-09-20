package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;

public class ProvisioningModeActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Intent result = new Intent();
        result.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_MODE,
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        );
        result.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS,
            true
        );

        setResult(RESULT_OK, result);
        finish();
    }
}
