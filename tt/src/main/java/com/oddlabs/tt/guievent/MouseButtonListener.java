package com.oddlabs.tt.guievent;

public interface MouseButtonListener extends MouseClickListener {
	void mousePressed(int button, int x, int y);
	void mouseReleased(int button, int x, int y);
	void mouseHeld(int button, int x, int y);
}
