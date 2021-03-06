package edu.bit.software_theory_camera;


import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import static edu.bit.software_theory_camera.CameraHelper.freeCamera;
import static edu.bit.software_theory_camera.CameraHelper.getCameraInstance;
import static edu.bit.software_theory_camera.CameraHelper.getCount;
import static edu.bit.software_theory_camera.CameraHelper.getOutputMediaFile;
import static edu.bit.software_theory_camera.CameraHelper.setUpCamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import java.nio.ByteBuffer;
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
    private TimerTask timerTask = null;

    @Override
    protected void onPause() {
        super.onPause();
        freeCamera(mCamera);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        freeCamera(mCamera);
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
            Toast.makeText(getApplicationContext(), "??????????????????????????????", Toast.LENGTH_SHORT).show();
            finish();
        }

        setUpCamera(mCamera);

        // Create our Preview view and set it as the content of our activity.
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        Button capture = findViewById(R.id.button_capture);
        capture.setOnClickListener(v->{
            Log.d("mhy","clicked");
            if(capturing){
                capturing = false;
                Log.d("mhy","COUNT: "+ getCount());
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
                timer.schedule(timerTask,0,500);
            }
        });
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    Log.d("mhy","pic taken");

                    OkHttpClient httpClient = new OkHttpClient();
                    RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpg"),data);
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", "head_img", fileBody)
                            .build();
                    String url = "http://" + Config.HOST + ":" + Config.PORT + Config.CAMERA_UPLOAD_URL;
                    Log.d("mhy",url);
                    Request request = new Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build();
                    Call call = httpClient.newCall(request);

                    try {
                        //???????????????????????????????????????
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
}