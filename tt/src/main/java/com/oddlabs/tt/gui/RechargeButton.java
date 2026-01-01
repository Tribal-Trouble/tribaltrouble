package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.ToolTip;
import org.jspecify.annotations.NonNull;

public class RechargeButton extends NonFocusIconButton implements ToolTip {
	private final @NonNull PlayerInterface player_interface;
	private final int magic_index;
	private Unit unit;

	public RechargeButton(@NonNull PlayerInterface player_interface, @NonNull ModeIconQuads icon, @NonNull String tool_tip, int magic_index) {
		super(icon, tool_tip);
		this.player_interface = player_interface;
		this.magic_index = magic_index;
		setCanFocus(true);
        var normal = icon.quad(ModeIconQuads.Mode.NORMAL);
		setDim(normal.getWidth(), normal.getHeight());
	}

	public final void setUnit(Unit unit) {
		this.unit = unit;
	}

	@Override
	public final void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
		if (unit.canDoMagic(magic_index))
			player_interface.doMagic(unit, magic_index);
	}

	@Override
	protected final void postRender(@NonNull GUIRenderer renderer) {
		IconQuad[] watch = GUIIcons.getIcons().getWatch();
		float progress = unit.getMagicProgress(magic_index);
		int index = (int)(progress*(watch.length - 1));
		if (!unit.isDead() && progress < 1f) {
			IconQuad watchQuad = watch[index];
			renderer.drawIcon(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight());
		}
	}
}
