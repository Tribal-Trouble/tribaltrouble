package com.oddlabs.tt.gui;

public final class NotifyArrowData {
	private final IconQuad arrow;
	private final int head_x;
	private final int head_y;
	private final int end_x;
	private final int end_y;

	public NotifyArrowData(IconQuad arrow, int head_x, int head_y, int end_x, int end_y) {
		this.arrow = arrow;
		this.head_x = head_x;
		this.head_y = head_y;
		this.end_x = end_x;
		this.end_y = end_y;
	}

	public IconQuad getArrow() {
		return arrow;
	}

	public int getHeadX() {
		return head_x;
	}

	public int getHeadY() {
		return head_y;
	}

	public int getEndX() {
		return end_x;
	}

	public int getEndY() {
		return end_y;
	}
}
