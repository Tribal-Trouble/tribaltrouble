package com.oddlabs.tt.gui;

public abstract class ButtonObject extends GUIObject {
	private boolean pressed = false;

	public ButtonObject() {
		setCanFocus(true);
	}

	protected final boolean isPressed() {
		return pressed;
	}

	@Override
	protected final void mouseReleased(int button, int x, int y) {
		pressed = false;
	}

	@Override
	protected final void mousePressed(int button, int x, int y) {
		pressed = true;
	}

	@Override
	protected void mouseHeld(int button, int x, int y) {
		if (pressed)
			mousePressedAll(button, x, y);
	}

	@Override
	public void setDisabled(boolean disabled) {
		if (disabled)
			pressed = false;
		super.setDisabled(disabled);
	}
}
