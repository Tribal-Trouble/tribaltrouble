package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public class TargetDelegate extends ControllableCameraDelegate {
    private final @NonNull Action action;

    public TargetDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera camera, @NonNull Action action) {
        super(viewer, camera);
        this.action = action;
    }

    @Override
    public boolean canHoverBehind() {
        return true;
    }

    @Override
    protected final @NonNull CursorType getCursorType() {
        return CursorType.TARGET;
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        // Prevent base GUIObject from handling UI_ACTIVATE (Space/Return as Click)
        event.consumeAction(GameAction.UI_ACTIVATE);

        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.UI_CANCEL)) {
                pop();
                event.consume();
                return;
            }
        }

        super.handleInput(event);
        if (event.isConsumed()) return;
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
        if (button == MouseButton.LEFT) {
            getViewer().getPicker().pickTarget(getViewer().getSelection().getCurrentSelection(), getViewer().getGUIRoot().getDelegate().getCamera().getState(), getViewer().getPeerHub().getPlayerInterface(), x, y, action);
            pop();
        } else {
            super.mousePressed(button, x, y);
        }
    }
}
