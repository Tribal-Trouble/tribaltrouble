package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public abstract class ControllableCameraDelegate extends InGameDelegate {
    private final @NonNull GameCamera game_camera;
    private FirstPersonDelegate first_person_delegate;

    public ControllableCameraDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera game_camera) {
        super(viewer, game_camera);
        this.game_camera = game_camera;
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        super.handleInput(event);
        if (event.isConsumed()) return;

        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.CAMERA_FIRST_PERSON)) {
                pushFirstPersonDelegate(true);
                event.consume();
                return;
            }
            if (event.consumeAction(GameAction.CAMERA_ZOOM_MODE)) {
                pushZoomDelegate();
                event.consume();
                return;
            }
        }
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
        if (button == MouseButton.MIDDLE) {
            pushFirstPersonDelegate(false);
        }
    }

    @Override
    public void mouseReleased(@NonNull MouseButton button, int x, int y) {
        if (button == MouseButton.MIDDLE && first_person_delegate != null) {
            first_person_delegate.mouseReleased(button, x, y);
        }
    }

    @Override
    public void mouseScrolled(int amount) {
        getCamera().mouseScrolled(amount);
    }

    @Override
    public void mouseMoved(int x, int y) {
        getCamera().mouseMoved(x, y);
    }

    @Override
    public final boolean canScroll() {
        var localInput = Renderer.getLocalInput();
        float scale = getGUIRoot().getGlobalScale();
        mouseMoved(Math.round(localInput.getMouseX() / scale), Math.round(localInput.getMouseY() / scale));
        return getGUIRoot().getModalDelegate() == null;
    }

    @Override
    public void mouseDragged(@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
        if (button == MouseButton.MIDDLE && first_person_delegate != null) {
            first_person_delegate.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
        }
    }

    private void pushFirstPersonDelegate(boolean key_pressed) {
        first_person_delegate = new FirstPersonDelegate(getViewer(), getCamera().getState(), key_pressed);
        getGUIRoot().pushDelegate(first_person_delegate);
    }

    private void pushZoomDelegate() {
        getGUIRoot().pushDelegate(new ZoomDelegate(getViewer(), game_camera));
    }
}
