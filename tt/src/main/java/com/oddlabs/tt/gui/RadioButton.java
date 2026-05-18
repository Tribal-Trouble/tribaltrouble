package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class RadioButton extends RadioButtonGroupElement {
    private boolean pressed = false;

    public RadioButton(boolean marked, @NonNull RadioButtonGroup group, @NonNull String text) {
        super(marked, group);
        Label label = new Label(text, Skin.getSkin().getEditFont());
        label.setPos(Skin.getSkin().getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getWidth(),
                (Skin.getSkin().getRadioButtonMarked().get(
                        ModeIconQuads.Mode.NORMAL).getHeight() - label.getHeight()) / 2);
        addChild(label);
        setDim(Skin.getSkin().getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getWidth() + label.getWidth(),
                Skin.getSkin().getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getHeight());
        setCanFocus(true);
    }

    @Override
    protected void mouseReleased(@NonNull MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected void mousePressed(@NonNull MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled() ? ModeIconQuads.Mode.DISABLED : isActive() ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;

        // When unpressed, active, pressed, and hovered, it should show the marked state
        IconQuad quad_to_render = isMarked() ? Skin.getSkin().getRadioButtonMarked().quad(
                skinMode) : skinMode == ModeIconQuads.Mode.ACTIVE && pressed
                        && isHovered() ? Skin.getSkin().getRadioButtonMarked().quad(
                                skinMode) : Skin.getSkin().getRadioButtonUnmarked().quad(skinMode);

        renderer.drawIcon(quad_to_render, 0, 0);
    }
}
