package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public class Panel extends Group {
	private final @NonNull PanelTab tab;

	public Panel(@NonNull String caption) {
		tab = new PanelTab(caption);
	}

	public final @NonNull PanelTab getTab() {
		return tab;
	}

	@Override
	public final void compileCanvas() {
		Box box = Skin.getSkin().getPanelData().getBox();
		super.compileCanvas(box.getLeftOffset(), box.getBottomOffset(), box.getRightOffset(), box.getTopOffset());
	}
}
