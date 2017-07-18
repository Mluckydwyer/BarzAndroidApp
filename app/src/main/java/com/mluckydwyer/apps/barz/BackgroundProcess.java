package com.mluckydwyer.apps.barz;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

/**
 * Project: Barz
 * Created By: mluck
 * Date: 7/17/2017
 */

public class BackgroundProcess implements CameraBridgeViewBase.CvCameraViewListener {

    private static final String TAG = "OCVBarz::Processing";

    private OpenCVLoader loader;
    private CameraBridgeViewBase ocvCameraView;
    private Context context;
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(context) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    Log.i(TAG, "OpenCV Loaded Successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                    ocvCameraView.enableView();
                    ocvCameraView.enableFpsMeter();
                    ocvCameraView.setCameraIndex(0);
                }
                break;
            }
        }
    };

    public BackgroundProcess(Context context) {
        this.context = context;
        loader = new OpenCVLoader();
        onCreate();
    }

    public void onCreate() {

    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        return inputFrame;
    }

}
