package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.KeyboardEvent;
import org.jspecify.annotations.NonNull;

public interface KeyListener extends EventListener {
	boolean keyPressed(@NonNull KeyboardEvent event);
	boolean keyReleased(@NonNull KeyboardEvent event);
	boolean keyRepeat(@NonNull KeyboardEvent event);
}
