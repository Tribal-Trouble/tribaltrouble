package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.FirstPersonCamera;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public class FirstPersonDelegate extends InGameDelegate {
	private final boolean key_pressed;
	private final int created_tick;

	private boolean done = false;

	public FirstPersonDelegate(@NonNull WorldViewer viewer, @NonNull CameraState camera_state, boolean key_pressed) {
		super(viewer, new FirstPersonCamera(viewer, viewer.getWorld().getHeightMap(), camera_state));
		this.key_pressed = key_pressed;
		created_tick = LocalEventQueue.getQueue().getManager().getTick();
	}

	private void release() {
		done = true;
	}

	@Override
	public final void doRemove() {
		super.doRemove();
		if (!done) {
			release();
		}
	}

	@Override
	public void keyPressed(@NonNull KeyboardEvent event) {
	}

	@Override
	public void keyReleased(@NonNull KeyboardEvent event) {
		if (key_pressed && !done) {
			switch (event.keyCode()) {
				case F:
					pop();
					break;
			}
		}
	}

	@Override
	public void mouseScrolled(int amount) {
	}

	@Override
	public void mouseMoved(int x, int y) {
		if (!done)
			getCamera().mouseMoved(x, y);
	}

	@Override
	public void mouseDragged (@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		if (created_tick == LocalEventQueue.getQueue().getManager().getTick())
			return;
		if ((button == MouseButton.MIDDLE || key_pressed) && !done && getGUIRoot().getModalDelegate() == null) {
			getCamera().mouseMoved(x, y);
		}
	}

	@Override
	public void mousePressed (@NonNull MouseButton button, int x, int y) {
	}

	@Override
	public void mouseReleased (@NonNull MouseButton button, int x, int y) {
		if (button == MouseButton.MIDDLE && !key_pressed && !done) {
			pop();
		}
	}

	@Override
	protected @NonNull CursorType getCursorType() {
		return CursorType.NULL;
	}
}
