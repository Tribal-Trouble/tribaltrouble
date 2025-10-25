package com.oddlabs.tt.camera;


public final class NullCamera extends Camera {
    public NullCamera() {
        super(null, new CameraState());
    }

    @Override
    public void doAnimate(float t) {
    }
}
