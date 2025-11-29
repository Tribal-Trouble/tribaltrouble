package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

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
	protected void renderGeometry() {
		GUIObject parent = getParent();
        ModeIconQuads.Mode skinMode = parent.isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : (isHovered() || parent.isActive())
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;

		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, button.quad(skinMode).getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		button.quad(skinMode).render(0, 0);
		GL11.glEnd();
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
