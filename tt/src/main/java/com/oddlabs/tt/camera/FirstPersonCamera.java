package com.oddlabs.tt.camera;

import org.jspecify.annotations.NullMarked;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputManager;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.viewer.WorldViewer;

@NullMarked
public final class FirstPersonCamera extends Camera {
    /** Radians of view rotation per physical pixel of mouse movement. */
    private static final float MOUSE_LOOK_SPEED = .002f;
    /** Radians of view rotation per second while a look key is held. */
    private static final float KEY_LOOK_SPEED = (float) (Math.PI / 2);

    private final WorldViewer viewer;
    private final LocalInput localInput = Renderer.getLocalInput();

    /** Cursor position in physical pixels at activation. */
    private final int physicalStartX;
    private final int physicalStartY;

    public FirstPersonCamera(WorldViewer viewer, HeightMap heightmap, CameraState camera) {
        super(heightmap, camera);
        this.viewer = viewer;
        physicalStartX = localInput.getMouseX();
        physicalStartY = localInput.getMouseY();
    }

    @Override
    public void doAnimate(float dt) {
        updateKeyboardLook(dt);
        updateKeyboardPan(dt);
    }

    @Override
    public void mouseMoved(int x, int y) {
        // Use physical coordinates from LocalInput rather than the logical (GUI-scaled) x/y
        // parameters, so look sensitivity is independent of UI scale and matches the
        // coordinate space used by setCursorPosition().
        int dx = localInput.getMouseX() - physicalStartX;
        int dy = localInput.getMouseY() - physicalStartY;
        if (dx == 0 && dy == 0) {
            return;
        }
        yaw(-dx * MOUSE_LOOK_SPEED);
        pitch(dy * MOUSE_LOOK_SPEED);
        localInput.getPointerInput().setCursorPosition(physicalStartX, physicalStartY);
    }

    /**
     * Turns the view horizontally.
     *
     * Positive turns the view left; negative turns right.
     */
    private void yaw(float radians) {
        if (Settings.getSettings().invert_camera_yaw) {
            radians *= -1;
        }
        getState().setTargetHorizAngle(getState().getTargetHorizAngle() + radians);
    }

    /** Tilts the view vertically. */
    private void pitch(float radians) {
        if (Settings.getSettings().invert_camera_pitch) {
            radians *= -1;
        }
        getState().setTargetVertAngle(getState().getTargetVertAngle() + radians);
    }

    private void updateKeyboardLook(float dt) {
        if (isKeyboardBlocked()) {
            return;
        }

        InputManager inputManager = localInput.getInputManager();
        float amount = dt * KEY_LOOK_SPEED;
        yaw(amount * axis(inputManager, GameAction.CAMERA_ROTATE_LEFT, GameAction.CAMERA_ROTATE_RIGHT));
        pitch(amount * axis(inputManager, GameAction.CAMERA_PITCH_UP, GameAction.CAMERA_PITCH_DOWN));
    }

    /** Moves the camera along the ground relative to the view direction, at a speed proportional to its height. */
    private void updateKeyboardPan(float dt) {
        if (isKeyboardBlocked()) {
            return;
        }

        InputManager inputManager = localInput.getInputManager();
        int moveRight = axis(inputManager, GameAction.CAMERA_PAN_RIGHT, GameAction.CAMERA_PAN_LEFT);
        int moveForward = axis(inputManager, GameAction.CAMERA_PAN_UP, GameAction.CAMERA_PAN_DOWN);
        if (moveRight == 0 && moveForward == 0) {
            return;
        }

        float heading = getState().getTargetHorizAngle();
        float forwardX = (float) Math.cos(heading);
        float forwardY = (float) Math.sin(heading);
        float rightX = forwardY;
        float rightY = -forwardX;
        float distance = getState().getTargetZ() * dt;
        getState().setTargetX(getState().getTargetX() + (moveForward * forwardX + moveRight * rightX) * distance);
        getState().setTargetY(getState().getTargetY() + (moveForward * forwardY + moveRight * rightY) * distance);

        checkPosition();
    }

    /** @return true if a text field or modal window has keyboard focus, so camera keys should be ignored. */
    private boolean isKeyboardBlocked() {
        return viewer.getGUIRoot().getDelegate().keyboardBlocked() || viewer.getGUIRoot().getModalDelegate() != null;
    }

    /** @return 1 if only the positive action is active, -1 if only the negative one is, otherwise 0. */
    private static int axis(InputManager inputManager, GameAction positive, GameAction negative) {
        int pos = inputManager.isActive(positive) ? 1 : 0;
        int neg = inputManager.isActive(negative) ? 1 : 0;
        return pos - neg;
    }
}
