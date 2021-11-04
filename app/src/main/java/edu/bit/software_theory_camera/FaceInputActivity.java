package edu.bit.software_theory_camera;

import static edu.bit.software_theory_camera.CameraHelper.freeCamera;
import static edu.bit.software_theory_camera.CameraHelper.getCameraInstance;
import static edu.bit.software_theory_camera.CameraHelper.setUpCamera;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

public class FaceInputActivity extends AppCompatActivity {

    private Camera mCamera = null;

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
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.input_preview);
        preview.addView(mPreview);



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
}