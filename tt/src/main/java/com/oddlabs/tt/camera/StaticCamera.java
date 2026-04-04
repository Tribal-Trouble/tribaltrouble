package com.oddlabs.tt.camera;


import org.jspecify.annotations.NonNull;

public class StaticCamera extends Camera {
    public StaticCamera(@NonNull CameraState camera) {
        super(null, camera);
    }

    @Override
    public void doAnimate(float t) {
    }
}
