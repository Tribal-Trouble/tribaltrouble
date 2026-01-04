package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.Key;
import org.jspecify.annotations.NonNull;

public record KeyboardEvent(@NonNull Key keyCode, char keyChar, boolean shiftDown, boolean controlDown, int clicks) {

	public KeyboardEvent(@NonNull Key key, char keyChar, boolean shiftDown, boolean controlDown) {
		this(key, keyChar, shiftDown, controlDown, 1);
	}

	public @NonNull Key getKeyCode() {
		return keyCode;
	}

	public char getKeyChar() {
		return keyChar;
	}

	public boolean isShiftDown() {
		return shiftDown;
	}

	public boolean isControlDown() {
		return controlDown;
	}

	public int getNumClicks() {
		return clicks;
	}
}
