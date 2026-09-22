package uk.co.dextersspot.dexterai;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.euicc.EuiccManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public final class DexterEsimClient {
    private static final String API="https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/ai-command-centre";
    private DexterEsimClient(){}

    public static JSONObject localCapability(Context c){
        JSONObject out=new JSONObject();
        try{
            boolean feature=c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_EUICC);
            EuiccManager em=(EuiccManager)c.getSystemService(Context.EUICC_SERVICE);
            boolean enabled=feature && em!=null && em.isEnabled();
            out.put("euiccSupported",feature);
            out.put("euiccEnabled",enabled);
            out.put("manufacturer",Build.MANUFACTURER);
            out.put("model",Build.MODEL);
            out.put("android",Build.VERSION.RELEASE);
            out.put("activeEmbeddedProfiles",embeddedProfiles(c));
        }catch(Exception e){
            out.put("euiccSupported",false);
            out.put("euiccEnabled",false);
            out.put("error",String.valueOf(e.getMessage()));
        }
        return out;
    }

    public static JSONObject report(Context c) throws Exception{
        JSONObject cap=localCapability(c);
        JSONObject body=new JSONObject()
            .put("euiccSupported",cap.optBoolean("euiccSupported"))
            .put("euiccEnabled",cap.optBoolean("euiccEnabled"))
            .put("metadata",cap);
        return post(c,"esim_device_report",body);
    }

    public static JSONObject bundle(Context c) throws Exception{
        return post(c,"esim_device_bundle",new JSONObject());
    }

    private static JSONArray embeddedProfiles(Context c){
        JSONArray a=new JSONArray();
        try{
            SubscriptionManager sm=(SubscriptionManager)c.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            List<SubscriptionInfo> list=sm==null?null:sm.getAvailableSubscriptionInfoList();
            if(list!=null)for(SubscriptionInfo s:list){
                if(!s.isEmbedded())continue;
                a.put(new JSONObject()
                    .put("displayName",String.valueOf(s.getDisplayName()))
                    .put("carrierName",String.valueOf(s.getCarrierName()))
                    .put("subscriptionId",s.getSubscriptionId())
                    .put("number",s.getNumber()==null?"":s.getNumber()));
            }
        }catch(Exception ignored){}
        return a;
    }

    private static JSONObject post(Context c,String action,JSONObject body)throws Exception{
        String token=DeviceAgentService.getDeviceToken(c);
        if(token==null||token.length()<32)throw new IOException("Dexter device is not paired.");
        body.put("action",action).put("sessionId",UUID.randomUUID().toString());
        HttpURLConnection h=(HttpURLConnection)new URL(API).openConnection();
        h.setConnectTimeout(10000);h.setReadTimeout(20000);h.setRequestMethod("POST");h.setDoOutput(true);
        h.setRequestProperty("Content-Type","application/json");
        h.setRequestProperty("x-dexter-device-token",token);
        try(OutputStream os=h.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=h.getResponseCode();
        InputStream is=(code>=200&&code<300)?h.getInputStream():h.getErrorStream();
        String text=readAll(is);
        JSONObject r=new JSONObject(text==null||text.isEmpty()?"{}":text);
        if(code<200||code>=300)throw new IOException(r.optString("error","eSIM request failed."));
        return r;
    }

    private static String readAll(InputStream is)throws IOException{
        if(is==null)return "";
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
        while((n=is.read(b))!=-1)out.write(b,0,n);
        return out.toString("UTF-8");
    }
}
