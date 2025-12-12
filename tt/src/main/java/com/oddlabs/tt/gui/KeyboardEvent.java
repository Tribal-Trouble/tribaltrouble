package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.Key;
import org.jspecify.annotations.NonNull;

public final class KeyboardEvent {
	private final @NonNull Key key;
	private final char key_char;
	private final boolean shift_down;
	private final boolean control_down;
	private final int clicks;

	public KeyboardEvent(@NonNull Key key, char key_char, boolean shift_down, boolean control_down) {
		this(key, key_char, shift_down, control_down, 1);
	}

	public KeyboardEvent(@NonNull Key key, char key_char, boolean shift_down, boolean control_down, int clicks) {
		this.key = key;
		this.key_char = key_char;
		this.shift_down = shift_down;
		this.control_down = control_down;
		this.clicks = clicks;
	}

	public @NonNull Key getKeyCode() {
		return key;
	}

	public char getKeyChar() {
		return key_char;
	}

	public boolean isShiftDown() {
		return shift_down;
	}

	public boolean isControlDown() {
		return control_down;
	}

	public int getNumClicks() {
		return clicks;
	}
}
