package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public final class SliderButton extends ButtonObject {
	private final @NonNull Slider slider;
	private final @NonNull ModeIconQuads button;

	public SliderButton(@NonNull Slider slider, @NonNull ModeIconQuads button) {
		super(Skin.getSkin().getEditFont());
		setDim(button.quad(ModeIconQuads.Mode.NORMAL).getWidth(), button.quad(ModeIconQuads.Mode.NORMAL).getHeight());
		this.slider = slider;
		this.button = button;
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		GUIObject parent = getParent();
        ModeIconQuads.Mode skinMode = parent.isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : (isHovered() || parent.isActive())
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;

		renderer.drawQuad(button.quad(skinMode), 0, 0, Color.WHITE_INT);
	}

	@Override
	public void mouseHeld (@NonNull MouseButton button, int x, int y) {
	}

	@Override
	public void keyPressed(@NonNull KeyboardEvent event) {
		switch (event.getKeyCode()) {
			case Keyboard.KEY_RIGHT:
				slider.setValue(slider.getValue() + 1);
				break;
			case Keyboard.KEY_LEFT:
				slider.setValue(slider.getValue() - 1);
				break;
		}
	}
}
