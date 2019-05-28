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
import android.widget.Toast;

import com.example.mlkitocv.components.CameraSource;
import com.example.mlkitocv.components.CameraSourcePreview;
import com.example.mlkitocv.components.GraphicOverlay;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Training extends AppCompatActivity {
    private static final String TAG = "Training";
    private static final int PERMISSION_REQUESTS = 1;

    private FaceDetectionProcessor faceDetectionProcessor;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private boolean facingBack = true;

    PersonRecogniser personRecogniser;
    String path;
    Labels nameLabels;
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        name = getIntent().getStringExtra("name");
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

        path = Environment.getExternalStorageDirectory()+"/facerecogMLKit/";
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

        final FloatingActionButton capture = findViewById(R.id.capture);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capture();
            }
        });
    }

    private void capture() {
        List<FirebaseVisionFace> faces = faceDetectionProcessor.getDetectedFaces();
        Bitmap original = faceDetectionProcessor.getOriginalCameraImage();
        int numFaces = faces.size();
        if(numFaces != 1) {
            Toast.makeText(Training.this, numFaces + " faces detected!", Toast.LENGTH_LONG).show();
        }
        else {
            Bitmap faceBmp = faceDetectionProcessor.getFaceBitmap();
            if(faceBmp != null){
                personRecogniser.savePic(faceBmp, name);
                Toast.makeText(Training.this, "Captured!", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(Training.this, "Please ensure that the whole face is in the frame", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void createCameraSource() {
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {
            faceDetectionProcessor = new FaceDetectionProcessor();
            cameraSource.setMachineLearningFrameProcessor(faceDetectionProcessor);
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
