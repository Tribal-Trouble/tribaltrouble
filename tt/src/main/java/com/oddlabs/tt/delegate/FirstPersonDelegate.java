package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.FirstPersonCamera;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public class FirstPersonDelegate extends InGameDelegate {
	private final boolean key_pressed;
	private final int created_tick;

	private boolean done = false;

	public FirstPersonDelegate(@NonNull WorldViewer viewer, CameraState camera_state, boolean key_pressed) {
		super(viewer, new FirstPersonCamera(viewer.getWorld().getHeightMap(), camera_state));
		this.key_pressed = key_pressed;
		created_tick = LocalEventQueue.getQueue().getManager().getTick();
	}

	private void release() {
		done = true;
	}

        @Override
	public void keyPressed(KeyboardEvent event) {
	}

        @Override
	public void keyReleased(@NonNull KeyboardEvent event) {
		if (key_pressed && !done) {
			switch (event.getKeyCode()) {
				case Keyboard.KEY_F:
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
	public void mouseDragged(int button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
		if (created_tick == LocalEventQueue.getQueue().getManager().getTick())
			return;
		if ((button == LocalInput.MIDDLE_BUTTON || key_pressed) && !done && getGUIRoot().getModalDelegate() == null) {
			getCamera().mouseMoved(x, y);
		}
	}

        @Override
	public void mousePressed(int button, int x, int y) {
	}

        @Override
	public void mouseReleased(int button, int x, int y) {
		if (button == LocalInput.MIDDLE_BUTTON && !key_pressed && !done) {
			pop();
		}
	}

        @Override
	protected int getCursorIndex() {
		return GUIRoot.CURSOR_NULL;
	}

        @Override
	public final void doRemove() {
		super.doRemove();
		if (!done) {
			release();
		}
	}
}
