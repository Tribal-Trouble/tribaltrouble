package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public class HorizButton extends ButtonObject {
    public HorizButton(@NonNull String caption, int width) {
        super(Skin.getSkin().getButtonFont());
        setDim(width, Skin.getSkin().getHorizButtonPressed().getHeight());
        Label label = new Label(caption, getFont());
        label.setPos((width - label.getWidth()) / 2,
                (Skin.getSkin().getHorizButtonPressed().getHeight() - getFont().getHeight()) / 2);
        addChild(label);
    }

    @Override
    protected final void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled() ? ModeIconQuads.Mode.DISABLED : isPressed()
                && isHovered() ? ModeIconQuads.Mode.ACTIVE : isActive() ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;

        var horizButton = skinMode == ModeIconQuads.Mode.ACTIVE && isPressed()
                && isHovered() ? Skin.getSkin().getHorizButtonPressed() : Skin.getSkin().getHorizButtonUnpressed();

        horizButton.render(renderer, 0, 0, getWidth(), skinMode);
    }
}
