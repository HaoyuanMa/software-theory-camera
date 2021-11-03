package edu.bit.software_theory_camera;


import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import android.hardware.Camera;
import android.media.CameraProfile;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity {
    private Camera mCamera = null;
    private boolean capturing = false;
    private Timer timer = new Timer();
    private boolean lock = false;
    private static int count = 0;
    private TimerTask timerTask = null;

    @Override
    protected void onPause() {
        super.onPause();
        if(mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCamera != null){
            mCamera.release();
            mCamera = null;
        }
        if(timerTask != null)
        {
            timerTask.cancel();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);



        mCamera = getCameraInstance();
        if (mCamera == null){
            Toast.makeText(getApplicationContext(), "请授权应用使用相机！", Toast.LENGTH_SHORT).show();
            finish();
        }


        int[] minFps = null;

        Camera.Parameters params = mCamera.getParameters();
        List<int[]> fpsList = params.getSupportedPreviewFpsRange();
        if(fpsList != null && fpsList.size() > 0) {
            minFps = fpsList.get(0);
            for (int[] fps : fpsList) {
                if (minFps[0] * minFps[1] > fps[0] * fps[1]) {
                    minFps = fps;
                    Log.d("mhy",""+fps[0]+","+fps[1]);
                }
            }
        }

        List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
        Camera.Size psize;
        int minSizeW = pictureSizes.get(0).width;
        int minSizeH = pictureSizes.get(0).height;
        int minSize =  minSizeW * minSizeH;
        for (int i = 0; i < pictureSizes.size(); i++) {
            psize = pictureSizes.get(i);
            Log.i("pictureSize",psize.width+" x "+psize.height);
            if(psize.width * psize.height < minSize){
                minSizeW = psize.width;
                minSizeH = psize.height;
            }
        }
        Log.d("mhy","minSize: " + minSizeW + "x" + minSizeH);

        Log.d("mhy","minFpsRange: "+minFps[0]+","+minFps[1]);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        params.setPreviewFpsRange(minFps[0],minFps[1]);
        params.setJpegQuality(CameraProfile.QUALITY_LOW);
        params.setPictureSize(minSizeW,minSizeH);
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);


        Button capture = findViewById(R.id.button_capture);
        capture.setOnClickListener(v->{

            Log.d("mhy","clicked");
            if(capturing){
                capturing = false;
                Log.d("mhy","COUNT: "+ count);
                timerTask.cancel();
                timerTask = null;
            } else {
                capturing = true;
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        Log.d("mhy","timerTask.run");
                        lock = true;
                        mCamera.takePicture(null,null,mPicture);
                        while (lock){

                        }
                    }
                };
                timer.schedule(timerTask,0,1000);
            }

        });

    }


    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(1); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.d("mhy",e.getMessage());
            return c;// Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                    if (pictureFile == null){
                        Log.d("mhy", "Error creating media file, check storage permissions");
                        return;
                    }

                    Log.d("mhy","pic taken");
//                    try {
//                        FileOutputStream fos = new FileOutputStream(pictureFile);
//                        fos.write(data);
//                        fos.close();
//                        Log.d("mhy","pic saved");
//                    } catch (FileNotFoundException e) {
//                        Log.d("mhy", "File not found: " + e.getMessage());
//                    } catch (IOException e) {
//                        Log.d("mhy", "Error accessing file: " + e.getMessage());
//                    }

                    OkHttpClient httpClient = new OkHttpClient();

                    RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpg"),data);
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", "head_img", fileBody)
                            .build();

                    Request request = new Request.Builder()
                            .url("http://10.20.16.237:8080/upload")
                            .post(requestBody)
                            .build();
                    Call call = httpClient.newCall(request);

                    try {
                        //同步请求，要放到子线程执行
                        Response response = call.execute();
                        Log.i("mhy", "okHttpGet run: response:"+ response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    lock = false;
                }
            }).start();

            camera.startPreview();
        }
    };


    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + "_" +count++ + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

}