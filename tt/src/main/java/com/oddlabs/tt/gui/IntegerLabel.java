package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public final class IntegerLabel extends Label {
	private final int val;

	public IntegerLabel(int val, @NonNull Font font, int width) {
		super("" + val, font, width, ALIGN_RIGHT);
		this.val = val;
	}

	public IntegerLabel(int val, @NonNull Font font) {
		super("" + val, font);
		this.val = val;
	}

    @Override
	public int compareTo(Label o) {
		if (o instanceof IntegerLabel other) {
			return val - other.val;
		} else
			return -1;
	}
}

