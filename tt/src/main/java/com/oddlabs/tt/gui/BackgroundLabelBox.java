package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public class BackgroundLabelBox extends LabelBox {
	public BackgroundLabelBox(@NonNull CharSequence text, @NonNull Font font, int width) {
		super(text, font, width);
	}

	@Override
	protected final void renderGeometry(@NonNull GUIRenderer renderer) {
		Box background_box = Skin.getSkin().getBackgroundBox();
		background_box.render(renderer, 0f, 1f, getWidth(), getHeight() - 2, ModeIconQuads.Mode.NORMAL);
		super.renderGeometry(renderer);
	}
}
