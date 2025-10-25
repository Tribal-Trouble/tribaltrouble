package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;

public final class GroupData {
	private final Box group;
	private final int caption_left;
	private final int caption_y;
	private final int caption_offset;
	private final Font caption_font;

	public GroupData(Box group, int caption_left, int caption_y, int caption_offset, Font caption_font) {
		this.group = group;
		this.caption_left = caption_left;
		this.caption_y = caption_y;
		this.caption_offset = caption_offset;
		this.caption_font = caption_font;
	}

	public Box getGroup() {
		return group;
	}

	public int getCaptionLeft() {
		return caption_left;
	}

	public int getCaptionY() {
		return caption_y;
	}

	public int getCaptionOffset() {
		return caption_offset;
	}

	public Font getCaptionFont() {
		return caption_font;
	}
}
