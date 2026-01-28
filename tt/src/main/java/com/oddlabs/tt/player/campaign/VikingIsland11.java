package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class VikingIsland11 extends Island {
	private static final  ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland11.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}
	
	public VikingIsland11(@NonNull Campaign campaign) {
		super(campaign);
	}

	@Override
	public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
		String[] ai_names = IntStream.range(0,6)
				.mapToObj(i -> i18n( "name" + i))
				.toArray(String[]::new);
		// gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
		GameNetwork game_network = startNewGame(network, gui_root, 256, Landscape.TerrainType.NATIVE, .75f, 1f, .85f, 83493473, 11, VikingCampaign.MAX_UNITS, ai_names);
		game_network.getClient().getServerInterface().setPlayerSlot(0,
				PlayerSlot.HUMAN,
				RacesResources.RACE_VIKINGS,
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
            case CampaignState.DIFFICULTY_EASY -> 10;
            case CampaignState.DIFFICULTY_NORMAL -> 25;
            case CampaignState.DIFFICULTY_HARD -> 40;
            default -> throw new IllegalArgumentException();
        };
        game_network.getClient().getServerInterface().setPlayerSlot(2,
				PlayerSlot.AI,
				RacesResources.RACE_VIKINGS,
				1,
				true,
				PlayerSlot.AI_HARD);
		game_network.getClient().setUnitInfo(2, new UnitInfo(true, false, 0, false, ai_peons, 0, 2, 0));
		game_network.getClient().getServerInterface().startServer();
	}

	@Override
	protected void start() {
		Runnable runnable;
		final Player enemy = getViewer().getWorld().getPlayers()[1];

		// Introduction
		final Runnable dialog8 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header8"),
                            i18n("dialog8"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START);
                    addModalForm(dialog);
                };
		final Runnable dialog7 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header7"),
                            i18n("dialog7"),
                            getCampaign().getIcons().getFaces()[3],
                            Origin.AT_END,
                            dialog8);
                    addModalForm(dialog);
                };
		final Runnable dialog6 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header6"),
                            i18n("dialog6"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog7);
                    addModalForm(dialog);
                };
		final Runnable dialog5 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header5"),
                            i18n("dialog5"),
                            getCampaign().getIcons().getFaces()[6],
                            Origin.AT_END,
                            dialog6);
                    addModalForm(dialog);
                };
		final Runnable dialog4 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header4"),
                            i18n("dialog4"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog5);
                    addModalForm(dialog);
                };
		final Runnable dialog3 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header3"),
                            i18n("dialog3"),
                            getCampaign().getIcons().getFaces()[6],
                            Origin.AT_END,
                            dialog4);
                    addModalForm(dialog);
                };
		final Runnable dialog2 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header2"),
                            i18n("dialog2"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog3);
                    addModalForm(dialog);
                };
		final Runnable dialog1 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                            i18n("dialog1"),
                            getCampaign().getIcons().getFaces()[6],
                            Origin.AT_END,
                            dialog2);
                    addModalForm(dialog);
                };
		runnable = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                            i18n("dialog0"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            dialog1);
                    addModalForm(dialog);
                };
		new GameStartedTrigger(getViewer().getWorld(), runnable);

		// Winner prize
		runnable = () -> {
                    getCampaign().getState().setIslandState(11, CampaignState.ISLAND_COMPLETED);
                    getCampaign().getState().setIslandState(12, CampaignState.ISLAND_AVAILABLE);
                    getCampaign().victory(getViewer());
                };

		// Winning condition
		new VictoryTrigger(getViewer(), runnable);

		// Tower
		insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 47, 22);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 54, 36);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 68, 36);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 80, 36);
		insertGuardTower(enemy, Race.UNIT_WARRIOR_RUBBER, 94, 30);
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
