package com.oddlabs.tt.gui;

public final class ColumnInfo {
	private final String caption;
	private final int width;

	public ColumnInfo(String caption, int width) {
		this.caption = caption;
		this.width = width;
	}

	public String getCaption() {
		return caption;
	}

	public int getWidth() {
		return width;
	}
}
