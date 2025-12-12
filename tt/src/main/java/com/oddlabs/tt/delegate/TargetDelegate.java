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
	public final void keyPressed(@NonNull KeyboardEvent event) {
		getCamera().keyPressed(event);
        switch (event.getKeyCode()) {
            case ESCAPE -> pop();
            case SPACE, RETURN -> {
            }
            default -> super.keyPressed(event);
        }
	}

	@Override
	public void keyReleased(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() != Key.SPACE || event.getKeyCode() != Key.RETURN)
			getCamera().keyReleased(event);
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
