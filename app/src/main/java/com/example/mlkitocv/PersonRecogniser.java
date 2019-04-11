package com.example.mlkitocv;

import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

public class PersonRecogniser {
    private FaceRecognizer fr;
    private String path;
    private int count = 0;

    PersonRecogniser(String path) {
        fr = LBPHFaceRecognizer.create();
        this.path = path;
    }

}
