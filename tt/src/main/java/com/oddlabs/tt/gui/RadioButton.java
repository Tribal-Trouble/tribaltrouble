package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public final class RadioButton extends RadioButtonGroupElement {
	private boolean pressed = false;

	public RadioButton(boolean marked, @NonNull RadioButtonGroup group, @NonNull String text) {
		super(marked, group);
		Label label = new Label(text, Skin.getSkin().getEditFont());
		addChild(label);
		label.setPos(Skin.getSkin().getRadioButtonMarked()[Skin.NORMAL].getWidth(), (Skin.getSkin().getRadioButtonMarked()[Skin.NORMAL].getHeight() - label.getHeight())/2);
		setDim(Skin.getSkin().getRadioButtonMarked()[Skin.NORMAL].getWidth() + label.getWidth(), Skin.getSkin().getRadioButtonMarked()[Skin.NORMAL].getHeight());
		setCanFocus(true);
	}

	@Override
	protected void mouseReleased(MouseButton button, int x, int y) {
		pressed = false;
	}

	@Override
	protected void mousePressed(MouseButton button, int x, int y) {
		pressed = true;
	}

	@Override
	protected void renderGeometry() {
		if (isDisabled()) {
			if (isMarked())
				Skin.getSkin().getRadioButtonMarked()[Skin.DISABLED].render(0, 0);
			else
				Skin.getSkin().getRadioButtonUnmarked()[Skin.DISABLED].render(0, 0);
		} else if (isActive()) {
			if (isMarked()) {
				Skin.getSkin().getRadioButtonMarked()[Skin.ACTIVE].render(0, 0);
			} else {
				if (pressed && isHovered())
					Skin.getSkin().getRadioButtonMarked()[Skin.ACTIVE].render(0, 0);
				else
					Skin.getSkin().getRadioButtonUnmarked()[Skin.ACTIVE].render(0, 0);
			}
		} else {
			if (isMarked())
				Skin.getSkin().getRadioButtonMarked()[Skin.NORMAL].render(0, 0);
			else
				Skin.getSkin().getRadioButtonUnmarked()[Skin.NORMAL].render(0, 0);
		}
	}
}
