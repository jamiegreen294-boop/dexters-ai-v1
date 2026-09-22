package uk.co.dextersspot.dexterai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class DexterInboxActivity extends Activity {
    private static final String API="https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/dexter-send-preview";
    private final int bg=Color.rgb(10,14,13), card=Color.rgb(28,33,31), text=Color.WHITE, muted=Color.rgb(185,191,188), accent=Color.rgb(239,139,34);
    private LinearLayout list;
    private TextView status;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        render();
        load();
    }

    private void render(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(18),dp(18),dp(28));
        root.setBackgroundColor(bg);
        scroll.addView(root);

        TextView title=new TextView(this);
        title.setText("Dexter Messages");
        title.setTextColor(text);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        root.addView(title);

        TextView sub=new TextView(this);
        sub.setText("Order updates and offers from Dexters");
        sub.setTextColor(muted);
        sub.setTextSize(14);
        sub.setPadding(0,dp(5),0,dp(14));
        root.addView(sub);

        status=new TextView(this);
        status.setText("Loading…");
        status.setTextColor(muted);
        status.setPadding(0,0,0,dp(12));
        root.addView(status);

        list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list,new LinearLayout.LayoutParams(-1,-2));

        setContentView(scroll);
    }

    private void load(){
        new Thread(()->{
            try{
                JSONObject data=post(new JSONObject().put("action","list"));
                JSONArray messages=data.optJSONArray("messages");
                runOnUiThread(()->show(messages));
            }catch(Exception e){
                runOnUiThread(()->status.setText("Could not load messages right now."));
            }
        },"dexter-inbox-load").start();
    }

    private void show(JSONArray messages){
        list.removeAllViews();
        if(messages==null || messages.length()==0){
            status.setText("No new notices.");
            return;
        }
        status.setText(messages.length()+" message"+(messages.length()==1?"":"s"));
        for(int i=0;i<messages.length();i++){
            JSONObject m=messages.optJSONObject(i);
            if(m==null) continue;
            final String id=m.optString("id","");
            final String deep=m.optString("deeplink","");
            String kind=m.optString("kind","system");
            String body=m.optString("body","");
            String when=m.optString("created_at","");
            boolean unread=m.isNull("read_at") || m.optString("read_at","").isEmpty();

            LinearLayout box=new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(16),dp(14),dp(16),dp(14));
            GradientDrawable d=new GradientDrawable();
            d.setColor(card); d.setCornerRadius(dp(18));
            if(unread) d.setStroke(dp(1),accent);
            box.setBackground(d);

            TextView sender=new TextView(this);
            sender.setText("Dexters  ·  "+("promotion".equals(kind)?"Offer":"Order update"));
            sender.setTextColor(accent);
            sender.setTextSize(12);
            sender.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            box.addView(sender);

            TextView msg=new TextView(this);
            msg.setText(body);
            msg.setTextColor(text);
            msg.setTextSize(16);
            msg.setPadding(0,dp(7),0,dp(7));
            box.addView(msg);

            if(!when.isEmpty()){
                TextView meta=new TextView(this);
                meta.setText(when.replace("T"," ").replace("Z"," UTC"));
                meta.setTextColor(muted);
                meta.setTextSize(11);
                box.addView(meta);
            }

            box.setOnClickListener(v->{
                markRead(id);
                if(!deep.isEmpty()){
                    try{
                        Uri u=Uri.parse(deep);
                        String s=u.getScheme();
                        if("http".equalsIgnoreCase(s)||"https".equalsIgnoreCase(s)){
                            Intent w=new Intent(this,DexterWebActivity.class);w.putExtra("url",deep);startActivity(w);
                        }
                    }catch(Exception ignored){}
                }
            });

            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,0,0,dp(10));
            list.addView(box,lp);
        }
    }

    private void markRead(String id){
        if(id==null||id.isEmpty())return;
        new Thread(()->{
            try{ post(new JSONObject().put("action","read").put("messageId",id)); }catch(Exception ignored){}
        },"dexter-message-read").start();
    }

    private JSONObject post(JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(20000); c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");
        c.setRequestProperty("x-dexter-device-token",DeviceAgentService.getDeviceToken(this));
        try(OutputStream os=c.getOutputStream()){ os.write(body.toString().getBytes("UTF-8")); }
        InputStream is=(c.getResponseCode()>=200&&c.getResponseCode()<300)?c.getInputStream():c.getErrorStream();
        String t=readAll(is);
        if(c.getResponseCode()<200||c.getResponseCode()>=300) throw new IOException("HTTP "+c.getResponseCode());
        return new JSONObject(t.isEmpty()?"{}":t);
    }

    private static String readAll(InputStream in)throws Exception{
        if(in==null)return "";
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] b=new byte[8192]; int n;
        while((n=in.read(b))>0)out.write(b,0,n);
        return out.toString("UTF-8");
    }

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
