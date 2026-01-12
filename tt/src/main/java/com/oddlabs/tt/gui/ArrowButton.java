package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

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
	public boolean keyPressed(@NonNull KeyboardEvent event) {
        switch (event.keyCode()) {
            case SPACE, RETURN -> {
				mousePressedAll(MouseButton.LEFT, 0, 0);
				return true;
			}
        }
		return true;
	}

	@Override
	public boolean keyRepeat(@NonNull KeyboardEvent event) {
        switch (event.keyCode()) {
            case TAB -> {
				return super.keyRepeat(event);
			}
        }
		return true;
	}

	@Override
	public boolean keyReleased(@NonNull KeyboardEvent event) {
        switch (event.keyCode()) {
            case SPACE, RETURN -> {
				mouseReleasedAll(MouseButton.LEFT, 0, 0);
				return true;
			}
        }
		return true;
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isPressed() && isHovered()
                    ? ModeIconQuads.Mode.ACTIVE
                    : isActive()
                        ? ModeIconQuads.Mode.ACTIVE // Active state for button
                        : ModeIconQuads.Mode.NORMAL;

        var quad_to_render_button = (!isDisabled() && isPressed() && isHovered() ? pressed : unpressed);

		renderer.drawModeIcon(quad_to_render_button, skinMode, 0, 0);
		renderer.drawModeIcon(arrow, skinMode, 0, 0);
	}

	@Override
	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
		// Steal click from scrollbar
	}
}
