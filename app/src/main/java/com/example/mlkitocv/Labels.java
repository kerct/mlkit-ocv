package com.example.mlkitocv;

import java.util.ArrayList;

public class Labels{
    String path;
    class Label {
        String label;
        int num;

        public Label(String s, int n) {
            label = s;
            num = n;
        }
    }
    ArrayList<Label> list = new ArrayList<>();

    public Labels(String path) {
        this.path = path;
    }

    public void add(String s, int n){
        list.add(new Label(s, n));
    }
}
