package com.oddlabs.tt.camera;


import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.util.StateChecksum;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;
import java.util.Objects;

/**
 * The View
 */
public abstract class Camera implements Animated {
    /**
     * The distance to project outwards from the screen corners when checking for landscape collisions.
     * This ensures the camera pulls up before the terrain goes off-screen.
     */
    private static final float LANDSCAPE_OFFSET = 5f;

    /**
     * Controls the interpolation speed for camera movements. A higher value results in faster, more responsive
     * movement, while a lower value provides a smoother, more dampened feel.
     */
    private static final float SMOOTHNESS_FACTOR = 15;
    /**
     * Minimum vertical distance to maintain between the camera's center and the ground below it.
     * This prevents the camera from sinking into the terrain.
     */
    private static final float GROUND_CLEARANCE = 1.0f;

    private final IntBuffer viewport = Objects.requireNonNull(BufferUtils.createIntBuffer(16));
    private final Matrix4f proj = new Matrix4f();
    private final CameraState tmp_camera = new CameraState();

    private final int[] viewportArray = new int[4];
    private final float[] hit_result_array = new float[3];

    private final @Nullable HeightMap heightmap;

    private final @NonNull CameraState state;
    private float smoothness_factor = SMOOTHNESS_FACTOR;

    public Camera(@Nullable HeightMap heightmap, @NonNull CameraState state) {
        this.heightmap = heightmap;
        this.state = state;
    }

    protected final @Nullable HeightMap getHeightMap() {
            return heightmap;
    }

    protected final void setSmoothnessFactor(float f) {
        smoothness_factor = f;
    }

    @Override
    public final void updateChecksum(@NonNull StateChecksum checksum) {
//System.out.println("camera_x = " + camera_x + " | camera_y = " + camera_y + " | camera_z = " + camera_z + " | dir_x = " + dir_x + " | dir_y = " + dir_y + " | dir_z = " + dir_z);
        state.updateChecksum(checksum);
    }

    @Override
    public final void animate(float delta_t) {
        doAnimate(delta_t);
        state.animate(delta_t, smoothness_factor);
    }

    protected abstract void doAnimate(float delta_t);

    protected final void checkPosition() {
        int mid = heightmap.getMetersPerWorld()/2;
        float dx = (state.getTargetX() - mid);
        float dy = (state.getTargetY() - mid);
        float squared_dist = dx*dx + dy*dy;
        if (squared_dist > heightmap.getMetersPerWorld()*heightmap.getMetersPerWorld()) {
                float scale = heightmap.getMetersPerWorld()/(float)Math.sqrt(squared_dist);
                state.setTargetX(dx*scale + mid);
                state.setTargetY(dy*scale + mid);
        }
        if (!bounce(state.getTargetX(), state.getTargetY(), state.getTargetZ(), state.getWidth(), state.getHeight())) {
            if (state.getTargetZ() > GameCamera.MAX_Z)
                state.setTargetZ(GameCamera.MAX_Z);
        }
    }

    protected final boolean bounce(float x, float y, float z, int width, int height) {
        boolean bounced = false;
        viewport.clear();
        viewport.put(0).put(0).put(width).put(height);
        viewport.flip();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                float fovy = Globals.FOV;
                float aspect = (float)width / height;
                float zNear = Globals.VIEW_MIN;
                float zFar = Globals.VIEW_MAX;
                proj.setPerspective((float)Math.toRadians(fovy), aspect, zNear, zFar);
                tmp_camera.set(state);
                tmp_camera.setTargetView(proj);

                Matrix4f combinedMatrix = new Matrix4f(proj).mul(tmp_camera.getModelView());
                unproject(i*width, j*height, 0f, tmp_camera.getModelView(), combinedMatrix);
                float hit_x = hit_result_array[0];
                float hit_y = hit_result_array[1];
                float hit_z = hit_result_array[2];

                float dx1 = hit_x - x;
                float dy1 = hit_y - y;
                float dz1 = hit_z - z;
                float inv_length = LANDSCAPE_OFFSET/(float)Math.sqrt(dx1*dx1 + dy1*dy1 + dz1*dz1);
                dx1 *= inv_length;
                dy1 *= inv_length;
                dz1 *= inv_length;

                float min_height = Math.max(heightmap.getNearestHeight(x + dx1, y + dy1),
                                heightmap.getSeaLevelMeters());
                hit_z = z + dz1;
                if (hit_z < min_height) {
                        bounced = true;
                        z = z + min_height - hit_z;
                }
            }
        }
        float min_height = heightmap.getNearestHeight(x, y) + GROUND_CLEARANCE;
        if (z < min_height) {
                bounced = true;
                z = min_height;
        }
        if (bounced)
                state.setTargetZ(z);
        return bounced;
    }

    private void unproject(float winx, float winy, float winz, @NonNull Matrix4f model, @NonNull Matrix4f proj) {
        // Use an absolute get to avoid changing the buffer's position
        viewport.get(0, viewportArray, 0, 4);

        Vector3f tempVector = new Vector3f();
        proj.mul(model);
        proj.unproject(winx, winy, winz, viewportArray, tempVector);

        hit_result_array[0] = tempVector.x;
        hit_result_array[1] = tempVector.y;
        hit_result_array[2] = tempVector.z;
    }

    public final @NonNull CameraState getState() {
        return state;
    }

    public final void disable() {
            LocalEventQueue.getQueue().getHighPrecisionManager().removeAnimation(this);
    }

    public void enable() {
            LocalEventQueue.getQueue().getHighPrecisionManager().registerAnimation(this);
    }

    public void keyPressed(@NonNull KeyboardEvent event) {
    }

    public void keyReleased(@NonNull KeyboardEvent event) {
    }

    public void mouseScrolled(int amount) {
    }

    public void mouseMoved(int x, int y) {
    }
}
