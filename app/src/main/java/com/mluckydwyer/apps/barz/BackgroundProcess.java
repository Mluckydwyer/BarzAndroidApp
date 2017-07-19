package com.mluckydwyer.apps.barz;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;

/**
 * Project: Barz
 * Created By: mluck
 * Date: 7/17/2017
 */

public class BackgroundProcess extends JavaCameraView implements SurfaceHolder.Callback, CameraBridgeViewBase.CvCameraViewListener {

    private static final String TAG = "OCVBarz::BackProcessing";
    public CameraBridgeViewBase ocvCameraView;
    private Context context;
    public BaseLoaderCallback loaderCallback = new BaseLoaderCallback(context) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    Log.i(TAG, "OpenCV Loaded Successfully");
                    ocvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public BackgroundProcess(Context context) {
        super(context, 0);
        this.context = context;
        Log.i(TAG, "Background Process Constructor Called");
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "Camera View Started");
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        return inputFrame;
    }

}
