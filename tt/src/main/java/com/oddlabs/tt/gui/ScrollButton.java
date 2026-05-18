package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class ScrollButton extends GUIObject {
    public ScrollButton() {
        setCanFocus(true);
//		setupPos();
    }

    public void setupPos(@NonNull ScrollBar owner) {
        setPos(owner.getButtonX(), owner.getButtonY());
        setDim(Skin.getSkin().getScrollBarData().scrollButton().getWidth(), owner.getButtonHeight());
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.hasAction(GameAction.UI_FOCUS_NEXT)) {
            // Bubble Tab
            return;
        }
        // Swallow others
        event.consume();
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled() ? ModeIconQuads.Mode.DISABLED : isActive() ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;

        Skin.getSkin().getScrollBarData().scrollButton().render(renderer, 0, 0, getHeight(), skinMode);
    }

    @Override
    public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
    }
}
