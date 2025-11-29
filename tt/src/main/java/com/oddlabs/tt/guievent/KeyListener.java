package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.KeyboardEvent;
import org.jspecify.annotations.NonNull;

public interface KeyListener extends EventListener {
	void keyPressed(@NonNull KeyboardEvent event);
	void keyReleased(@NonNull KeyboardEvent event);
	void keyRepeat(@NonNull KeyboardEvent event);
}
