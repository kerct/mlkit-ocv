package com.example.mlkitocv;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.mlkitocv.components.CameraImageGraphic;
import com.example.mlkitocv.components.FrameMetadata;
import com.example.mlkitocv.components.GraphicOverlay;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.util.List;

public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>>  {

    private static final String TAG = "FaceDetectionProcessor";
    private final FirebaseVisionFaceDetector detector;
    private boolean isTraining;
    private Recognise recognise;
    private List<FirebaseVisionFace> detectedFaces;
    private Bitmap originalCameraImage;
    private GraphicOverlay graphicOverlay;

    public FaceDetectionProcessor(Recognise r) {
        isTraining = false;
        recognise = r;
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder().build();
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }

    public FaceDetectionProcessor() {
        isTraining = true;
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder().build();
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionFace> faces,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {
        detectedFaces = faces;
        this.graphicOverlay = graphicOverlay;

        graphicOverlay.clear();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
            graphicOverlay.add(imageGraphic);

            Bitmap copy = Bitmap.createScaledBitmap(originalCameraImage,
                    graphicOverlay.getWidth(), graphicOverlay.getHeight(), true);
            this.originalCameraImage = copy;

            for (int i = 0; i < faces.size(); ++i) {
                FirebaseVisionFace face = faces.get(i);
                FaceGraphic faceGraphic;
                if(isTraining) {
                    faceGraphic = new FaceGraphic(graphicOverlay, face, null);
                }
                else {
                    Bitmap faceBmp = getFaceBitmap(copy, face, graphicOverlay);

                    // face alignment (2D)
//            Matrix matrix = new Matrix();
//            matrix.postRotate(face.getHeadEulerAngleZ());
//            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), true);
//            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

                    String res = recognise.recogniseFace(faceBmp);
                    faceGraphic = new FaceGraphic(graphicOverlay, face, res);
                }
                graphicOverlay.add(faceGraphic);
            }
        }

        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }

    public List<FirebaseVisionFace> getDetectedFaces() {
        return detectedFaces;
    }

    public Bitmap getOriginalCameraImage() {
        return originalCameraImage;
    }

    public Bitmap getFaceBitmap() {
        return getFaceBitmap(originalCameraImage, detectedFaces.get(0), graphicOverlay);
    }

    private Bitmap getFaceBitmap(Bitmap original, FirebaseVisionFace face, GraphicOverlay overlay) {
        Rect boundingBox = new FaceGraphic(overlay, face, null).boundingBox();
        if(rectInScreen(original, boundingBox)){
            return Bitmap.createBitmap(original, boundingBox.left, boundingBox.top,
                    boundingBox.width(), boundingBox.height());
        }
        Log.d(TAG, "face out of screen");
        return null;
    }

    private boolean rectInScreen(Bitmap bmp, Rect rect) {
        Rect screen = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        return screen.contains(rect);
    }
}
