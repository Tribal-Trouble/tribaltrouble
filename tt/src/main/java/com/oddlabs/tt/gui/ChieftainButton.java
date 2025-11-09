package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public class ChieftainButton extends NonFocusIconButton implements ToolTip {
	private final PlayerInterface player_interface;
	private final WorldViewer viewer;
	private Building current_building;

	public ChieftainButton(WorldViewer viewer, PlayerInterface player_interface, IconQuad @NonNull [] icon_quad, String tool_tip) {
		super(icon_quad, tool_tip);
		this.player_interface = player_interface;
		this.viewer = viewer;
		setCanFocus(true);
		setDim(icon_quad[0].getWidth(), icon_quad[0].getHeight());
	}

	public final void setBuilding(Building current_building) {
		this.current_building = current_building;
	}

	@Override
	protected void mouseClicked(int button, int x, int y, int clicks) {
                player_interface.trainChieftain(current_building, !current_building.getChieftainContainer().isTraining());
	}

	@Override
	protected final void postRender() {
		IconQuad[] watch = Icons.getIcons().getWatch();
		int index = (int)(getProgress()*(watch.length - 1));
		if (!current_building.isDead() && current_building.getChieftainContainer().isTraining())
			watch[index].render(getWidth() - watch[index].getWidth(),  getHeight() - watch[index].getHeight());
	}

	protected final float getProgress() {
		if (!current_building.isDead())
			return current_building.getChieftainContainer().getBuildProgress();
		else
			return 0;
	}
}
