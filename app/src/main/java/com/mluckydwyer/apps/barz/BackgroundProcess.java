package com.mluckydwyer.apps.barz;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;

import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Project: Barz
 * Created By: mluck
 * Date: 7/17/2017
 */

public class BackgroundProcess extends JavaCameraView implements SurfaceHolder.Callback, CameraBridgeViewBase.CvCameraViewListener {

    private static final String TAG = "OCVBarz::BackProcessing";
    private final int FPS = 15;
    private final int VIDEO_LENGTH_SEC = 5;
    public CameraBridgeViewBase ocvCameraView;
    private BackgroundSubtractor backgroundSubtractorPreview;
    private BackgroundSubtractor backgroundSubtractorFinal;
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
        backgroundSubtractorPreview = Video.createBackgroundSubtractorMOG2(15, 400.0, true); // Num frames that affect model, shadow threshold, detect shadows
        backgroundSubtractorFinal = Video.createBackgroundSubtractorKNN(25, 400.0, true); // Num frames that affect model, shadow threshold, detect shadows
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
        if (MainActivity.isRecording) videoFrames.add(inputFrame);
        return openCVPreview(inputFrame);
    }

    public Mat openCVPreview(Mat inputFrame) {
        //store width and heights for scaling/optimization purposes
        int w = inputFrame.width();
        int h = inputFrame.height();

        //resize image for fps purposes lol
        //Imgproc.resize(inputFrame, inputFrame, new Size(256, 144));

        //convert to HSV color space for swag purposes (test with this)
        //Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGB2HSV, 3);

        //makes image go to black and white
        //Mat toGray = new Mat();
        //Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGB2GRAY);

        //do binary thres first
        //Imgproc.threshold(inputFrame, inputFrame, 127, 255, Imgproc.THRESH_BINARY);

        //gaussian blur
        //Imgproc.GaussianBlur(inputFrame, inputFrame, new Size(5,5), 0);

        //now threshold da image
        //Imgproc.threshold(inputFrame, inputFrame, 138, 255, Imgproc.THRESH_BINARY);
        //Imgproc.threshold(inputFrame, inputFrame, 0, 255, Imgproc.THRESH_OTSU);

        //create an erosion and dilation matrixes
        //Mat erosion = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(3,3));
        //Mat dilation = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(8,8));

        //now erode and dilate the toGray matrix
        //Imgproc.erode(inputFrame, inputFrame, erosion);
        //Imgproc.erode(toGray, toGray, erosion);
        //Imgproc.dilate(inputFrame, inputFrame, dilation);
        //Imgproc.dilate(toGray, toGray, dilation);

        //Imgproc.resize(toGray, toGray, new Size(w, h));

        //create region of interest and the bar from that for swag
        Rect roi = new Rect(w/3, 0, 20, h);
        Mat bar = new Mat(inputFrame, roi);
        Imgproc.cvtColor(bar, bar, Imgproc.COLOR_RGB2HSV, 3);
        Imgproc.cvtColor(bar, bar, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(bar, bar, new Size(5,5), 0);
        Imgproc.threshold(bar, bar, 0, 255, Imgproc.THRESH_OTSU);

        Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGB2GRAY);

        Mat submat = inputFrame.submat(roi);
        bar.copyTo(submat);

        return inputFrame;
    }

    public Mat openCVFinal(Mat inputFrame) {
        //Imgproc.pyrDown(inputFrame, inputFrame);
        Imgproc.resize(inputFrame, inputFrame, new Size(960, 540));


        Mat fgMask = new Mat();
        backgroundSubtractorFinal.apply(inputFrame, fgMask);

        //Mat dnMask = new Mat();
        //Photo.fastNlMeansDenoising(fgMask, dnMask, 5, 7, 21);

        Mat imgMasked = new Mat();
        inputFrame.copyTo(imgMasked, fgMask);

        //Imgproc.pyrUp(imgMasked, imgMasked);

        return imgMasked;
    }

    public String compileVideo() {
        Mat tmp = new Mat(ocvCameraView.getHeight(), ocvCameraView.getWidth(), CvType.CV_8U, new Scalar(4));
        org.jcodec.api.awt.SequenceEncoder encoder = null;
        File videoFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        String path = videoFile.getPath() + File.separator +
                "VID_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";

        if (!videoFile.exists()) {
            if (!videoFile.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return "Failed to create Directory";
            }
        }

        try {
            encoder = new org.jcodec.api.awt.SequenceEncoder(new File(path));
            ArrayList<Bitmap> videoBitmaps = new ArrayList<Bitmap>();
            Bitmap bmp = null;

            Log.e(TAG, "VFs: " + videoFrames.size());

            for (Mat frame : videoFrames) {
                tmp = openCVFinal(frame);
                bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(tmp, bmp);
                videoBitmaps.add(bmp);
                Log.e(TAG, "Frame Rendered" + bmp.getWidth() + "=" + tmp.width() + "\t" + bmp.getHeight() + "=" + tmp.height());
            }
            videoFrames.clear();

            Log.e(TAG, "Frames: " + videoBitmaps.size());

            for (Bitmap bmpFrame : videoBitmaps) {
                /*int[][] pixelData = new int[3][bmpFrame.getWidth() * bmpFrame.getHeight()];

                Log.e(TAG, pixelData.length + " " + pixelData[0].length + "\t" + bmpFrame.getWidth() + " " + bmpFrame.getHeight());

                for (int x = 0; x < bmpFrame.getWidth(); x++) {
                    for (int y = 0; y < bmpFrame.getHeight(); y++) {
                        pixelData[0][bmpFrame.getWidth() * y + x] = Color.red(bmpFrame.getPixel(x, y));
                        pixelData[1][bmpFrame.getWidth() * y + x] = Color.green(bmpFrame.getPixel(x, y));
                        pixelData[2][bmpFrame.getWidth() * y + x] = Color.blue(bmpFrame.getPixel(x, y));
                    }
                }*/

                Picture dst = Picture.create(bmpFrame.getWidth(), bmpFrame.getHeight(), ColorSpace.RGB);
                int[] dstData = dst.getPlaneData(0);
                int off = 0;

                for (int i = 0; i < bmpFrame.getHeight(); ++i) {
                    for (int j = 0; j < bmpFrame.getWidth(); ++j) {
                        int rgb1 = bmpFrame.getPixel(j, i);
                        dstData[off++] = rgb1 >> 16 & 255;
                        dstData[off++] = rgb1 >> 8 & 255;
                        dstData[off++] = rgb1 & 255;
                    }
                }


                //Picture pic = new Picture(bmpFrame.getWidth(), bmpFrame.getHeight(), pixelData, ColorSpace.RGB);

                Log.e(TAG, "Frame encoded: " + dst.getWidth() + "=" + bmpFrame.getWidth() + "\t" + dst.getHeight() + "=" + bmpFrame.getHeight());
                encoder.encodeNativeFrame(dst);
            }


            encoder.finish();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return videoFile.getPath();
    }

}