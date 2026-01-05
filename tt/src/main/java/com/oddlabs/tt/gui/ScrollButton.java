package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class ScrollButton extends GUIObject {
	public ScrollButton() {
		setCanFocus(true);
//		setupPos();
	}

	public void setupPos(@NonNull ScrollBar owner) {
		setPos(owner.getButtonX(), owner.getButtonY());
		setDim(Skin.getSkin().getScrollBarData().scrollButton().getWidth(), owner.getButtonHeight());
	}

	@Override
	public void keyPressed(@NonNull KeyboardEvent event) {
	}

	@Override
	public void keyRepeat(@NonNull KeyboardEvent event) {
        switch (event.keyCode()) {
            case TAB -> super.keyRepeat(event);
        }
	}

	@Override
	public void keyReleased(@NonNull KeyboardEvent event) {
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive()
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;

		Skin.getSkin().getScrollBarData().scrollButton()
		        .render(renderer, 0, 0, getHeight(), skinMode);
	}

	@Override
	public void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
	}
}
