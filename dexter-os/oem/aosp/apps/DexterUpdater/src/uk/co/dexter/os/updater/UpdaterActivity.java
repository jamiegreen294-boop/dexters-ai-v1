package uk.co.dexter.os.updater;

import android.app.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UpdaterActivity extends Activity {
    private TextView status;
    private Button install;
    private JSONObject manifest;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(34,48,34,34);
        r.setBackgroundColor(Color.rgb(3,6,8));

        TextView t=new TextView(this);t.setText("Dexter Update");t.setTextColor(Color.WHITE);t.setTextSize(30);r.addView(t);
        status=new TextView(this);status.setText("Stable channel · signed OTA only");status.setTextColor(Color.LTGRAY);status.setTextSize(17);status.setPadding(0,18,0,18);r.addView(status);

        Button check=new Button(this);check.setText("Check for update");check.setOnClickListener(v->check());r.addView(check);
        install=new Button(this);install.setText("Install update");install.setEnabled(false);install.setOnClickListener(v->install());r.addView(install);
        setContentView(r);
    }

    private void check(){
        status.setText("Checking…");
        new Thread(()->{
            try{
                String endpoint=android.os.SystemProperties.get("ro.dexter.ota.manifest","");
                if(endpoint.isEmpty())throw new IOException("OTA manifest endpoint is not provisioned for this hardware.");
                HttpURLConnection h=(HttpURLConnection)new URL(endpoint).openConnection();
                h.setConnectTimeout(10000);h.setReadTimeout(20000);
                String json=read(h.getInputStream());
                JSONObject m=new JSONObject(json);
                String product=m.getString("product"),payload=m.getString("payload"),sha=m.getString("sha256");
                long size=m.getLong("size");
                if(!product.toLowerCase().contains("dexter")||payload.isEmpty()||sha.length()!=64||size<=0)throw new IOException("Invalid Dexter OTA manifest.");
                manifest=m;
                runOnUiThread(()->{status.setText("Update available: "+m.optString("version","unknown"));install.setEnabled(true);});
            }catch(Exception e){runOnUiThread(()->status.setText(e.getMessage()));}
        },"DexterUpdateCheck").start();
    }

    private void install(){
        JSONObject m=manifest;if(m==null)return;
        install.setEnabled(false);
        status.setText("Handing signed payload to Android update engine…");
        new Thread(()->{
            try{
                android.os.UpdateEngine engine=new android.os.UpdateEngine();
                engine.bind(new android.os.UpdateEngineCallback(){
                    public void onStatusUpdate(int s,float p){runOnUiThread(()->status.setText("Installing "+Math.round(p*100)+"%"));}
                    public void onPayloadApplicationComplete(int e){runOnUiThread(()->status.setText(e==0?"Update installed. Restart to finish.":"Update failed: "+e));}
                });
                String payload=m.getString("payload");
                long offset=m.optLong("offset",0),size=m.getLong("size");
                org.json.JSONArray a=m.optJSONArray("payloadProperties");
                String[] headers=new String[a==null?0:a.length()];
                if(a!=null)for(int i=0;i<a.length();i++)headers[i]=a.getString(i);
                engine.applyPayload(payload,offset,size,headers);
            }catch(Exception e){runOnUiThread(()->{status.setText("Update could not start: "+e.getMessage());install.setEnabled(true);});}
        },"DexterUpdateApply").start();
    }

    private static String read(InputStream is)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
        while((n=is.read(b))!=-1)out.write(b,0,n);
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
