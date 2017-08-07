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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.R.attr.max;

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
        backgroundSubtractorPreview = Video.createBackgroundSubtractorKNN(15, 400.0, false); // Num frames that affect model, shadow threshold, detect shadows
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
        if (MainActivity.isRecording) videoFrames.add(inputFrame);
        return openCVPreview(inputFrame);
    }

    public Mat openCVPreview(Mat inputFrame) {
        Mat toGray = new Mat();
        Imgproc.cvtColor(inputFrame, toGray, Imgproc.COLOR_RGB2GRAY);

        int ksize = 21;
        int amount = 11;
        int size = 3;

        Mat atMask = new Mat();
        Imgproc.adaptiveThreshold(toGray, atMask, max, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, ksize, amount);


        Mat eroded = new Mat();
        //Imgproc.erode(atMask, eroded, Imgproc.getGaussianKernel(100, 50, Imgproc.MORPH_ERODE));
        Imgproc.erode(atMask, eroded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(size * 2 + 1, size * 2 + 1), new Point(size, size)));


        //remove point noise
        //Mat denoised = new Mat();
        //Photo.fastNlMeansDenoising(eroded, denoised);

        /*int previewScaleFactor = 3;

        Core.flip(inputFrame, inputFrame, 1);

        Mat downScale = new Mat();
        Imgproc.pyrDown(inputFrame, downScale);
        for (int i = 0; i < previewScaleFactor - 1; i++)
            Imgproc.pyrDown(downScale, downScale);

        Mat fgMask = new Mat();
        //backgroundSubtractorPreview.apply(downScale, fgMask);
        Imgproc.adaptiveThreshold(downScale, fgMask, 10, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_MASK, 5, 2);

        Mat upScaleMask = new Mat();
        Imgproc.pyrUp(fgMask, upScaleMask);
        for (int i = 0; i < previewScaleFactor - 1; i++)
            Imgproc.pyrUp(upScaleMask, upScaleMask);

        //Mat dnMask = new Mat();
        //Photo.fastNlMeansDenoising(upScaleMask, dnMask, 10, 7, 21);*/

        Mat imgMasked = new Mat();
        inputFrame.copyTo(imgMasked, eroded);

        return imgMasked;
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
