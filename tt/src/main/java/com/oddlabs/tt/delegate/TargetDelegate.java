package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public class TargetDelegate extends ControllableCameraDelegate {
	private final int action;

	public TargetDelegate(@NonNull WorldViewer viewer, GameCamera camera, int action) {
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
			case Keyboard.KEY_ESCAPE:
				pop();
				break;
			case Keyboard.KEY_SPACE:
			case Keyboard.KEY_RETURN:
				break;
			default:
				super.keyPressed(event);
				break;
		}
	}

	@Override
	public void keyReleased(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() != Keyboard.KEY_SPACE || event.getKeyCode() != Keyboard.KEY_RETURN)
			getCamera().keyReleased(event);
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		if (button == LocalInput.LEFT_BUTTON) {
			getViewer().getPicker().pickTarget(getViewer().getSelection().getCurrentSelection(), getViewer().getGUIRoot().getDelegate().getCamera().getState(), getViewer().getPeerHub().getPlayerInterface(), x, y, action);
			pop();
		} else {
			super.mousePressed(button, x, y);
		}
	}
}
