package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.input.PointerInput;
import com.oddlabs.tt.viewer.WorldViewer;
import org.lwjgl.input.Keyboard;

public class ZoomDelegate extends InGameDelegate {
	private final static float ZOOM_FACTOR_CORRECTION = .25f;

	private final int start_x;
	private final int start_y;

	private final GameCamera game_camera;

	private boolean done = false;

	public ZoomDelegate(WorldViewer viewer, GameCamera camera) {
		super(viewer, camera);
		game_camera = camera;
		start_x = LocalInput.getMouseX();
		start_y = LocalInput.getMouseY();
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
	public void keyPressed(KeyboardEvent event) {
	}

        @Override
	public void keyReleased(KeyboardEvent event) {
		if (!done) {
			switch (event.getKeyCode()) {
				case Keyboard.KEY_Z:
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
		if (!done) {
			int dy = y - start_y;

			float zoom_factor = dy*ZOOM_FACTOR_CORRECTION;
			game_camera.zoom(zoom_factor);
			PointerInput.setCursorPosition(start_x, start_y);
		}
	}

        @Override
	public void mouseDragged(int button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
	}

        @Override
	public void mousePressed(int button, int x, int y) {
	}

        @Override
	public void mouseReleased(int button, int x, int y) {
	}

        @Override
	protected int getCursorIndex() {
		return GUIRoot.CURSOR_NULL;
	}
}
