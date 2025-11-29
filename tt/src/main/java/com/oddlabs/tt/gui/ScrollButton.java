package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public final class ScrollButton extends GUIObject {
	public ScrollButton() {
		setCanFocus(true);
//		setupPos();
	}

	public void setupPos(@NonNull ScrollBar owner) {
		setPos(owner.getButtonX(), owner.getButtonY());
		setDim(Skin.getSkin().getScrollBarData().getScrollButton().getWidth(), owner.getButtonHeight());
	}

	@Override
	public void keyPressed(@NonNull KeyboardEvent event) {
	}

	@Override
	public void keyRepeat(@NonNull KeyboardEvent event) {
		switch (event.getKeyCode()) {
			case Keyboard.KEY_TAB:
				super.keyRepeat(event);
				break;
		}
	}

	@Override
	public void keyReleased(@NonNull KeyboardEvent event) {
	}

	@Override
	protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive()
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;
        Skin.getSkin().getScrollBarData().getScrollButton()
		        .render(0, 0, getHeight(), skinMode);
	}

	@Override
	public void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
	}
}
