package com.oddlabs.tt.gui;

import org.jspecify.annotations.Nullable;

public enum MouseButton {
	LEFT,
	RIGHT,
	MIDDLE;

	public static @Nullable MouseButton fromInt(int button) {
		switch (button) {
			case 0: return LEFT;
			case 1: return RIGHT;
			case 2: return MIDDLE;
			default: return null;
		}
	}
}
