package com.mluckydwyer.apps.barz;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private static final String TAG = "Barz::MainActivity";

    static {
        System.loadLibrary("opencv_java3");
    }

    private Camera camera;
    private BackgroundProcess backgroundProcess;
    private VideoCapture videoCapture;
    private MediaRecorder mediaRecorder;

    private boolean isRecording = false;
    private int cameraNum = 0;

    private ImageView captureButton;
    private ImageView recordButton;



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

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
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

        releaseCamera();
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

        videoCapture = new VideoCapture();
        //videoCapture.open(-1);

        // Record code
        camera = getCameraInstance();
        captureButton = (ImageView) findViewById(R.id.outline_circle);
        recordButton = (ImageView) findViewById(R.id.recording_circle);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isRecording) {
                            if (prepareVideoRecorder()) {
                                mediaRecorder.start();
                                playAnimation();
                            } else {
                                releaseCamera();
                            }
                        }
                    }
                }
        );
    }

    private void releaseCamera(){
        if (camera != null) {
            camera.release();
            camera = null;
        }

        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }

    private boolean prepareVideoRecorder() {
        camera = getCameraInstance();
        mediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mediaRecorder.setProfile(CamcorderProfile.get(cameraNum, CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            //Log.d(TAG, "Exception preparing MediaRecorder: " + e.getMessage());
            releaseCamera();
            return false;
        }
        return true;
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
                // stop recording and release camera
                mediaRecorder.stop();  // stop the recording
                releaseCamera(); // release the MediaRecorder object
                isRecording = false;
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