package com.mluckydwyer.apps.barz;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

/**
 * Project: Barz
 * Created By: mluck
 * Date: 7/17/2017
 */

public class BackgroundProcess extends JavaCameraView implements SurfaceHolder.Callback, CameraBridgeViewBase.CvCameraViewListener {

    private static final String TAG = "OCVBarz::BackProcessing";
    public CameraBridgeViewBase ocvCameraView;
    private BackgroundSubtractor backgroundSubtractor;
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
        backgroundSubtractor = Video.createBackgroundSubtractorKNN(20, 400.0, false); // Num frames that affect model, shadow threshold, detect shadows

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
        int previewScaleFactor = 2;

        Core.flip(inputFrame, inputFrame, 1);

        Mat downScale = new Mat();
        Imgproc.pyrDown(inputFrame, downScale);
        for (int i = 0; i < previewScaleFactor - 1; i++)
            Imgproc.pyrDown(downScale, downScale);

        Mat fgMask = new Mat();
        backgroundSubtractor.apply(downScale, fgMask);

        Mat upScaleMask = new Mat();
        Imgproc.pyrUp(fgMask, upScaleMask);
        for (int i = 0; i < previewScaleFactor - 1; i++)
            Imgproc.pyrUp(upScaleMask, upScaleMask);

        //Mat dnMask = new Mat();
        //Photo.fastNlMeansDenoising(upScaleMask, dnMask, 10, 7, 21);

        Mat imgMasked = new Mat();
        inputFrame.copyTo(imgMasked, upScaleMask);

        Mat output = new Mat();
        output = imgMasked;

        return output;
    }

}
