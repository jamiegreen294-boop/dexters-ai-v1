package uk.co.dexter.os.camera;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.camera2.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class CameraActivity extends Activity implements TextureView.SurfaceTextureListener {
    private TextureView preview;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private CaptureRequest.Builder previewRequest;
    private ImageReader reader;
    private HandlerThread thread;
    private Handler cameraHandler;
    private String cameraId;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        preview=new TextureView(this);
        preview.setSurfaceTextureListener(this);
        root.addView(preview,new FrameLayout.LayoutParams(-1,-1));

        Button shutter=new Button(this);
        shutter.setText("●");
        shutter.setTextSize(28);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(110,110,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        sp.bottomMargin=40;
        shutter.setOnClickListener(v->capture());
        root.addView(shutter,sp);

        TextView title=new TextView(this);
        title.setText("Dexter Camera");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setPadding(24,24,24,24);
        root.addView(title,new FrameLayout.LayoutParams(-1,80,Gravity.TOP));
        setContentView(root);
    }

    @Override protected void onResume(){super.onResume();startThread();if(preview.isAvailable())openCamera();}
    @Override protected void onPause(){closeCamera();stopThread();super.onPause();}

    private void startThread(){thread=new HandlerThread("DexterCamera");thread.start();cameraHandler=new Handler(thread.getLooper());}
    private void stopThread(){if(thread!=null){thread.quitSafely();try{thread.join();}catch(Exception ignored){}thread=null;cameraHandler=null;}}

    private void openCamera(){
        try{
            CameraManager cm=(CameraManager)getSystemService(CAMERA_SERVICE);
            for(String id:cm.getCameraIdList()){
                CameraCharacteristics ch=cm.getCameraCharacteristics(id);
                Integer facing=ch.get(CameraCharacteristics.LENS_FACING);
                if(facing!=null&&facing==CameraCharacteristics.LENS_FACING_BACK){cameraId=id;break;}
            }
            if(cameraId==null&&cm.getCameraIdList().length>0)cameraId=cm.getCameraIdList()[0];
            if(cameraId==null)return;
            StreamConfigurationMap map=cm.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            android.util.Size size=(map!=null&&map.getOutputSizes(ImageFormat.JPEG)!=null&&map.getOutputSizes(ImageFormat.JPEG).length>0)
                ? map.getOutputSizes(ImageFormat.JPEG)[0] : new android.util.Size(1920,1080);
            reader=ImageReader.newInstance(size.getWidth(),size.getHeight(),ImageFormat.JPEG,2);
            reader.setOnImageAvailableListener(r->saveImage(r.acquireNextImage()),cameraHandler);
            cm.openCamera(cameraId,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice c){camera=c;createPreview();}
                public void onDisconnected(CameraDevice c){c.close();camera=null;}
                public void onError(CameraDevice c,int e){c.close();camera=null;}
            },cameraHandler);
        }catch(Exception ignored){}
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
                        session.setRepeatingRequest(previewRequest.build(),null,cameraHandler);
                    }catch(Exception ignored){}
                }
                public void onConfigureFailed(CameraCaptureSession s){}
            },cameraHandler);
        }catch(Exception ignored){}
    }

    private void capture(){
        try{
            if(camera==null||session==null)return;
            CaptureRequest.Builder still=camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            still.addTarget(reader.getSurface());
            still.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            session.capture(still.build(),null,cameraHandler);
        }catch(Exception ignored){}
    }

    private void saveImage(Image image){
        if(image==null)return;
        try{
            ByteBuffer b=image.getPlanes()[0].getBuffer();
            byte[] data=new byte[b.remaining()];b.get(data);
            Uri output=getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            if(output!=null){
                try(OutputStream os=getContentResolver().openOutputStream(output)){if(os!=null)os.write(data);}
                setResult(RESULT_OK);
                runOnUiThread(this::finish);
                return;
            }
            ContentValues v=new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME,"DEXTER_"+System.currentTimeMillis()+".jpg");
            v.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
            v.put(MediaStore.Images.Media.RELATIVE_PATH,"DCIM/Dexter");
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
            if(uri!=null)try(OutputStream os=getContentResolver().openOutputStream(uri)){if(os!=null)os.write(data);}
        }catch(Exception ignored){} finally{image.close();}
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
