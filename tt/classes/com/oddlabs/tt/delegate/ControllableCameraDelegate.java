package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.viewer.WorldViewer;

public abstract strictfp class ControllableCameraDelegate extends InGameDelegate {
    private final GameCamera game_camera;
    private FirstPersonDelegate first_person_delegate;

    public ControllableCameraDelegate(WorldViewer viewer, GameCamera game_camera) {
        super(viewer, game_camera);
        this.game_camera = game_camera;
    }

    public void keyPressed(KeyboardEvent event) {
        Settings settings = Settings.getSettings();
        int key = event.getKeyCode();
        if (key == settings.getKeybind(Globals.KB_CAMERA_FIRST_PERSON_TOGGLE)) {
            pushFirstPersonDelegate(true);
        } else if (key == settings.getKeybind(Globals.KB_CAMERA_ZOOM_HOLD)) {
            pushZoomDelegate();
        } else {
            super.keyPressed(event);
        }
    }

    public void mousePressed(int button, int x, int y) {
        if (button == LocalInput.MIDDLE_BUTTON) {
            pushFirstPersonDelegate(false);
        }
    }

    public void mouseReleased(int button, int x, int y) {
        if (button == LocalInput.MIDDLE_BUTTON && first_person_delegate != null) {
            first_person_delegate.mouseReleased(button, x, y);
        }
    }

    public void mouseScrolled(int amount) {
        getCamera().mouseScrolled(amount);
    }

    public void mouseMoved(int x, int y) {
        getCamera().mouseMoved(x, y);
    }

    public final boolean canScroll() {
        mouseMoved(LocalInput.getMouseX(), LocalInput.getMouseY());
        return getGUIRoot().getModalDelegate() == null;
    }

    public void mouseDragged(
            int button,
            int x,
            int y,
            int relative_x,
            int relative_y,
            int absolute_x,
            int absolute_y) {
        if (button == LocalInput.MIDDLE_BUTTON && first_person_delegate != null) {
            first_person_delegate.mouseDragged(
                    button, x, y, relative_x, relative_y, absolute_x, absolute_y);
        }
    }

    private final void pushFirstPersonDelegate(boolean key_pressed) {
        first_person_delegate =
                new FirstPersonDelegate(getViewer(), getCamera().getState(), key_pressed);
        getGUIRoot().pushDelegate(first_person_delegate);
    }

    private final void pushZoomDelegate() {
        getGUIRoot().pushDelegate(new ZoomDelegate(getViewer(), game_camera));
    }
}
