package com.oddlabs.tt.camera;

import com.oddlabs.tt.delegate.SelectionDelegate;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.RadialFogInfo;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * Camera for showing the island overview map.
 */
public final class MapCamera extends Camera {
    private static final float MAP_THRESHOLD = .1f;
    private static final float MAP_TIME_FACTOR = 1000f;
    private static final float SMOOTHNESS_FACTOR = 200f;
    public static final float MAP_Z_FACTOR = 1.3f;

    private enum MapMode {
        TO_MAP,
        IN_MAP,
        FROM_MAP
    }

    private final @NonNull SelectionDelegate delegate;
    private final @NonNull CameraState original_camera_state;
    private final float distance_to_landscape;
    private final Label label = new Label(Utils.getBundleString(ResourceBundle.getBundle(MapCamera.class.getName()), "map_mode"), Skin.getSkin().getHeadlineFont());

    private @NonNull MapMode map_mode = MapMode.TO_MAP;

    public MapCamera(@NonNull SelectionDelegate delegate, @NonNull GameCamera old_camera) {
        original_camera_state = old_camera.getState();
        FogInfo radialFog = new RadialFogInfo(Color.WHITE, 0.25f);
        CameraState mapCameraState = new CameraState(radialFog);
        mapCameraState.set(old_camera.getState());
        mapCameraState.setFog(radialFog);
        super(old_camera.getHeightMap(), mapCameraState);
        this.delegate = delegate;
        float[] target = old_camera.getRotationPoint();
        float dx = target[0] - original_camera_state.getTargetX();
        float dy = target[1] - original_camera_state.getTargetY();
        float dz = getHeightMap().getNearestHeight(target[0], target[1]) - original_camera_state.getTargetZ();
        distance_to_landscape = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);

        setSmoothnessFactor(SMOOTHNESS_FACTOR);
    }

    @Override
    public void doAnimate(float t) {
        float factor = t*1000f/Math.max(t*1000f, Settings.getSettings().mapmode_delay*MAP_TIME_FACTOR);
        float dx;
        float dy;
        float dz;
        float da;
        float map_x = getHeightMap().getMetersPerWorld()/2;
        float map_y = getHeightMap().getMetersPerWorld()/2;
        float map_z = getHeightMap().getMetersPerWorld()*MAP_Z_FACTOR;

        switch (map_mode) {
            case TO_MAP -> {
                dx = map_x - original_camera_state.getTargetX();
                dy = map_y - original_camera_state.getTargetY();
                dz = map_z - original_camera_state.getTargetZ();
                CameraState state = getState();
                state.setTargetX(getState().getTargetX() + dx * factor);
                state.setTargetY(getState().getTargetY() + dy * factor);
                state.setTargetZ(getState().getTargetZ() + dz * factor);
                if (state.getTargetZ() > map_z - MAP_THRESHOLD) {
                    state.setTargetX(map_x);
                    state.setTargetY(map_y);
                    state.setTargetZ(map_z);
                    changeMode(MapMode.IN_MAP);
                }

                da = CameraState.MIN_ANGLE - original_camera_state.getTargetVertAngle();
                state.setTargetVertAngle(state.getTargetVertAngle() + da * factor);
            }
            case IN_MAP -> {
            }
            case FROM_MAP -> {
                dx = original_camera_state.getTargetX() - map_x;
                dy = original_camera_state.getTargetY() - map_y;
                dz = original_camera_state.getTargetZ() - map_z;
                CameraState camera_state = getState();
                camera_state.setTargetX(getState().getTargetX() + dx * factor);
                camera_state.setTargetY(getState().getTargetY() + dy * factor);
                camera_state.setTargetZ(getState().getTargetZ() + dz * factor);
                if (camera_state.getTargetZ() <= original_camera_state.getTargetZ()) {
                    camera_state.setTargetX(original_camera_state.getTargetX());
                    camera_state.setTargetY(original_camera_state.getTargetY());
                    camera_state.setTargetZ(original_camera_state.getTargetZ());
                    camera_state.setTargetVertAngle(original_camera_state.getTargetVertAngle());
                    checkPosition();
                    delegate.exitMapMode();
                    break;
                }

                da = original_camera_state.getTargetVertAngle() - CameraState.MIN_ANGLE;
                camera_state.setTargetVertAngle(getState().getTargetVertAngle() + da * factor);
            }
        }
    }

    private void changeMode(@NonNull MapMode mode) {
        map_mode = mode;
        switch (mode) {
            case TO_MAP -> {
            }
            case IN_MAP -> {
                label.setPos((delegate.getGUIRoot().getWidth() - label.getWidth()) / 2, delegate.getGUIRoot().getHeight() - label.getHeight());
                delegate.addChild(label);
                getState().setNoDetailMode(true);
            }
            case FROM_MAP -> {
                label.remove();
                getState().setNoDetailMode(false);
            }
        }
    }

    public void mapGoto(float x, float y) {
        this.mapGoto(x, y, false);
    }

    public void mapGoto(float x, float y, boolean override) {
        if (map_mode == MapMode.IN_MAP || override) {
            //	float radius = (float)Math.cos(getVertAngle());
            //	float old_dir_x = (float)Math.cos(getHorizAngle())*radius;
            //	float old_dir_y = (float)Math.sin(getHorizAngle())*radius;
            //	float old_dir_z = (float)Math.sin(getVertAngle());
            float radius = (float)Math.cos(original_camera_state.getTargetVertAngle());
            float old_dir_x = (float)Math.cos(getState().getHorizAngle())*radius;
            float old_dir_y = (float)Math.sin(getState().getHorizAngle())*radius;
            float old_dir_z = (float)Math.sin(original_camera_state.getTargetVertAngle());
            // Adjust the position of the original camera.
            original_camera_state.setTargetX(x - old_dir_x*distance_to_landscape);
            original_camera_state.setTargetY(y - old_dir_y*distance_to_landscape);
            original_camera_state.setTargetZ(getHeightMap().getNearestHeight(x, y) - old_dir_z*distance_to_landscape);
            changeMode(MapMode.FROM_MAP);
        }
    }

    @Override
    public void keyPressed(@NonNull KeyboardEvent event) {
        switch (event.keyCode()) {
            case SPACE, NUMPAD5 ->
                    changeMode((map_mode == MapMode.TO_MAP || map_mode == MapMode.IN_MAP) ? MapMode.FROM_MAP : MapMode.TO_MAP);
        }
    }
}
