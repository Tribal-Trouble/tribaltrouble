package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public class BackgroundLabelBox extends LabelBox {
	public BackgroundLabelBox(@NonNull CharSequence text, @NonNull Font font, int width) {
		super(text, font, width);
	}

	@Override
	protected final void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		Box background_box = Skin.getSkin().getBackgroundBox();
		background_box.render(0f, 1f, getWidth(), getHeight() - 2, ModeIconQuads.Mode.NORMAL);
		super.renderGeometry(clip_left, clip_right, clip_bottom, clip_top);
	}
}
