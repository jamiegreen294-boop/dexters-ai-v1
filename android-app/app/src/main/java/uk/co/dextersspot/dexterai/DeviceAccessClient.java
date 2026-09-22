package uk.co.dextersspot.dexterai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class DeviceAccessClient {
    private static final String API="https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/ai-command-centre";
    private static final String PREF="dexter_device";
    private DeviceAccessClient(){}

    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public static JSONObject status(Context c) throws Exception {
        return post(c,"device_access_status",new JSONObject(),null);
    }

    public static JSONObject setupPin(Context c,String role,String pin) throws Exception {
        JSONObject b=new JSONObject().put("targetRole",role).put("pin",pin);
        return post(c,"device_access_setup_pin",b,session(c));
    }

    public static JSONObject login(Context c,String role,String pin) throws Exception {
        JSONObject b=new JSONObject().put("targetRole",role).put("pin",pin);
        JSONObject r=post(c,"device_access_login",b,null);
        if(r.optBoolean("ok",false)){
            String s=r.optString("accessSession","");
            String rr=r.optString("role","staff");
            String exp=r.optString("expiresAt","");
            prefs(c).edit().putString("access_session",s).putString("access_role_session",rr).putString("access_expires_at",exp).apply();
            DeviceOwnerPolicy.setAccessRole(c,rr);
        }
        return r;
    }

    public static JSONObject validate(Context c) throws Exception {
        JSONObject r=post(c,"device_access_validate",new JSONObject(),session(c));
        if(!r.optBoolean("valid",false)){
            clearLocal(c);
            try{DeviceOwnerPolicy.setAccessRole(c,"staff");}catch(Exception ignored){}
        }
        return r;
    }

    public static JSONObject logout(Context c) throws Exception {
        JSONObject r;
        try{r=post(c,"device_access_logout",new JSONObject(),session(c));}
        finally{
            clearLocal(c);
            try{DeviceOwnerPolicy.setAccessRole(c,"staff");}catch(Exception ignored){}
        }
        return r;
    }

    public static void enforceExpiry(Context c){
        String exp=prefs(c).getString("access_expires_at","");
        if(exp==null||exp.isEmpty())return;
        try{
            long when=java.time.Instant.parse(exp).toEpochMilli();
            if(System.currentTimeMillis()>=when){
                clearLocal(c);
                try{DeviceOwnerPolicy.setAccessRole(c,"staff");}catch(Exception ignored){}
            }
        }catch(Exception e){
            clearLocal(c);
            try{DeviceOwnerPolicy.setAccessRole(c,"staff");}catch(Exception ignored){}
        }
    }

    public static String session(Context c){return prefs(c).getString("access_session","");}
    public static String sessionRole(Context c){
        enforceExpiry(c);
        return prefs(c).getString("access_role_session","staff");
    }
    public static String expiresAt(Context c){return prefs(c).getString("access_expires_at","");}

    private static void clearLocal(Context c){
        prefs(c).edit().remove("access_session").remove("access_role_session").remove("access_expires_at").apply();
    }

    private static JSONObject post(Context c,String action,JSONObject body,String accessSession) throws Exception {
        String token=DeviceAgentService.getDeviceToken(c);
        if(token==null||token.length()<32)throw new IOException("Dexter device is not paired.");
        body.put("action",action).put("sessionId",java.util.UUID.randomUUID().toString());
        HttpURLConnection h=(HttpURLConnection)new URL(API).openConnection();
        h.setConnectTimeout(10000);h.setReadTimeout(20000);h.setRequestMethod("POST");h.setDoOutput(true);
        h.setRequestProperty("Content-Type","application/json");
        h.setRequestProperty("x-dexter-device-token",token);
        if(accessSession!=null&&!accessSession.isEmpty())h.setRequestProperty("x-dexter-access-session",accessSession);
        try(OutputStream os=h.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        InputStream is=(h.getResponseCode()>=200&&h.getResponseCode()<300)?h.getInputStream():h.getErrorStream();
        String text=readAll(is);
        JSONObject r=new JSONObject(text==null||text.isEmpty()?"{}":text);
        if(h.getResponseCode()<200||h.getResponseCode()>=300){
            String msg=r.optString("error","Access request failed.");
            throw new AccessException(h.getResponseCode(),msg,r);
        }
        return r;
    }

    private static String readAll(InputStream is)throws IOException{
        if(is==null)return "";
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
        while((n=is.read(b))!=-1)out.write(b,0,n);
        return out.toString("UTF-8");
    }

    public static final class AccessException extends IOException{
        public final int status; public final JSONObject payload;
        AccessException(int status,String message,JSONObject payload){super(message);this.status=status;this.payload=payload;}
    }
}
