package com.mluckydwyer.apps.barz;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import com.bumptech.glide.gifencoder.AnimatedGifEncoder;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    private ArrayList<Mat> videoFrames;

    public BackgroundProcess(Context context) {
        super(context, 0);
        this.context = context;
        backgroundSubtractor = Video.createBackgroundSubtractorMOG2(20, 400.0, true); // Num frames that affect model, shadow threshold, detect shadows
        videoFrames = new ArrayList<Mat>();

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
        Core.flip(inputFrame, inputFrame, 1);
        System.gc();
        if (MainActivity.isRecording) {
            videoFrames.add(inputFrame);
            return inputFrame;
        }
        return openCVPreview(inputFrame);
    }

    public Mat openCVPreview(Mat inputFrame) {
        //store width and heights for scaling/optimization purposes
        int w = inputFrame.width();
        int h = inputFrame.height();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<MatOfPoint> barPoints = new ArrayList<MatOfPoint>();
        List<MatOfInt> hulls = new ArrayList<MatOfInt>();
        Mat erosion = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(5, 5));
        Mat dilation = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(8, 8));
        Rect roi = new Rect(w / 3, 0, 20, h);
        Mat bar = new Mat(inputFrame, roi);
        Mat maskedBar = new Mat();
        Mat barArea = bar.clone();

        Imgproc.cvtColor(bar, bar, Imgproc.COLOR_RGB2HSV, 3);
        Imgproc.cvtColor(bar, bar, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(bar, bar, new Size(5, 5), 0);
        Imgproc.threshold(bar, bar, 0, 255, Imgproc.THRESH_OTSU);

        Imgproc.erode(bar, bar, erosion);
        Imgproc.dilate(bar, bar, dilation);
        Imgproc.dilate(bar, bar, dilation);

        Mat backsub = new Mat();
        backgroundSubtractor.apply(barArea, backsub);

        Mat m = new Mat();
        Core.subtract(bar, backsub, bar);
        Core.subtract(barArea, Core.mean(inputFrame), m);
        Imgproc.cvtColor(m, m, Imgproc.COLOR_RGB2HSV, 3);
        Imgproc.cvtColor(m, m, Imgproc.COLOR_RGB2GRAY);

        Mat submat = inputFrame.submat(roi);
        barArea.copyTo(maskedBar, bar);

        double buff[] = new double[maskedBar.channels()];

        maskedBar.copyTo(submat);

        return inputFrame;
    }

    public Mat openCVFinal(Mat inputFrame) {
        /*//Imgproc.pyrDown(inputFrame, inputFrame);
        Imgproc.resize(inputFrame, inputFrame, new Size(960, 540));


        Mat fgMask = new Mat();
        //backgroundSubtractorFinal.apply(inputFrame, fgMask);

        //Mat dnMask = new Mat();
        //Photo.fastNlMeansDenoising(fgMask, dnMask, 5, 7, 21);

        Mat imgMasked = new Mat();
        inputFrame.copyTo(imgMasked, fgMask);

        //Imgproc.pyrUp(imgMasked, imgMasked);

        return imgMasked;*/

        Mat m = openCVPreview(inputFrame);

        return m;
    }

    public void compileVideo() {
        Mat tmp = new Mat(ocvCameraView.getHeight(), ocvCameraView.getWidth(), CvType.CV_8U, new Scalar(4));
        ArrayList<Bitmap> videoBitmaps = new ArrayList<Bitmap>();
        Bitmap bmp = null;

        Log.e(TAG, "VFs: " + videoFrames.size());

        int i = 1;
        for (Mat frame : videoFrames) {
            Imgproc.resize(frame, frame, new Size(960, 540));
            tmp = openCVFinal(frame); //TODO
            //tmp = openCVPreview(frame);
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(tmp, bmp);
            videoBitmaps.add(bmp);
            Log.e(TAG, "Frame Rendered" + bmp.getWidth() + "=" + tmp.width() + "\t" + bmp.getHeight() + "=" + tmp.height() + "\t" + i);
            i++;
        }
        videoFrames.clear();

        Log.e(TAG, "Frames: " + videoBitmaps.size());

        makeGIF(videoBitmaps);
    }

    public void makeGIF(ArrayList<Bitmap> videoBitmaps) {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/myimages/popout.gif");
            file.delete();
        } catch (Exception e) {
        }

        AnimatedGifEncoder e = new AnimatedGifEncoder();
        e.start(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/popout.gif");
        //e.setDelay(250);

        for (Bitmap b : videoBitmaps)
            e.addFrame(b);

        e.setQuality(1);
        e.finish();
        Log.e(TAG, "Gif Made");
    }

}