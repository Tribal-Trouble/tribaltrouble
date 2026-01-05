package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.PointerInput;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public class ZoomDelegate extends InGameDelegate {
	private static final float ZOOM_FACTOR_CORRECTION = .25f;

	private final int start_x;
	private final int start_y;

	private final GameCamera game_camera;

	private boolean done = false;

	public ZoomDelegate(@NonNull WorldViewer viewer, GameCamera camera) {
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
	public void keyPressed(@NonNull KeyboardEvent event) {
	}

	@Override
	public void keyReleased(@NonNull KeyboardEvent event) {
		if (!done) {
            switch (event.keyCode()) {
                case Z -> pop();
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
	public void mouseDragged (@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x, int absolute_y) {
	}

	@Override
	public void mousePressed (@NonNull MouseButton button, int x, int y) {
	}

	@Override
	public void mouseReleased (@NonNull MouseButton button, int x, int y) {
	}

	@Override
	protected @NonNull CursorType getCursorType() {
		return CursorType.NULL;
	}
}
