package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;

public class BackgroundLabelBox extends LabelBox {
	public BackgroundLabelBox(CharSequence text, Font font, int width) {
		super(text, font, width);
	}

        @Override
	protected final void renderGeometry() {
		Box background_box = Skin.getSkin().getBackgroundBox();
		background_box.render(0, 1, getWidth(), getHeight() - 2, Skin.NORMAL);
		super.renderGeometry();
	}
}

