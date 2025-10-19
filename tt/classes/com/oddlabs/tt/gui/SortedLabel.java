package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;

public final class SortedLabel extends Label {
	private final int index;

	public SortedLabel(String text, int index, Font font) {
		super(text, font);
		this.index = index;
	}

    @Override
	public int compareTo(Label o) {
		if (o instanceof IntegerLabel) {
			SortedLabel other = (SortedLabel)o;
			return index - other.index;
		} else
			return -1;
	}
}

