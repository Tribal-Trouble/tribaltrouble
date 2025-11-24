package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public final class IntegerLabel extends Label {
	private final int val;

	public IntegerLabel(int val, @NonNull Font font, int width) {
		super(Integer.toString(val), font, width, Alignment.RIGHT);
		this.val = val;
	}

	public IntegerLabel(int val, @NonNull Font font) {
		super(Integer.toString(val), font);
		this.val = val;
	}

	@Override
	public int compareTo(@NonNull Label o) {
        return o instanceof IntegerLabel other ? val - other.val : -1;
	}
}
