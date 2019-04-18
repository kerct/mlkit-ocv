package com.example.mlkitocv;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.mlkitocv.components.CameraSource;
import com.example.mlkitocv.components.CameraSourcePreview;
import com.example.mlkitocv.components.GraphicOverlay;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Recognise extends AppCompatActivity {
    private static final String TAG = "Recognise";
    private static final int PERMISSION_REQUESTS = 1;

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private boolean facingBack = true;

    private PersonRecogniser personRecogniser;
    private String path;

    Labels nameLabels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognise);

        preview = findViewById(R.id.preview);
        graphicOverlay = findViewById(R.id.overlay);

        FloatingActionButton fab = findViewById(R.id.fab);
        if (Camera.getNumberOfCameras() == 1) {
            fab.hide();
        }
        else{
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (cameraSource != null) {
                        if (facingBack) {
                            cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
                            facingBack = false;
                        } else {
                            cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
                            facingBack = true;
                        }
                    }
                    preview.stop();
                    startCameraSource();
                }
            });
        }

        if (allPermissionsGranted()) {
            createCameraSource();
            startCameraSource();
        } else {
            getRuntimePermissions();
        }

        path = Environment.getExternalStorageDirectory()+"/facerecogOCV/";
        nameLabels = new Labels(path);
        boolean success=(new File(path)).mkdirs();
        if (!success)
        {
            Log.e("Error","Error creating directory");
        }

        if(OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCV loaded");
        } else{
            Log.e(TAG, "OpenCV not loaded");
        }

        personRecogniser = new PersonRecogniser(path);
        personRecogniser.train();
        Log.d(TAG, "personRecogniser trained");
    }

    public String recogniseFace(Bitmap original, FirebaseVisionFace face) {
        Rect boundingBox = face.getBoundingBox();
        if(rectInScreen(original, boundingBox)){
            Bitmap bmp = Bitmap.createBitmap(original, boundingBox.left, boundingBox.top,
                    boundingBox.width(), boundingBox.height());
            return personRecogniser.predict(bmp);
        }
        return "Unknown";
    }

    private boolean rectInScreen(Bitmap bmp, Rect rect) {
        Rect screen = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        return screen.contains(rect);
    }

    private void createCameraSource() {
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {
            cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "graphicOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
        /*
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (allPermissionsGranted()) {
            createCameraSource();
            startCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }
}
