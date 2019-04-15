package com.example.mlkitocv;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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

    public String get(int i) {
        Iterator<Label> iterator = list.iterator();
        while (iterator.hasNext()) {
            Label label = iterator.next();
            if (label.num==i)
                return label.label;
        }
        return "";
    }

    public int get(String s) {
        Iterator<Label> iterator = list.iterator();
        while (iterator.hasNext()) {
            Label label = iterator.next();
            if (label.label.equalsIgnoreCase(s))
                return label.num;
        }
        return -1;
    }

    public void save() {
        try {
            File f = new File (path+"faces.txt");
            f.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            Iterator<Label> iterator = list.iterator();
            while (iterator.hasNext()) {
                Label label = iterator.next();
                bw.write(label.label + "," + label.num);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            Log.e("LABELS - SAVE",e.getMessage() + " " + e.getCause());
            e.printStackTrace();
        }
    }


    public int max() {
        int m = 0;
        Iterator<Label> iterator = list.iterator();
        while (iterator.hasNext()) {
            Label label = iterator.next();
            if (label.num > m)
                m = label.num;
        }
        Log.i("LABELS", "max: " + m);
        return m;
    }
}
