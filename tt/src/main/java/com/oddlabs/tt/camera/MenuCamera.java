package com.oddlabs.tt.camera;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;

public final class MenuCamera extends Camera {
    private final static float ANGLE_DELTA = 0.020f;
    private final static float RADIUS = 176f;
    private final static float HEIGHT = 0f;
    private final static float LANDSCAPE_OFFSET = 5f;
    private final static float CENTER_X = 128f;
    private final static float CENTER_Y = 128f;
    private final static float CENTER_Z = 128f; // NOT HEIGHT!

    private final World world;
    private final AnimationManager manager;
    private float center_angle;

    public MenuCamera(World world, AnimationManager manager) {
            super(world.getHeightMap(), new CameraState());
            this.world = world;
            this.manager = manager;
            reset();
    }

    private void reset() {
            center_angle = 1;
            getState().setCurrentVertAngle(-(float)Math.atan((HEIGHT - CENTER_Z)/RADIUS));
            updatePos(0f);
    }

    private void updatePos(float t) {
            center_angle = (center_angle + ANGLE_DELTA*t)%(2*(float)Math.PI);
            getState().setCurrentX(CENTER_X + RADIUS*(float)Math.cos(center_angle));
            getState().setCurrentY(CENTER_Y + RADIUS*(float)Math.sin(center_angle));
            getState().setCurrentHorizAngle((float)Math.PI*.925f + center_angle);
            getState().setCurrentZ(LANDSCAPE_OFFSET);
    }

    @Override
    public void doAnimate(float t) {
            updatePos(t);
            world.tick(t);
            manager.runAnimations(t);
    }
}
