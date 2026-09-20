package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PolicyComplianceActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Intent result = new Intent();
        setResult(RESULT_OK, result);
        finish();
    }
}
