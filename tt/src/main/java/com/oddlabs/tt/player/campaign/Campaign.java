package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.CampaignMapForm;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.gui.CampaignIcons;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LoadCampaignBox;
import com.oddlabs.tt.trigger.GameOverDelayTrigger;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public abstract class Campaign {
	private final static ResourceBundle bundle = ResourceBundle.getBundle(Campaign.class.getName());
	private final CampaignState state;
	private CampaignState[] campaign_states; // for saving

	public Campaign(CampaignState state) {
		this.state = state;
	}

	public final CampaignState getState() {
		return state;
	}

	public final void pushDelegate(@NonNull NetworkSelector network, @NonNull GUI gui) {
		final GUIRoot gui_root = gui.newFade();
		gui_root.pushDelegate(new CampaignMapForm(network, gui_root, Campaign.this));
	}

	public void defeated(@NonNull WorldViewer viewer, @NonNull String game_over_message) {
		GUIRoot gui_root = viewer.getGUIRoot();
		new GameOverDelayTrigger(viewer, gui_root.getDelegate().getCamera(), game_over_message);
		doDefeated();
	}

	public final void doDefeated() {
		state.setCurrentIsland(state.getPrevIsland());
	}

	public final void victory(final @NonNull WorldViewer viewer) {
		GUIRoot gui_root = viewer.getGUIRoot();
		new GameOverDelayTrigger(viewer, gui_root.getDelegate().getCamera(), Utils.getBundleString(bundle, "island_complete"));
		LoadCampaignBox.loadSavegames(
				new DeterministicSerializerLoopbackInterface<CampaignState[]>() {
					@Override
					public void loadSucceeded(CampaignState[] campaign_states) {
						Campaign.this.campaign_states = campaign_states;
						doSave(viewer);
					}

					@Override
					public void saveSucceeded() {
					}

					@Override
					public void failed(@NonNull Throwable e) {
						doFailed(e, viewer);
					}
				});
	}

	private void doSave(final @NonNull WorldViewer viewer) {
		for (int i = 0; i < campaign_states.length; i++) {
			if (campaign_states[i].getName().equals(getState().getName())) {
				campaign_states[i] = getState();
			}
		}
		LoadCampaignBox.saveSavegames(campaign_states,
				new DeterministicSerializerLoopbackInterface<CampaignState[]>() {
					@Override
					public void loadSucceeded(CampaignState[] object) {
					}

					@Override
					public void saveSucceeded() {
					}

					@Override
					public void failed(@NonNull Throwable e) {
						doFailed(e, viewer);
					}
				});
	}

	private void doFailed(@NonNull Throwable e, @NonNull WorldViewer viewer) {
		String failed_message = Utils.getBundleString(bundle, "failed_message", LoadCampaignBox.SAVEGAMES_FILE_NAME, e.getMessage());
		viewer.getGUIRoot().addModalForm(new MessageForm(failed_message));
	}

	public abstract CampaignIcons getIcons();
	public abstract void islandChosen(NetworkSelector network, GUIRoot gui_root, int number);
	public abstract CharSequence getCurrentObjective();
	public abstract void startIsland(NetworkSelector network, GUIRoot gui_root, int number);
}
