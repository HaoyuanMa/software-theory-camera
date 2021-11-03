package edu.bit.software_theory_camera;


import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import android.hardware.Camera;
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
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity {
    private Camera mCamera = null;
    private boolean capturing = false;
    private Timer timer = new Timer();
    private boolean lock = false;
    private static int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("mhy","timerTask.run");
                lock = true;
                mCamera.takePicture(null,null,mPicture);
                while (lock){

                }
            }
        };

        mCamera = getCameraInstance();
        if (mCamera == null){
            Toast.makeText(getApplicationContext(), "请授权应用使用相机！", Toast.LENGTH_SHORT).show();
            finish();
        }

        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
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
            } else {
                capturing = true;
                timer.schedule(timerTask,0,100);
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
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d("mhy", "Error creating media file, check storage permissions");
                return;
            }

            Log.d("mhy","pic taken");
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d("mhy","pic saved");
            } catch (FileNotFoundException e) {
                Log.d("mhy", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("mhy", "Error accessing file: " + e.getMessage());
            }
            camera.startPreview();
            lock = false;
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