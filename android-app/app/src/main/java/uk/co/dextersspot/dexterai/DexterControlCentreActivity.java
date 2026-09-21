package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class DexterControlCentreActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        Intent i=new Intent(this,DexterHomeActivity.class);
        i.putExtra("page","control");
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
