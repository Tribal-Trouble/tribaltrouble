package com.oddlabs.tt.guievent;

public interface MouseMotionListener extends EventListener {
	void mouseDragged(int button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y);
	void mouseMoved(int x, int y);
	void mouseEntered();
	void mouseExited();
}
