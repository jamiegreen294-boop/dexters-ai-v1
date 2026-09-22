package uk.co.dextersspot.dexterai;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.camera2.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class DexterCameraActivity extends Activity implements TextureView.SurfaceTextureListener {
    private TextureView preview;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private CaptureRequest.Builder previewRequest;
    private ImageReader reader;
    private HandlerThread thread;
    private Handler handler;
    private String cameraId;
    private TextView status;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        preview=new TextureView(this);
        preview.setSurfaceTextureListener(this);
        root.addView(preview,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(16,12,16,12);
        top.setBackgroundColor(0xAA000000);

        Button back=new Button(this);
        back.setText("‹ Back to Dexter");
        back.setTextSize(15);
        back.setOnClickListener(v->finishToDexter());
        top.addView(back,new LinearLayout.LayoutParams(-2,60));

        TextView title=new TextView(this);
        title.setText("Dexter Camera");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding(18,0,0,0);
        top.addView(title,new LinearLayout.LayoutParams(0,60,1));
        root.addView(top,new FrameLayout.LayoutParams(-1,84,Gravity.TOP));

        status=new TextView(this);
        status.setText("Opening camera…");
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setBackgroundColor(0x88000000);
        FrameLayout.LayoutParams st=new FrameLayout.LayoutParams(-1,64,Gravity.BOTTOM);
        st.bottomMargin=140;
        root.addView(status,st);

        Button shutter=new Button(this);
        shutter.setText("●");
        shutter.setTextSize(30);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(110,110,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        sp.bottomMargin=24;
        shutter.setOnClickListener(v->capture());
        root.addView(shutter,sp);

        setContentView(root);
    }

    @Override protected void onResume(){super.onResume();startThread();if(preview.isAvailable())openCamera();}
    @Override protected void onPause(){closeCamera();stopThread();super.onPause();}
    @Override public void onBackPressed(){finishToDexter();}

    private void finishToDexter(){
        try{
            Intent i=new Intent(this,DexterHomeActivity.class);
            i.putExtra("page","home");
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }catch(Exception ignored){}
        finish();
    }

    private void startThread(){if(thread!=null)return;thread=new HandlerThread("DexterCamera");thread.start();handler=new Handler(thread.getLooper());}
    private void stopThread(){if(thread!=null){thread.quitSafely();try{thread.join();}catch(Exception ignored){}thread=null;handler=null;}}

    private void openCamera(){
        try{
            if(checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                status.setText("Camera permission unavailable.");
                return;
            }
            CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);
            for(String id:cm.getCameraIdList()){
                CameraCharacteristics ch=cm.getCameraCharacteristics(id);
                Integer facing=ch.get(CameraCharacteristics.LENS_FACING);
                if(facing!=null&&facing==CameraCharacteristics.LENS_FACING_BACK){cameraId=id;break;}
            }
            if(cameraId==null&&cm.getCameraIdList().length>0)cameraId=cm.getCameraIdList()[0];
            if(cameraId==null){status.setText("No camera found.");return;}
            android.hardware.camera2.params.StreamConfigurationMap map=cm.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            android.util.Size[] sizes=map==null?null:map.getOutputSizes(ImageFormat.JPEG);
            android.util.Size size=(sizes!=null&&sizes.length>0)?sizes[Math.min(sizes.length-1,2)]:new android.util.Size(1920,1080);
            reader=ImageReader.newInstance(size.getWidth(),size.getHeight(),ImageFormat.JPEG,2);
            reader.setOnImageAvailableListener(r->saveImage(r.acquireNextImage()),handler);
            cm.openCamera(cameraId,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice c){camera=c;runOnUiThread(()->status.setText("Ready"));createPreview();}
                public void onDisconnected(CameraDevice c){c.close();camera=null;runOnUiThread(()->status.setText("Camera disconnected."));}
                public void onError(CameraDevice c,int e){c.close();camera=null;runOnUiThread(()->status.setText("Camera error "+e));}
            },handler);
        }catch(Exception e){status.setText("Unable to open camera.");}
    }

    private void createPreview(){
        try{
            SurfaceTexture texture=preview.getSurfaceTexture();
            if(texture==null||camera==null)return;
            texture.setDefaultBufferSize(1280,720);
            Surface surface=new Surface(texture);
            previewRequest=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequest.addTarget(surface);
            camera.createCaptureSession(Arrays.asList(surface,reader.getSurface()),new CameraCaptureSession.StateCallback(){
                public void onConfigured(CameraCaptureSession s){
                    session=s;
                    try{
                        previewRequest.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        session.setRepeatingRequest(previewRequest.build(),null,handler);
                    }catch(Exception ignored){}
                }
                public void onConfigureFailed(CameraCaptureSession s){runOnUiThread(()->status.setText("Preview unavailable."));}
            },handler);
        }catch(Exception e){runOnUiThread(()->status.setText("Preview unavailable."));}
    }

    private void capture(){
        try{
            if(camera==null||session==null){status.setText("Camera not ready.");return;}
            status.setText("Saving…");
            CaptureRequest.Builder still=camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            still.addTarget(reader.getSurface());
            still.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            session.capture(still.build(),new CameraCaptureSession.CaptureCallback(){
                @Override public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest r,TotalCaptureResult result){
                    runOnUiThread(()->status.setText("Saved to Photos"));
                }
            },handler);
        }catch(Exception e){status.setText("Could not take photo.");}
    }

    private void saveImage(Image image){
        if(image==null)return;
        try{
            ByteBuffer b=image.getPlanes()[0].getBuffer();
            byte[] data=new byte[b.remaining()];b.get(data);
            ContentValues v=new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME,"DEXTER_"+System.currentTimeMillis()+".jpg");
            v.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
            v.put(MediaStore.Images.Media.RELATIVE_PATH,"DCIM/Dexter");
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
            if(uri!=null)try(OutputStream os=getContentResolver().openOutputStream(uri)){if(os!=null)os.write(data);}
        }catch(Exception ignored){}finally{image.close();}
    }

    private void closeCamera(){
        try{if(session!=null)session.close();}catch(Exception ignored){}
        try{if(camera!=null)camera.close();}catch(Exception ignored){}
        try{if(reader!=null)reader.close();}catch(Exception ignored){}
        session=null;camera=null;reader=null;
    }

    public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){openCamera();}
    public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}
    public boolean onSurfaceTextureDestroyed(SurfaceTexture s){return true;}
    public void onSurfaceTextureUpdated(SurfaceTexture s){}
}
