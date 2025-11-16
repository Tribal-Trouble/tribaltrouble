package com.oddlabs.tt.camera;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import org.jspecify.annotations.NonNull;

public final class MenuCamera extends Camera {
    private static final float ANGLE_DELTA = 0.020f;
    private static final float RADIUS = 176f;
    private static final float HEIGHT = 0f;
    private static final float LANDSCAPE_OFFSET = 5f;
    private static final float CENTER_X = 128f;
    private static final float CENTER_Y = 128f;
    private static final float CENTER_Z = 128f; // NOT HEIGHT!

    private final @NonNull World world;
    private final @NonNull AnimationManager manager;
    private float center_angle;

    public MenuCamera(@NonNull World world, @NonNull AnimationManager manager) {
        super(world.getHeightMap(), new CameraState());
        this.world = world;
        this.manager = manager;
        reset();
    }

    private void reset() {
        center_angle = 1;
        getState().setCurrentVertAngle(-(float) Math.atan((HEIGHT - CENTER_Z) / RADIUS));
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
