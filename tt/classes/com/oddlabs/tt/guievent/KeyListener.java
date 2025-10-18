package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.KeyboardEvent;

public interface KeyListener extends EventListener {
	void keyPressed(KeyboardEvent event);
	void keyReleased(KeyboardEvent event);
	void keyRepeat(KeyboardEvent event);
}
