package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.Key;
import org.jspecify.annotations.NonNull;

public record KeyboardEvent(@NonNull Key keyCode, char keyChar, boolean shiftDown, boolean controlDown, int clicks) {

	public KeyboardEvent(@NonNull Key keyCode, char keyChar, boolean shiftDown, boolean controlDown) {
		this(keyCode, keyChar, shiftDown, controlDown, 1);
	}
}
