package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public class PanelTab extends GUIObject {
	private static final float[] HIGHLIGHT_COLOR = com.oddlabs.util.Color.argb4f(0x00_FF_00_FF);
	private boolean selected;
	private final @NonNull Label label;

	public PanelTab(@NonNull String caption) {
		PanelData data = Skin.getSkin().getPanelData();
		Font font = Skin.getSkin().getButtonFont();
		label = new Label(caption, font);
		label.setPos(data.getLeftCaptionOffset(), (data.getTab().getHeight() - font.getHeight())/2 + data.getBottomCaptionOffset());
		addChild(label);
		setDim(data.getLeftCaptionOffset() + label.getWidth() + data.getRightCaptionOffset(), data.getTab().getHeight());
		setCanFocus(true);
	}

	public final void select(boolean selected) {
		this.selected = selected;
		focusNotifyAll(false);
		if (selected)
			label.setColor(Label.DEFAULT_COLOR);
	}

	public final int getRenderState() {
        return isDisabled() ? Skin.DISABLED : isActive() || selected ? Skin.ACTIVE : Skin.NORMAL;
	}

	@Override
	protected final void renderGeometry() {
		Skin.getSkin().getPanelData().getTab().render(0, 0, getWidth(), getRenderState());
	}

	public final void updateNotify() {
		if (!selected)
			label.setColor(HIGHLIGHT_COLOR);
	}
}
