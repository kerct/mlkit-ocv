package com.example.mlkitocv;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizer;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.MatVector;

public class PersonRecogniser {
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;

    private FaceRecognizer fr;
    private String path;
    private int count = 0;
    private Labels nameLabels;

    PersonRecogniser(String path) {
        // using default values
        fr = com.googlecode.javacv.cpp.opencv_contrib.createLBPHFaceRecognizer();
        this.path = path;
        nameLabels = new Labels(path);
    }

    private void savePic(Mat m, String name) {
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
        }
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
        int[] labels = new int[imageFiles.length];

        int counter = 0;
        int label;

        IplImage img;
        IplImage grayImg;

        int pathLength = path.length();

        for (File image: imageFiles) {
            String p = image.getAbsolutePath();
            img = cvLoadImage(p);

            int nameLastIndex = p.lastIndexOf("-");
            int numLastIndex = p.lastIndexOf(".");
            int currCount = Integer.parseInt(p.substring(nameLastIndex + 1, numLastIndex));

            if (count < currCount)
                count++;

            String description = p.substring(pathLength, nameLastIndex);

            if (nameLabels.get(description) < 0)
                nameLabels.add(description, nameLabels.max() + 1);

            label = nameLabels.get(description);

            grayImg = IplImage.create(img.width(), img.height(), IPL_DEPTH_8U, 1);

            cvCvtColor(img, grayImg, CV_BGR2GRAY);

            images.put(counter, grayImg);

            labels[counter] = label;

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

    public String predict(Mat m) {
        final int CONFIDENCE = 75;

        if (!canPredict())
            return "";

        int n[] = new int[1]; // [0]
        double p[] = new double[1]; // [0.0]
        IplImage ipl = MatToIplImage(m);

        fr.predict(ipl, n, p);

        // set the associated confidence (distance)
        if ((n[0] != -1) && (p[0] < CONFIDENCE)) {
            return nameLabels.get(n[0]) + " " + p[0];
        }
        else
            return "Unknown";
    }

    private IplImage MatToIplImage(Mat m)
    {
        Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bmp);
        return BitmapToIplImage(bmp);
    }

    private IplImage BitmapToIplImage(Bitmap bmp) {
        Bitmap bmp2 = Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);
        bmp = bmp2;

        IplImage image = IplImage.create(bmp.getWidth(), bmp.getHeight(),
                IPL_DEPTH_8U, 4);

        bmp.copyPixelsToBuffer(image.getByteBuffer());

        IplImage grayImg = IplImage.create(image.width(), image.height(),
                IPL_DEPTH_8U, 1);

        cvCvtColor(image, grayImg, opencv_imgproc.CV_BGR2GRAY);

        return grayImg;
    }

}
