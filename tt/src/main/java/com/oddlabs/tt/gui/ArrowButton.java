package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public final class ArrowButton extends ButtonObject {
	private final @NonNull ModeIconQuads pressed;
	private final @NonNull ModeIconQuads unpressed;
	private final @NonNull ModeIconQuads arrow;

	public ArrowButton(@NonNull ModeIconQuads pressed, @NonNull ModeIconQuads unpressed, @NonNull ModeIconQuads arrow) {
		super(Skin.getSkin().getEditFont());
		setDim(pressed.quad(ModeIconQuads.Mode.NORMAL).getWidth(), pressed.quad(ModeIconQuads.Mode.NORMAL).getHeight());
		this.pressed = pressed;
		this.unpressed = unpressed;
		this.arrow = arrow;
	}

	@Override
	public void keyPressed(@NonNull KeyboardEvent event) {
		switch (event.getKeyCode()) {
			case Keyboard.KEY_SPACE:
			case Keyboard.KEY_RETURN:
				mousePressedAll(MouseButton.LEFT, 0, 0);
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
				mouseReleasedAll(MouseButton.LEFT, 0, 0);
				break;
		}
	}

	@Override
	protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isPressed() && isHovered()
                    ? ModeIconQuads.Mode.ACTIVE
                    : isActive()
                        ? ModeIconQuads.Mode.ACTIVE // Active state for button
                        : ModeIconQuads.Mode.NORMAL;

        var quad_to_render_button = (!isDisabled() && isPressed() && isHovered() ? pressed : unpressed);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, quad_to_render_button.quad(skinMode).getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		quad_to_render_button.quad(skinMode).render(0, 0);
		arrow.quad(skinMode).render(0, 0);
		GL11.glEnd();
	}

	@Override
	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
		// Steal click from scrollbar
	}
}
