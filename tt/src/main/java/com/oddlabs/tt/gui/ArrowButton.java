package com.oddlabs.tt.gui;

import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public final class ArrowButton extends ButtonObject {
	private final Quad @NonNull [] pressed;
	private final Quad[] unpressed;
	private final Quad[] arrow;

	public ArrowButton(Quad @NonNull [] pressed, Quad[] unpressed, Quad[] arrow) {
		setDim(pressed[Skin.NORMAL].getWidth(), pressed[Skin.NORMAL].getHeight());
		this.pressed = pressed;
		this.unpressed = unpressed;
		this.arrow = arrow;
	}

        @Override
	public void keyPressed(@NonNull KeyboardEvent event) {
		switch (event.getKeyCode()) {
			case Keyboard.KEY_SPACE:
			case Keyboard.KEY_RETURN:
				mousePressedAll(LocalInput.LEFT_BUTTON, 0, 0);
				break;
		}
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
		switch (event.getKeyCode()) {
			case Keyboard.KEY_SPACE:
			case Keyboard.KEY_RETURN:
				mouseReleasedAll(LocalInput.LEFT_BUTTON, 0, 0);
				break;
		}
	}

        @Override
	protected void renderGeometry() {
		if (isDisabled()) {
			unpressed[Skin.DISABLED].render(0, 0);
			arrow[Skin.DISABLED].render(0, 0);
		} else if (isPressed() && isHovered()) {
			pressed[Skin.ACTIVE].render(0, 0);
			arrow[Skin.ACTIVE].render(0, 0);
		} else if (isActive()) {
			unpressed[Skin.ACTIVE].render(0, 0);
			arrow[Skin.ACTIVE].render(0, 0);
		} else {
			unpressed[Skin.NORMAL].render(0, 0);
			arrow[Skin.NORMAL].render(0, 0);
		}
	}

        @Override
	protected void mouseClicked(int button, int x, int y, int clicks) {
		// Steal click from scrollbar
	}
}
