package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.Key;
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
	public final boolean keyPressed(@NonNull KeyboardEvent event) {
        if (getCamera().keyPressed(event)) return true;
        switch (event.keyCode()) {
            case ESCAPE -> {
                pop();
                return true;
            }
            case SPACE, RETURN -> {
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
	}

	@Override
	public boolean keyReleased(@NonNull KeyboardEvent event) {
		if (event.keyCode() != Key.SPACE && event.keyCode() != Key.RETURN)
			return getCamera().keyReleased(event);
		return true;
	}

	@Override
	public void mousePressed (@NonNull MouseButton button, int x, int y) {
		if (button == MouseButton.LEFT) {
			getViewer().getPicker().pickTarget(getViewer().getSelection().getCurrentSelection(), getViewer().getGUIRoot().getDelegate().getCamera().getState(), getViewer().getPeerHub().getPlayerInterface(), x, y, action);
			pop();
		} else {
			super.mousePressed(button, x, y);
		}
	}
}
