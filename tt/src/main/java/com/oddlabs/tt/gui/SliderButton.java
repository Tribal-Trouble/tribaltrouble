package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

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
        ModeIconQuads.Mode skinMode = parent.isDisabled() ? ModeIconQuads.Mode.DISABLED : (isHovered()
                || parent.isActive()) ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;

        renderer.drawModeIcon(button, skinMode, 0, 0);
    }

    @Override
    public void mouseHeld(@NonNull MouseButton button, int x, int y) {
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.UI_NAV_RIGHT)) {
                slider.setValue(slider.getValue() + 1);
                return;
            }
            if (event.consumeAction(GameAction.UI_NAV_LEFT)) {
                slider.setValue(slider.getValue() - 1);
                return;
            }
        }

        // Swallow others (legacy behavior)
        event.consume();
    }
}
