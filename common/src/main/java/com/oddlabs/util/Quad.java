package com.oddlabs.util;

import java.io.Serial;
import java.io.Serializable;

public class Quad implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    protected final float u1;
    protected final float v1;
    protected final float u2;
    protected final float v2;
    protected final int width;
    protected final int height;

    public Quad(float u1, float v1, float u2, float v2, int width, int height) {
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        this.width = width;
        this.height = height;
    }

    public float getU1() {
        return u1;
    }

    public float getV1() {
        return v1;
    }

    public float getU2() {
        return u2;
    }

    public float getV2() {
        return v2;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
