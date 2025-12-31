package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.render.GUIRenderer;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public class PanelTab extends GUIObject {
	private static final Vector4fc HIGHLIGHT_COLOR = com.oddlabs.util.Color.argb4v(0xFF_00_FF_00);
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

	public final ModeIconQuads.@NonNull Mode getRenderState() {
        return isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive() || selected
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;
	}

	@Override
	protected final void renderGeometry(@NonNull GUIRenderer renderer) {
        Skin.getSkin().getPanelData().getTab()
                .render(renderer, 0, 0, getWidth(), getRenderState());
	}

	public final void updateNotify() {
		if (!selected)
			label.setColor(HIGHLIGHT_COLOR);
	}
}
