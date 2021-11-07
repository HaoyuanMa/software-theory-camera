package edu.bit.software_theory_camera;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import android.hardware.Camera;
import android.media.CameraProfile;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraHelper {


    private static int count = 0;

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

    public static File getOutputMediaFile(int type){
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

    public static void setUpCamera(Camera mCamera){
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
        params.setRotation(270);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
    }

    public static void freeCamera(Camera camera){
        if(camera != null){
            camera.release();
            camera = null;
        }
    }

    public static int getCount(){
        return count;
    }
}
