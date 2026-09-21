package uk.co.dextersspot.dexterai;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DexterHeroView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r=new RectF();

    public DexterHeroView(Context c){ super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight(),rad=22*getResources().getDisplayMetrics().density;
        r.set(0,0,w,h);

        LinearGradient g=new LinearGradient(0,0,w,h,
            new int[]{Color.rgb(13,31,43),Color.rgb(19,52,74),Color.rgb(9,18,25)},null,Shader.TileMode.CLAMP);
        p.setShader(g); c.drawRoundRect(r,rad,rad,p); p.setShader(null);

        p.setColor(Color.argb(45,255,255,255));
        c.drawRoundRect(r,rad,rad,p);

        // skyline silhouette
        float base=h*0.66f;
        p.setColor(Color.rgb(6,14,20));
        float x=w*0.48f;
        float[] widths={0.06f,0.055f,0.07f,0.05f,0.08f,0.055f};
        float[] heights={0.25f,0.38f,0.30f,0.52f,0.34f,0.64f};
        for(int i=0;i<widths.length;i++){
            float bw=w*widths[i], bh=h*heights[i];
            c.drawRect(x,base-bh,x+bw,base,p);
            // windows
            Paint wp=new Paint(Paint.ANTI_ALIAS_FLAG); wp.setColor(Color.rgb(218,170,78));
            float yy=base-bh+8;
            for(int rr=0;rr<6;rr++){
                float xx=x+6;
                for(int cc=0;cc<3;cc++) c.drawRect(xx+cc*8,yy,xx+cc*8+2,yy+2,wp);
                yy+=10;
            }
            x+=bw+w*0.008f;
        }

        // accent line
        p.setColor(Color.rgb(218,170,78)); c.drawRect(w*0.48f,base-2,w*0.97f,base,p);

        text.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        text.setColor(Color.WHITE); text.setTextSize(dp(12)); c.drawText("Hello,",dp(16),dp(24),text);
        text.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD)); text.setTextSize(dp(20));
        c.drawText("Let's make",dp(16),dp(49),text); c.drawText("business happen.",dp(16),dp(73),text);

        text.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL)); text.setTextSize(dp(10.5f)); text.setColor(Color.rgb(236,236,238));
        c.drawText("☀  Glasgow",dp(16),h-dp(24),text); c.drawText("12°C",dp(16),h-dp(10),text);

        String date=new SimpleDateFormat("EEE d MMM",Locale.UK).format(new Date());
        text.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD)); text.setTextSize(dp(12)); text.setColor(Color.WHITE);
        float tw=text.measureText(date); c.drawText(date,w-dp(16)-tw,h-dp(14),text);
    }

    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
}
