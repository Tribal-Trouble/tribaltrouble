package com.oddlabs.tt.camera;

import com.oddlabs.tt.delegate.SelectionDelegate;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.RadialFogInfo;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Camera for showing the island overview map.
 */
public final class MapCamera extends Camera {
    private static final float MAP_THRESHOLD = .1f;
    private static final float MAP_TIME_FACTOR = 1000f;
    private static final float SMOOTHNESS_FACTOR = 200f;
    public static final float MAP_Z_FACTOR = 1.3f;
    private static final float LANDING_ANGLE = -(float) Math.PI / 4f;
    private static final float MIN_LANDING_DISTANCE = 20f;

    private static final Set<GameAction> BLOCKED_ACTIONS = EnumSet.of(
            GameAction.CAMERA_PAN_LEFT,
            GameAction.CAMERA_PAN_RIGHT,
            GameAction.CAMERA_PAN_UP,
            GameAction.CAMERA_PAN_DOWN,
            GameAction.CAMERA_PITCH_UP,
            GameAction.CAMERA_PITCH_DOWN,
            GameAction.CAMERA_ROTATE_LEFT,
            GameAction.CAMERA_ROTATE_RIGHT,
            GameAction.CAMERA_ZOOM_IN,
            GameAction.CAMERA_ZOOM_OUT,
            GameAction.CAMERA_FIRST_PERSON,
            GameAction.CAMERA_ZOOM_MODE
    );

    private enum MapMode {
        TO_MAP,
        IN_MAP,
        FROM_MAP
    }

    private final @NonNull SelectionDelegate delegate;
    private final @NonNull CameraState original_camera_state;
    private final Label label = new Label(Utils.getBundleString(ResourceBundle.getBundle(MapCamera.class.getName()),
            "map_mode"), Skin.getSkin().getHeadlineFont());

    private @NonNull MapMode map_mode = MapMode.TO_MAP;
    private float fogTime = 0f;

    public MapCamera(@NonNull SelectionDelegate delegate, @NonNull GameCamera old_camera) {
        original_camera_state = old_camera.getState();
        FogInfo radialFog = new RadialFogInfo(Color.WHITE, 0.25f);
        CameraState mapCameraState = new CameraState(radialFog);
        mapCameraState.set(old_camera.getState());
        mapCameraState.setFog(radialFog);
        super(old_camera.getHeightMap(), mapCameraState);
        this.delegate = delegate;

        setSmoothnessFactor(SMOOTHNESS_FACTOR);
        getState().setNoDetailMode(true);
    }

    @Override
    public void doAnimate(float t) {
        float factor = t * 1000f / Math.max(t * 1000f, Settings.getSettings().mapmode_delay * MAP_TIME_FACTOR);
        float dx;
        float dy;
        float dz;
        float da;
        float map_x = getHeightMap().getMetersPerWorld() / 2f;
        float map_y = getHeightMap().getMetersPerWorld() / 2f;
        float map_z = getHeightMap().getMetersPerWorld() * MAP_Z_FACTOR;
        float start_z = original_camera_state.getTargetZ();

        // Calculate transition progress (0.0 = at start, 1.0 = at map)
        float current_z = getState().getTargetZ();
        float total_dist = map_z - start_z;
        float progress = (Math.abs(total_dist) > 0.001f) ? Math.clamp((current_z - start_z) / total_dist, 0f, 1f) : 1f;

        // Base organic fog pulse (sum of sines for non-predictable period)
        fogTime += t;
        float pulse = (float) (Math.sin(fogTime * 0.4125f) * 0.5 + Math.sin(fogTime * 0.8625f) * 0.3 + Math.sin(
                fogTime * 1.7625f) * 0.2);
        float baseDensity = 0.30f + pulse * 0.12f;

        // Apply transition
        // 0% to 25%: Keep original distance fog to hide horizon near ground
        // 25% to 100%: Fade in Radial Fog (vignette)
        if (progress < 0.25f) {
            getState().setFog(original_camera_state.getFog());
        } else {
            float fade = (progress - 0.25f) / 0.75f;
            // Radius shrinks from 1.5x to 1.0x as we ascend
            float radiusScale = 1.5f - (0.5f * fade);
            getState().setFog(new RadialFogInfo(Color.WHITE, baseDensity * fade, radiusScale));
        }
        // The fog should be disabled here because it shows this strange ring around the map
        getState().getFog().setEnabled(false);

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
                    getState().setNoDetailMode(false);
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
                label.setPos((delegate.getGUIRoot().getWidth() - label.getWidth()) / 2,
                        delegate.getGUIRoot().getHeight() - label.getHeight());
                delegate.addChild(label);
            }
            case FROM_MAP -> {
                label.remove();
            }
        }
    }

    public void mapGoto(float x, float y) {
        this.mapGoto(x, y, false);
    }

    public void mapGoto(float x, float y, boolean override) {
        if (map_mode == MapMode.IN_MAP || override) {
            // Land the eye near the click using a fixed 45° angle (avoids weird views
            // when the prior tilt was near horizontal). Preserve the user's absolute
            // eye Z so a fully-zoomed-out camera stays fully zoomed out across the
            // teleport — matching what they'd see if they scrolled there manually,
            // since scroll/bounce only push the eye up, never down. Then derive
            // landing_distance so the 45° ray from the eye hits the clicked point.
            float clickGroundZ = getHeightMap().getNearestHeight(x, y);
            float sinDown = -(float) Math.sin(LANDING_ANGLE);
            float minEyeZ = clickGroundZ + MIN_LANDING_DISTANCE * sinDown;
            float landingEyeZ = Math.min(
                    Math.max(minEyeZ, original_camera_state.getTargetZ()),
                    GameCamera.MAX_Z);
            float landing_distance = Math.max(MIN_LANDING_DISTANCE, (landingEyeZ - clickGroundZ) / sinDown);
            float radius = (float) Math.cos(LANDING_ANGLE);
            float old_dir_x = (float) Math.cos(getState().getHorizAngle()) * radius;
            float old_dir_y = (float) Math.sin(getState().getHorizAngle()) * radius;
            // Adjust the position of the original camera.
            original_camera_state.setTargetX(x - old_dir_x * landing_distance);
            original_camera_state.setTargetY(y - old_dir_y * landing_distance);
            original_camera_state.setTargetZ(landingEyeZ);
            changeMode(MapMode.FROM_MAP);
        }
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        // Block camera controls and delegate switches while in map mode
        for (GameAction action : BLOCKED_ACTIONS) {
            if (event.consumeAction(action)) {
                event.consume();
                return;
            }
        }

        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.CAMERA_MAP_MODE)) {
                changeMode((map_mode == MapMode.TO_MAP
                        || map_mode == MapMode.IN_MAP) ? MapMode.FROM_MAP : MapMode.TO_MAP);
                event.consume();
            }
        }
    }
}
