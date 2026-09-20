package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import java.util.ArrayList;

public class ProvisioningModeActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent result = new Intent();
        int mode = DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE;
        ArrayList<Integer> allowed = getIntent().getIntegerArrayListExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES);
        if (allowed != null && !allowed.isEmpty() &&
            !allowed.contains(DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)) {
            mode = allowed.get(0);
        }
        result.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, mode);
        result.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, true);
        setResult(RESULT_OK, result);
        finish();
    }
}
