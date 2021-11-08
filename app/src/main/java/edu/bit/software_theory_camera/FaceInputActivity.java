package edu.bit.software_theory_camera;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static edu.bit.software_theory_camera.CameraHelper.freeCamera;
import static edu.bit.software_theory_camera.CameraHelper.getCameraInstance;
import static edu.bit.software_theory_camera.CameraHelper.getOutputMediaFile;
import static edu.bit.software_theory_camera.CameraHelper.setUpCamera;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FaceInputActivity extends AppCompatActivity {

    private Camera mCamera = null;
    private String res = "";
    private boolean lock = false;
    CameraPreview mPreview = null;

    private int id = 0;
    private final int FINISH = 1;

    private Handler resultHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if(msg.what == FINISH){
                    findViewById(R.id.button_submit).setActivated(true);
                    ResultDialog resultDialog = new ResultDialog(mPreview.getContext());
                    resultDialog.show();
                }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_input);
        mCamera = getCameraInstance();
        if (mCamera == null){
            Toast.makeText(getApplicationContext(), "请授权应用使用相机！", Toast.LENGTH_SHORT).show();
            finish();
        }
        setUpCamera(mCamera);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.input_preview);
        preview.addView(mPreview);

        EditText editText = findViewById(R.id.input_id);
        Button submit = findViewById(R.id.button_submit);

        submit.setOnClickListener(v->{

            v.setActivated(false);

            InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPreview.getWindowToken(),0);

            String idText = editText.getText().toString();
            if(!idText.isEmpty()){
                id = Integer.parseInt(idText);
            }

            Log.d("mhy","clicked,id= " + id);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!res.equals("11")){
                        lock = true;
                        mCamera.takePicture(null,null,mPicture);
                        while (lock){

                        }
                    }

                    OkHttpClient httpClient = new OkHttpClient();
                    String url = "http://" + Config.HOST + ":" + Config.PORT + Config.FACE_INPUT_RECOGNISE_URL + "?id=" + id;
                    Request request = new Request.Builder()
                            .get()
                            .url(url)
                            .build();
                    Call call = httpClient.newCall(request);

                    try {
                        //同步请求，要放到子线程执行
                        Response response = call.execute();
                        Log.i("mhy", "okHttpGet run: response:"+ response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Message message = new Message();
                    message.what = FINISH;
                    resultHandler.sendMessage(message);

                }
            }).start();

        });

    }
    @Override
    protected void onPause() {
        super.onPause();
        freeCamera(mCamera);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        freeCamera(mCamera);
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

                    OkHttpClient httpClient = new OkHttpClient();
                    RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpg"),data);
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", id + "", fileBody)
                            .build();

                    String url = "http://" + Config.HOST + ":" + Config.PORT + Config.FACE_INPUT_UPLOAD_URL;
                    Request request = new Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build();
                    Call call = httpClient.newCall(request);

                    try {
                        //同步请求，要放到子线程执行
                        Response response = call.execute();
                        res = response.body().string();
                        Log.i("mhy", "okHttpGet run: response:"+ res);
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