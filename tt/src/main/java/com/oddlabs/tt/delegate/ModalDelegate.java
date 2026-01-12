package com.oddlabs.tt.delegate;

import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MouseButton;
import org.jspecify.annotations.NonNull;

/**
 * Captures keyboard and mouse input
 */
public final class ModalDelegate extends Delegate {
	@Override
	public boolean keyPressed(@NonNull KeyboardEvent event) {
		return false;
	}

	@Override
	public boolean keyReleased(@NonNull KeyboardEvent event) {
		return false;
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
}
