package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
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
    public void handleInput(@NonNull InputEvent event) {
        if (event.consumeAction(GameAction.UI_ACTIVATE)) {
            if (event.getPhase() == InputPhase.PRESSED) {
                mousePressedAll(MouseButton.LEFT, 0, 0);
            } else if (event.getPhase() == InputPhase.RELEASED) {
                mouseReleasedAll(MouseButton.LEFT, 0, 0);
            }
            return;
        }

        if (event.hasAction(GameAction.UI_FOCUS_NEXT)) {
            // Bubble TAB
            return;
        }

        // Swallow everything else
        event.consume();
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled() ? ModeIconQuads.Mode.DISABLED : isPressed()
                && isHovered() ? ModeIconQuads.Mode.ACTIVE : isActive() ? ModeIconQuads.Mode.ACTIVE // Active state for button
                : ModeIconQuads.Mode.NORMAL;

        var quad_to_render_button = (!isDisabled() && isPressed() && isHovered() ? pressed : unpressed);

        renderer.drawModeIcon(quad_to_render_button, skinMode, 0, 0);
        renderer.drawModeIcon(arrow, skinMode, 0, 0);
    }

    @Override
    protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        // Steal click from scrollbar
    }
}
