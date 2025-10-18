package com.oddlabs.tt.camera;


public  class StaticCamera extends Camera {
    public StaticCamera(CameraState camera) {
        super(null, camera);
    }

    @Override
    public void doAnimate(float t) {
    }
}
