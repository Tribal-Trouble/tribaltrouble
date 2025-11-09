package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public class HorizButton extends ButtonObject {
	public HorizButton(@NonNull String caption, int width) {
		setDim(width, Skin.getSkin().getHorizButtonPressed().getHeight());
		Font font = Skin.getSkin().getButtonFont();
		Label label = new Label(caption, font);
		label.setPos((width - label.getWidth())/2, (Skin.getSkin().getHorizButtonPressed().getHeight() - font.getHeight())/2);
		addChild(label);
	}

	@Override
	protected final void renderGeometry() {
		if (isDisabled())
			Skin.getSkin().getHorizButtonUnpressed().render(0, 0, getWidth(), Skin.DISABLED);
		else if (isPressed() && isHovered())
			Skin.getSkin().getHorizButtonPressed().render(0, 0, getWidth(), Skin.ACTIVE);
		else if (isActive())
			Skin.getSkin().getHorizButtonUnpressed().render(0, 0, getWidth(), Skin.ACTIVE);
		else
			Skin.getSkin().getHorizButtonUnpressed().render(0, 0, getWidth(), Skin.NORMAL);
	}
}
