package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public class IconButton extends ButtonObject {
	private final @NonNull ModeIconQuads icon;
	private @Nullable IconDisabler icon_disabler = null;

	public IconButton(@NonNull ModeIconQuads icon) {
		super(Skin.getSkin().getEditFont());
        this.icon = icon;
        var normal = icon.quad(ModeIconQuads.Mode.NORMAL);
		setDim(normal.getWidth(), normal.getHeight());
	}

	public final void setIconDisabler(@Nullable IconDisabler icon_disabler) {
		this.icon_disabler = icon_disabler;
	}

	public final void doUpdate() {
		setDisabled(icon_disabler != null && icon_disabler.isDisabled());
	}

	@Override
	protected final void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                :  isHovered() || isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                        : ModeIconQuads.Mode.NORMAL;

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, icon.quad(skinMode).getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		icon.quad(skinMode).render(0, 0);
		GL11.glEnd();
	}
}
