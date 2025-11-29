package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.MouseButton;
import org.jspecify.annotations.NonNull;

public interface MouseMotionListener extends EventListener {
	void mouseDragged(@NonNull MouseButton button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y);
	void mouseMoved(int x, int y);
	void mouseEntered();
	void mouseExited();
}
