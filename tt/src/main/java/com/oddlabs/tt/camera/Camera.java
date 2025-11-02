package com.oddlabs.tt.camera;


import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * The View
 */
public abstract class Camera implements Animated {
    private final static float LANDSCAPE_OFFSET = 5f;
    private final static float SMOOTHNESS_FACTOR = 15;

    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final Matrix4f proj = new Matrix4f();
    private final CameraState tmp_camera = new CameraState();

    // Buffers for GLU methods
    private final FloatBuffer model_buffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer proj_buffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer hit_result_buffer = BufferUtils.createFloatBuffer(3);
    private final float[] hit_result_array = new float[3];


    private final HeightMap heightmap;

    private final CameraState state;
    private float smoothness_factor = SMOOTHNESS_FACTOR;

    public Camera(HeightMap heightmap, CameraState state) {
            this.heightmap = heightmap;
            this.state = state;
    }

    protected final HeightMap getHeightMap() {
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
            if (!bounce(state.getTargetX(), state.getTargetY(), state.getTargetZ())) {
                    if (state.getTargetZ() > GameCamera.MAX_Z)
                            state.setTargetZ(GameCamera.MAX_Z);
            }
    }

    protected final boolean bounce(float x, float y, float z) {
            boolean bounced = false;
            viewport.clear();
            viewport.put(0).put(0).put(LocalInput.getViewWidth()).put(LocalInput.getViewHeight());
            viewport.flip();

            for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                            proj.setIdentity();
                            float fovy = Globals.FOV;
                            float aspect = LocalInput.getViewAspect();
                            float zNear = Globals.VIEW_MIN;
                            float zFar = Globals.VIEW_MAX;

                            // Create a perspective projection matrix manually, since LWJGL 2's Matrix4f lacks a frustum() method.
                            float yScale = 1.0f / (float) Math.tan(Math.toRadians(fovy / 2.0f));
                            float xScale = yScale / aspect;
                            float frustumLength = zFar - zNear;

                            proj.m00 = xScale;
                            proj.m11 = yScale;
                            proj.m22 = -((zFar + zNear) / frustumLength);
                            proj.m23 = -1;
                            proj.m32 = -((2 * zNear * zFar) / frustumLength);
                            proj.m33 = 0;
                            tmp_camera.set(state);
                            tmp_camera.setTargetView(proj);

                            gluUnProject(i*LocalInput.getViewWidth(),
                                            j*LocalInput.getViewHeight(),
                                            0f,
                                            tmp_camera.getModelView(), proj);
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
            float min_height = heightmap.getNearestHeight(x, y);
            if (z < min_height) {
                    bounced = true;
                    z = min_height;
            }
            if (bounced)
                    state.setTargetZ(z);
            return bounced;
    }

    private boolean gluUnProject(float winx, float winy, float winz, @NonNull Matrix4f model, @NonNull Matrix4f proj) {
        model_buffer.clear();
        model.store(model_buffer);
        model_buffer.flip();

        proj_buffer.clear();
        proj.store(proj_buffer);
        proj_buffer.flip();

        hit_result_buffer.clear();
        boolean result = GLU.gluUnProject(winx, winy, winz, model_buffer, proj_buffer, viewport, hit_result_buffer);
        hit_result_buffer.get(hit_result_array);
        return result;
    }

    public final CameraState getState() {
            return state;
    }

    public final void disable() {
            LocalEventQueue.getQueue().getHighPrecisionManager().removeAnimation(this);
    }

    public void enable() {
            LocalEventQueue.getQueue().getHighPrecisionManager().registerAnimation(this);
    }

    public void keyPressed(KeyboardEvent event) {
    }

    public void keyReleased(KeyboardEvent event) {
    }

    public void mouseScrolled(int amount) {
    }

    public void mouseMoved(int x, int y) {
    }
}
