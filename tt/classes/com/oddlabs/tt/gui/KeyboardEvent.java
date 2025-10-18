package com.oddlabs.tt.gui;

public final class KeyboardEvent {
	private final int key_code;
	private final char key_char;
	private final boolean shift_down;
	private final boolean control_down;
	private final int clicks;

	public KeyboardEvent(int key_code, char key_char, boolean shift_down, boolean control_down) {
		this(key_code, key_char, shift_down, control_down, 1);
	}

	public KeyboardEvent(int key_code, char key_char, boolean shift_down, boolean control_down, int clicks) {
		this.key_code = key_code;
		this.key_char = key_char;
		this.shift_down = shift_down;
		this.control_down = control_down;
		this.clicks = clicks;
	}

	public int getKeyCode() {
		return key_code;
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
