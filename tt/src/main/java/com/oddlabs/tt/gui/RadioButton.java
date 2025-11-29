package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class RadioButton extends RadioButtonGroupElement {
	private boolean pressed = false;

	public RadioButton(boolean marked, @NonNull RadioButtonGroup group, @NonNull String text) {
		super(marked, group);
		Label label = new Label(text, Skin.getSkin().getEditFont());
        label.setPos(Skin.getSkin().getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getWidth(), (Skin.getSkin().getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getHeight() - label.getHeight())/2);
		addChild(label);
		setDim(Skin.getSkin().getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getWidth() + label.getWidth(), Skin.getSkin().getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getHeight());
		setCanFocus(true);
	}

	@Override
	protected void mouseReleased (@NonNull MouseButton button, int x, int y) {
		pressed = false;
	}

	@Override
	protected void mousePressed (@NonNull MouseButton button, int x, int y) {
		pressed = true;
	}

	@Override
	protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive()
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;

        // When unpressed, active, pressed, and hovered, it should show the marked state
        IconQuad quad_to_render = isMarked()
                ? Skin.getSkin().getRadioButtonMarked().quad(skinMode)
                : skinMode == ModeIconQuads.Mode.ACTIVE && pressed && isHovered()
                    ? Skin.getSkin().getRadioButtonMarked().quad(skinMode)
                    : Skin.getSkin().getRadioButtonUnmarked().quad(skinMode);

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, quad_to_render.getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		quad_to_render.render(0, 0);
		GL11.glEnd();
	}
}
