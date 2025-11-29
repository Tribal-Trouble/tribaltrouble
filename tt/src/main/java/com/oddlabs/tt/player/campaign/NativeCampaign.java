package com.oddlabs.tt.player.campaign;


import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.gui.CampaignIcons;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.NativeCampaignIcons;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public final class NativeCampaign extends Campaign {
	public static final int MAX_UNITS = 41;
	private static final int[] INITIAL_STATES = new int[]{
/*
		CampaignState.ISLAND_AVAILABLE,
		CampaignState.ISLAND_AVAILABLE,
		CampaignState.ISLAND_AVAILABLE,
		CampaignState.ISLAND_AVAILABLE,
		CampaignState.ISLAND_AVAILABLE,
		CampaignState.ISLAND_AVAILABLE,
		CampaignState.ISLAND_AVAILABLE,
		CampaignState.ISLAND_AVAILABLE};
*/
		CampaignState.ISLAND_AVAILABLE,
		CampaignState.ISLAND_UNAVAILABLE,
		CampaignState.ISLAND_UNAVAILABLE,
		CampaignState.ISLAND_UNAVAILABLE,
		CampaignState.ISLAND_UNAVAILABLE,
		CampaignState.ISLAND_UNAVAILABLE,
		CampaignState.ISLAND_UNAVAILABLE,
		CampaignState.ISLAND_HIDDEN};

	private final Island @NonNull [] islands;

	public NativeCampaign(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
		this(network, gui_root, new CampaignState(INITIAL_STATES));
	}

	public NativeCampaign(NetworkSelector network, GUIRoot gui_root, CampaignState campaign_state) {
		super(campaign_state);
		islands = new Island[NativeCampaignIcons.getIcons().getNumIslands()];
		islands[0] = new NativeIsland0(this);
		islands[1] = new NativeIsland1(this);
		islands[2] = new NativeIsland2(this);
		islands[3] = new NativeIsland3(this);
		islands[4] = new NativeIsland4(this);
		islands[5] = new NativeIsland5(this);
		islands[6] = new NativeIsland6(this);
		islands[7] = new NativeIsland7(this);
		if (getState().getCurrentIsland() == -1) {
			startIsland(network, gui_root, 0);
		}
		getState().setHasMagic0(true);
		getState().setHasRubberWeapons(true);
	}

	@Override
	public @NonNull CampaignIcons getIcons() {
		return NativeCampaignIcons.getIcons();
	}

	@Override
	public void islandChosen(NetworkSelector network, @NonNull GUIRoot gui_root, int number) {
		if (Renderer.isRegistered()) {
			Form dialog = new CampaignDialogForm(islands[number].getHeader(),
					islands[number].getDescription(),
					null,
					Origin.AT_START,
					new IslandListener(network, gui_root, number), true);
			gui_root.addModalForm(dialog);
		}
	}

	@Override
	public CharSequence getCurrentObjective() {
		if (getState().getCurrentIsland() != -1) {
			return islands[getState().getCurrentIsland()].getCurrentObjective();
		}
		throw new RuntimeException();
	}

	@Override
	public void defeated(@NonNull WorldViewer viewer, String game_over_message) {
		if (getState().getCurrentIsland() == 4)
			((NativeIsland4)islands[4]).removeCounter();
		super.defeated(viewer, game_over_message);
	}

	@Override
	public void startIsland(NetworkSelector network, GUIRoot gui_root, int number) {
		getState().setCurrentIsland(number);
		islands[number].chosen(network, gui_root);
	}

	private final class IslandListener implements Runnable {
		private final int number;
		private final GUIRoot gui_root;
		private final NetworkSelector network;

		public IslandListener(NetworkSelector network, GUIRoot gui_root, int number) {
			this.number = number;
			this.gui_root = gui_root;
			this.network = network;
		}

		@Override
		public void run() {
			startIsland(network, gui_root, number);
		}
	}
}
