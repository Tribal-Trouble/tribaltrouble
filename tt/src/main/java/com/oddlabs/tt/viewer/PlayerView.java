package com.oddlabs.tt.viewer;

public final class PlayerView {
    private boolean has_camera;
    private float camera_x;
    private float camera_y;
    private float camera_z;
    private float camera_horiz_angle;
    private float camera_vert_angle;

    void setCamera(float x, float y, float z, float horiz_angle, float vert_angle) {
        has_camera = true;
        camera_x = x;
        camera_y = y;
        camera_z = z;
        camera_horiz_angle = horiz_angle;
        camera_vert_angle = vert_angle;
    }

    public boolean hasCamera() {
        return has_camera;
    }

    public float getCameraX() {
        return camera_x;
    }

    public float getCameraY() {
        return camera_y;
    }

    public float getCameraZ() {
        return camera_z;
    }

    public float getCameraHorizAngle() {
        return camera_horiz_angle;
    }

    public float getCameraVertAngle() {
        return camera_vert_angle;
    }
}
