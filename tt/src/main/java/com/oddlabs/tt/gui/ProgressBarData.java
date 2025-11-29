package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public final class ProgressBarData {
	private final @NonNull Horizontal progress_bar;
	private final @NonNull ModeIconQuads left_fill;
	private final @NonNull ModeIconQuads center_fill;
	private final @NonNull ModeIconQuads right_fill;
	private final @NonNull Font font;

	public ProgressBarData(@NonNull Horizontal progress_bar, @NonNull ModeIconQuads left_fill, @NonNull ModeIconQuads center_fill, @NonNull ModeIconQuads right_fill, @NonNull Font font) {
		this.progress_bar = progress_bar;
		this.left_fill = left_fill;
		this.center_fill = center_fill;
		this.right_fill = right_fill;
		this.font = font;
	}

	public @NonNull Horizontal getProgressBar() {
		return progress_bar;
	}

	public @NonNull ModeIconQuads getLeftFill() {
		return left_fill;
	}

	public @NonNull ModeIconQuads getCenterFill() {
		return center_fill;
	}

	public @NonNull ModeIconQuads getRightFill() {
		return right_fill;
	}

	public @NonNull Font getFont() {
		return font;
	}
}
