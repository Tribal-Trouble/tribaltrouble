package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public abstract class RadioButtonGroupElement extends GUIObject {
	private boolean marked = false;

	private final @NonNull RadioButtonGroup group;

	public RadioButtonGroupElement(boolean marked, @NonNull RadioButtonGroup group) {
		this.group = group;
		group.add(this);
		if (marked)
			group.mark(this);
	}

	public final boolean isMarked() {
		return marked;
	}

	protected final void setMarked(boolean marked) {
		this.marked = marked;
	}

	@Override
	protected void mouseClicked(int button, int x, int y, int clicks) {
		group.mark(this);
	}
}
