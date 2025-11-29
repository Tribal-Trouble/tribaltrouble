package com.oddlabs.tt.player.campaign;

import com.oddlabs.tt.delegate.GameStatsDelegate;
import com.oddlabs.tt.delegate.InGameMainMenu;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;

final class CampaignInGameInfo implements InGameInfo {
	private final Campaign campaign;

	public CampaignInGameInfo(Campaign campaign) {
		this.campaign = campaign;
	}

	@Override
	public boolean isRated() {
		return false;
	}

	@Override
	public boolean isMultiplayer() {
		return false;
	}

	@Override
	public float getRandomStartPosition() {
		return 0f;
	}

	@Override
	public void addGUI(WorldViewer viewer, @NonNull InGameMainMenu menu, @NonNull Group game_infos) {
		menu.addAbortButton(Utils.getBundleString(Menu.bundle, "end_game"));
		LabelBox label_objective = new LabelBox(Utils.getBundleString(Menu.bundle, "objective"), Skin.getSkin().getEditFont(), LocalInput.getViewWidth()/2);
		LabelBox label_description = new LabelBox(campaign.getCurrentObjective(), Skin.getSkin().getEditFont(), LocalInput.getViewWidth()/2);
		game_infos.addChild(label_objective);
		game_infos.addChild(label_description);
		label_objective.place();
		label_description.place(label_objective, BOTTOM_LEFT);
		game_infos.compileCanvas();
	}

	@Override
	public void addGameOverGUI(WorldViewer viewer, final @NonNull GameStatsDelegate delegate, int header_y, @NonNull Group group) {
		HorizButton button_ok = new OKButton(150);
		button_ok.addMouseClickListener( (_, _, _, _) -> delegate.startMenu());

		group.addChild(button_ok);
		button_ok.place();
	}

	@Override
	public void close(@NonNull WorldViewer viewer) {
		if (campaign.getState().getIslandState(0) != CampaignState.ISLAND_COMPLETED) {
			Renderer.startMenu(viewer.getNetwork(), viewer.getGUIRoot().getGUI());
		} else {
			campaign.pushDelegate(viewer.getNetwork(), viewer.getGUIRoot().getGUI());
		}

	}

	@Override
	public void abort(@NonNull WorldViewer viewer) {
		viewer.getGUIRoot().pushDelegate(new GameStatsDelegate(viewer, viewer.getGUIRoot().getDelegate().getCamera(), Utils.getBundleString(Menu.bundle, "game_aborted")));
		campaign.doDefeated();
	}
}
