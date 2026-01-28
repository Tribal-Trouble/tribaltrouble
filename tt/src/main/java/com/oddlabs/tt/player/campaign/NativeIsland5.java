package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class NativeIsland5 extends Island {
	private static final  ResourceBundle bundle = ResourceBundle.getBundle(NativeIsland5.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	public NativeIsland5(@NonNull Campaign campaign) {
		super(campaign);
	}

	@Override
	public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
		String[] ai_names = IntStream.range(0,6)
				.mapToObj(i -> i18n( "name" + i))
				.toArray(String[]::new);
		// gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
		GameNetwork game_network = startNewGame(network, gui_root, 512, Landscape.TerrainType.VIKING, 1f, 1f, 1f, 4, 5, NativeCampaign.MAX_UNITS, ai_names);
		game_network.getClient().getServerInterface().setPlayerSlot(0,
				PlayerSlot.HUMAN,
				RacesResources.RACE_NATIVES,
				0,
				true,
				PlayerSlot.AI_NONE);
		game_network.getClient().setUnitInfo(0,
				new UnitInfo(false, false, 0, true,
					getCampaign().getState().getNumPeons(),
					getCampaign().getState().getNumRockWarriors(),
					getCampaign().getState().getNumIronWarriors(),
					getCampaign().getState().getNumRubberWarriors()));

		int ai_peons = switch (getCampaign().getState().getDifficulty()) {
            case CampaignState.DIFFICULTY_EASY -> 5;
            case CampaignState.DIFFICULTY_NORMAL -> 10;
            case CampaignState.DIFFICULTY_HARD -> 25;
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
				PlayerSlot.AI,
				RacesResources.RACE_VIKINGS,
				1,
				true,
				PlayerSlot.AI_HARD);
		game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 0, false, ai_peons, 0, 0, 0));
		game_network.getClient().getServerInterface().startServer();
	}

	@Override
	protected void start() {
		Runnable runnable;
		// Introduction
		final Runnable dialog0 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                            i18n("dialog0"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START);
                    addModalForm(dialog);
                };
		new GameStartedTrigger(getViewer().getWorld(), dialog0);

		// Winner prize
		final Runnable prize = () -> {
                    getCampaign().getState().setIslandState(5, CampaignState.ISLAND_COMPLETED);
                    getCampaign().getState().setIslandState(3, CampaignState.ISLAND_AVAILABLE);
                    getCampaign().getState().setIslandState(6, CampaignState.ISLAND_AVAILABLE);
                    getCampaign().victory(getViewer());
                };

		// Winning condition
		runnable = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                            i18n("dialog1"),
                            getCampaign().getIcons().getFaces()[8],
                            Origin.AT_END,
                            prize);
                    addModalForm(dialog);
                };
		new VictoryTrigger(getViewer(), runnable);
	}

	@Override
	public @NonNull CharSequence getHeader() {
		return i18n("header");
	}

	@Override
	public @NonNull CharSequence getDescription() {
		return i18n("description");
	}

	@Override
	public @NonNull CharSequence getCurrentObjective() {
		return i18n("objective");
	}
}
