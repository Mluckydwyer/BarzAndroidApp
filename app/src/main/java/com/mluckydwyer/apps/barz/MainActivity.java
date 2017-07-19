package com.mluckydwyer.apps.barz;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Barz::MainActivity";

    static {
        System.loadLibrary("opencv_java3");
    }

    private Camera camera;
    private BackgroundProcess backgroundProcess;

    public static Camera getCameraInstance(){
        Camera c = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    c = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    e.getStackTrace();
                }
                return c;
            }
        }
        c = Camera.open();
        return c;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundProcess.ocvCameraView != null)
            backgroundProcess.ocvCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, backgroundProcess.loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            backgroundProcess.loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        backgroundProcess = new BackgroundProcess(this);
        backgroundProcess.ocvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_preview);
        backgroundProcess.ocvCameraView.setSystemUiVisibility(SurfaceView.INVISIBLE);
        backgroundProcess.ocvCameraView.enableFpsMeter();
        backgroundProcess.ocvCameraView.setVisibility(SurfaceView.VISIBLE);
        backgroundProcess.ocvCameraView.setCvCameraViewListener(backgroundProcess);
    }
}