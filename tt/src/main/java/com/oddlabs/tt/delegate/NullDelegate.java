package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.NullCamera;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MouseButton;
import org.jspecify.annotations.NonNull;

public class NullDelegate extends CameraDelegate<NullCamera> {
	private final boolean render_cursor;

	public NullDelegate(@NonNull GUIRoot gui_root, boolean render_cursor) {
		super(gui_root, new NullCamera());
		this.render_cursor = render_cursor;
	}

	@Override
	public void keyPressed(@NonNull KeyboardEvent event) {
	}

	@Override
	public void keyReleased(@NonNull KeyboardEvent event) {
	}

	@Override
	public void mouseScrolled(int amount) {
	}

	@Override
	public void mouseMoved(int x, int y) {
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
	public final boolean renderCursor() {
		return render_cursor;
	}
}
