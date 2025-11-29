package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ChieftainButton extends NonFocusIconButton implements ToolTip {
	private final @NonNull PlayerInterface player_interface;
    private @Nullable Building current_building;

	public ChieftainButton(@NonNull WorldViewer viewer, @NonNull PlayerInterface player_interface, @NonNull ModeIconQuads icon, @NonNull String tool_tip) {
		super(icon, tool_tip);
		this.player_interface = player_interface;
        setCanFocus(true);
	}

	public final void setBuilding(@NonNull Building current_building) {
		this.current_building = current_building;
	}

	@Override
	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
        player_interface.trainChieftain(current_building, !current_building.getChieftainContainer().isTraining());
	}

	@Override
	protected final void postRender(@NonNull GUIRenderer renderer) {
		IconQuad[] watch = GUIIcons.getIcons().getWatch();
		int index = (int)(getProgress()*(watch.length - 1));
		if (!current_building.isDead() && current_building.getChieftainContainer().isTraining()) {
			IconQuad watchQuad = watch[index];
			renderer.drawQuad(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight(), Color.WHITE_INT);
		}
	}

	protected final float getProgress() {
        return !current_building.isDead() ? current_building.getChieftainContainer().getBuildProgress() : 0;
	}
}
