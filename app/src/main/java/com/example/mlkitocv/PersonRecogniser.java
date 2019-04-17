package com.example.mlkitocv;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.nio.IntBuffer;

import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.DoublePointer;
import org.opencv.android.Utils;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_face.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvCvtColor;

public class PersonRecogniser {
    private static final String TAG = "PersonRecogniser";
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;

    private FaceRecognizer fr;
    private String path;
    private int count = 0;
    private Labels nameLabels;

    PersonRecogniser(String path) {
        Log.d(TAG, "creating person recogniser");
        // using default values
        fr = LBPHFaceRecognizer.create();
        Log.d(TAG, "created face recogniser");
        this.path = path;
        nameLabels = new Labels(path);
    }

    private void savePic(Mat m, String name) {
        /*
        Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bmp);
        bmp = Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);

        FileOutputStream f;
        try {
            f = new FileOutputStream(path + name + "-" + count + ".jpg",true);
            count++;
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, f);
            f.close();
        } catch (Exception e) {
            Log.e("error",e.getCause() + " " + e.getMessage());
            e.printStackTrace();
        }*/
    }

    public void train() {
        File root = new File(path);
        FilenameFilter pngFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jpg");
            }
        };

        File[] imageFiles = root.listFiles(pngFilter);
        MatVector images = new MatVector(imageFiles.length);

        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();

        int counter = 0;
        int pathLength = path.length();

        for (File image: imageFiles) {
            String p = image.getAbsolutePath();

            int nameLastIndex = p.lastIndexOf("-");
            int numLastIndex = p.lastIndexOf(".");
            int currCount = Integer.parseInt(p.substring(nameLastIndex + 1, numLastIndex));

            if (count < currCount)
                count++;

            String description = p.substring(pathLength, nameLastIndex);

            if (nameLabels.get(description) < 0)
                nameLabels.add(description, nameLabels.max() + 1);

            int label = nameLabels.get(description);

            Mat img = imread(image.getAbsolutePath(), IMREAD_GRAYSCALE);
            images.put(counter, img);
            labelsBuf.put(counter, label);
            counter++;
        }

        if (counter > 0)
            if (nameLabels.max() > 1)
                fr.train(images, labels);

        nameLabels.save();
    }

    private boolean canPredict()
    {
        return (nameLabels.max() > 1);
    }

    public String predict(Bitmap bmp) {
        final int CONFIDENCE = 75;

        if (!canPredict()){
            Log.d(TAG, "can't predict");
            return "";
        }
        Log.d(TAG, "can predict");

        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);
        Mat mat = bitmapToMat(bmp);

        Log.d(TAG, "predicting");
        fr.predict(mat, label, confidence);
        Log.d(TAG, "predicted");

        int predictedLabel = label.get(0);
        double predictedConfidence = confidence.get(0);

        // set the associated confidence (distance)
        if ((predictedLabel != -1) && (predictedConfidence < CONFIDENCE)) {
            return nameLabels.get(predictedLabel) + " " + predictedConfidence;
        }
        else
            return "Unknown";
    }

    private Mat bitmapToMat(Bitmap bmp) {
        OpenCVFrameConverter.ToMat convertToMat = new OpenCVFrameConverter.ToMat();
        OpenCVFrameConverter.ToOrgOpenCvCoreMat convertToOCVMat = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();

        Mat mat = new Mat(bmp.getWidth(), bmp.getHeight());
        org.opencv.core.Mat cvmat = convertToOCVMat.convert(convertToMat.convert(mat));
        Bitmap bmp32 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, cvmat);
        mat = convertToMat.convert(convertToMat.convert(cvmat));

        return mat;
    }

}
