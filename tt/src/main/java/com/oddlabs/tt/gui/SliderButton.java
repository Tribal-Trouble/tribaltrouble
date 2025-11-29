package com.oddlabs.tt.gui;

import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public final class SliderButton extends ButtonObject {
	private final Slider slider;
	private final Quad @NonNull [] button;

	public SliderButton(Slider slider, Quad @NonNull [] button) {
		setDim(button[Skin.NORMAL].getWidth(), button[Skin.NORMAL].getHeight());
		this.slider = slider;
		this.button = button;
	}

	@Override
	protected void renderGeometry() {
		GUIObject parent = (GUIObject)getParent();
		if (parent.isDisabled()) {
			button[Skin.DISABLED].render(0, 0);
		} else if (isHovered() || parent.isActive()) {
			button[Skin.ACTIVE].render(0, 0);
		} else {
			button[Skin.NORMAL].render(0, 0);
		}
	}

	@Override
	public void mouseHeld(MouseButton button, int x, int y) {
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
