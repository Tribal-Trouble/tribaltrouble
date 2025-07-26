package com.oddlabs.util;

import com.oddlabs.util.*;

public class TrafoState {
    public static Matrix4f matrix = new Matrix4f();
    public static float[] color = new float[4];
    private static Matrix4f[] mat_stack = new Matrix4f[20];
    private static int counter = 0;
    private static int width;
    private static int height;

    static {
        for (int i = 0; i < 20; i++) {
            mat_stack[i] = new Matrix4f();
        }
        setColor(1f, 1f, 1f, 1f);
    }

    public static int getWidth() { return width; }
    public static int getHeight() { return height; }

    public static void setResolution(int w, int h) {
        width = w;
        height = h;
    }

    private static void recalc() {
        matrix.setIdentity();
        for (int i = 0; i < counter; i++) {
            Matrix4f.mul(mat_stack[i], matrix, matrix);
        }
    }

    public static void pushMatrix(Matrix4f mat) {
        mat_stack[counter].load(mat);
        counter++;
        assert counter <= 20;
        recalc();
    }

    public static void popMatrix() {
        counter--;
        assert counter >= 0;
        recalc();  
    }

    public static void setColor(float r, float g, float b, float a) {
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
    }
}
