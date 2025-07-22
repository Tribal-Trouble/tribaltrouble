package com.oddlabs.util;

import com.oddlabs.util.*;

public class TrafoState {
    public static Matrix4f matrix = new Matrix4f();
    private static Matrix4f[] mat_stack = new Matrix4f[20];
    private static int counter = 0;

    static {
        for (int i = 0; i < 20; i++) {
            mat_stack[i] = new Matrix4f();
        }
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
}
