package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class MultiColumnComboBoxData {
	private final @NonNull Box box;
	private final @NonNull Horizontal button_pressed;
	private final @NonNull Horizontal button_unpressed;
	private final @NonNull ModeIconQuads descending;
	private final @NonNull ModeIconQuads ascending;
	private final @NonNull Vector4fc color1;
	private final @NonNull Vector4fc color2;
	private final @NonNull Vector4fc color_marked;
	private final @NonNull Font font;
	private final int caption_offset;

	MultiColumnComboBoxData(@NonNull Box box,
                                   @NonNull Horizontal button_pressed,
                                   @NonNull Horizontal button_unpressed,
                                   @NonNull ModeIconQuads descending,
                                   @NonNull ModeIconQuads ascending,
                                   int color1,
                                   int color2,
                                   int color_marked,
                                   @NonNull Font font,
                                   int caption_offset) {
		this.box = box;
		this.button_pressed = button_pressed;
		this.button_unpressed = button_unpressed;
		this.descending = descending;
		this.ascending = ascending;
		this.color1 = com.oddlabs.util.Color.argb4v(color1);
		this.color2 = com.oddlabs.util.Color.argb4v(color2);
		this.color_marked = com.oddlabs.util.Color.argb4v(color_marked);
		this.font = font;
		this.caption_offset = caption_offset;
	}

	public @NonNull Box getBox() {
		return box;
	}

	public @NonNull Horizontal getButtonPressed() {
		return button_pressed;
	}

	public @NonNull Horizontal getButtonUnpressed() {
		return button_unpressed;
	}

	public @NonNull ModeIconQuads getDescending() {
		return descending;
	}

	public @NonNull ModeIconQuads getAscending() {
		return ascending;
	}

	public @NonNull Vector4fc getColor1() {
		return color1;
	}

	public @NonNull Vector4fc getColor2() {
		return color2;
	}

	public @NonNull Vector4fc getColorMarked() {
		return color_marked;
	}

	public @NonNull Font getFont() {
		return font;
	}

	public int getCaptionOffset() {
		return caption_offset;
	}
}
