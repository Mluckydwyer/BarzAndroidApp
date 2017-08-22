package com.mluckydwyer.apps.barz;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.videoio.VideoCapture;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Barz::MainActivity";
    public static boolean isRecording = false;

    static {
        System.loadLibrary("opencv_java3");
    }

    private BackgroundProcess backgroundProcess;
    private VideoCapture videoCapture;
    private ImageView captureButton;
    private ImageView recordButton;

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

        captureButton = (ImageView) findViewById(R.id.outline_circle);
        recordButton = (ImageView) findViewById(R.id.recording_circle);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isRecording) playAnimation();
                    }
                }
        );
    }

    private void playAnimation(){
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.scale_animation);
        recordButton.setAnimation(animation);
        recordButton.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isRecording = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isRecording = false;

                backgroundProcess.compileVideo();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    //unused animation code
    private void playClickAnimation(){
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.click_animation);
        recordButton.setAnimation(animation);
        recordButton.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}