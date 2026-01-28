package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    protected @NonNull ModeIconQuads getIcon() {
        return icon;
    }

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                :  isHovered() || isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                        : ModeIconQuads.Mode.NORMAL;

		renderer.drawModeIcon(icon, skinMode, 0, 0);
	}
}
