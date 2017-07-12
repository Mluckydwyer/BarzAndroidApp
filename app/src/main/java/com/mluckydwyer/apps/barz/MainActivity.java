package com.mluckydwyer.apps.barz;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private CameraPreview cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Create an instance of Camera
        camera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);
    }

    private boolean checkCameraHardware(Context context){
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            return true;
        return false;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try{
            c = Camera.open();
        }catch(Exception e){

        }
        return c;
    }
}