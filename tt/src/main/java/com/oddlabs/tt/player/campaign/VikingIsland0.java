package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.AI;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.PlayerEleminatedTrigger;
import com.oddlabs.tt.trigger.campaign.SupplyGatheredTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class VikingIsland0 extends Island {
	private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland0.class.getName());

	private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	public VikingIsland0(Campaign campaign) {
		super(campaign);
	}

	@Override
	public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
		String[] ai_names = IntStream.range(0,6)
				.mapToObj(i -> i18n( "name" + i))
				.toArray(String[]::new);
		// gametype, owner, game, meters_per_world, hills, vegetation_amount, supplies_amount, seed, speed, map_code
		GameNetwork game_network = startNewGame(network, gui_root, 256, Landscape.TerrainType.NATIVE, .5f, 1f, .1f, 45363, 0, VikingCampaign.MAX_UNITS, ai_names);
		game_network.getClient().getServerInterface().setPlayerSlot(0,
				PlayerSlot.HUMAN,
				RacesResources.RACE_VIKINGS,
				0,
				true,
				PlayerSlot.AI_NONE);
		game_network.getClient().setUnitInfo(0,
				new UnitInfo(false, false, 0, false,
					getCampaign().getState().getNumPeons(),
					getCampaign().getState().getNumRockWarriors(),
					getCampaign().getState().getNumIronWarriors(),
					getCampaign().getState().getNumRubberWarriors()));
		game_network.getClient().getServerInterface().setPlayerSlot(1,
				PlayerSlot.AI,
				RacesResources.RACE_VIKINGS,
				PlayerInfo.TEAM_NEUTRAL,
				true,
				PlayerSlot.AI_NEUTRAL_CAMPAIGN);
		game_network.getClient().setUnitInfo(1,
				new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
		game_network.getClient().getServerInterface().setPlayerSlot(2,
				PlayerSlot.AI,
				RacesResources.RACE_NATIVES,
				1,
				true,
				PlayerSlot.AI_PASSIVE_CAMPAIGN);
		game_network.getClient().setUnitInfo(2,
				new UnitInfo(true, true, 0, false, 0, 10, 5, 0));
		game_network.getClient().getServerInterface().startServer();
	}

	@Override
	protected void start() {
		final Player local_player = getViewer().getLocalPlayer();
		final Player chieftain = getViewer().getWorld().getPlayers()[1];
		final Player enemy = getViewer().getWorld().getPlayers()[2];

		// Introduction
		new GameStartedTrigger(getViewer().getWorld(),
                () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                            i18n("dialog0"),
                            getCampaign().getIcons().getFaces()[1],
                            Origin.AT_START);
                    addModalForm(dialog);
                });

		// Disable Chieftain
		getViewer().getLocalPlayer().enableChieftains(false);

		// Winning condition
		new VictoryTrigger(getViewer(),
                () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                            i18n("dialog1"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
							() -> {
								// Winner prize
								getCampaign().getState().setIslandState(0, CampaignState.ISLAND_COMPLETED);
								getCampaign().getState().setIslandState(1, CampaignState.ISLAND_AVAILABLE);
								getCampaign().getState().setIslandState(3, CampaignState.ISLAND_AVAILABLE);
								getCampaign().victory(getViewer());
							});
                    addModalForm(dialog);
                });

		// Place prisoners
		placePrisoners(chieftain, enemy, 0, 0, 0, 0, true);

		// Fill native armory with units and weapons
		final int num_units = 10;
		if (enemy.getArmory().getSupplyContainer(IronAxeWeapon.class).getNumSupplies() < num_units*3)
			enemy.getArmory().getSupplyContainer(IronAxeWeapon.class).increaseSupply(num_units*3);
		if (enemy.getArmory().getUnitContainer().getNumSupplies() < num_units*3)
			enemy.getArmory().getUnitContainer().increaseSupply(num_units*3);

		// Deploy and attack mid-game
		Runnable runnable = () -> {
			Building armory = local_player.getArmory();
			if (armory != null && !armory.isDead()) {
				if (enemy.getArmory() != null && !enemy.getArmory().isDead()) {
					enemy.deployUnits(enemy.getArmory(), DeployType.IRON_WARRIOR, num_units);
					AI.attackLandscape(enemy, armory, num_units);
				}
			}
		};
		if (getCampaign().getState().getDifficulty() == CampaignState.DIFFICULTY_NORMAL) {
			new SupplyGatheredTrigger(getViewer().getLocalPlayer(), runnable, TreeSupply.class, 30);
			new SupplyGatheredTrigger(getViewer().getLocalPlayer(), runnable, RockSupply.class, 30);
			new SupplyGatheredTrigger(getViewer().getLocalPlayer(), runnable, IronSupply.class, 30);
		} else if (getCampaign().getState().getDifficulty() == CampaignState.DIFFICULTY_HARD) {
			new SupplyGatheredTrigger(getViewer().getLocalPlayer(), runnable, TreeSupply.class, 20);
			new SupplyGatheredTrigger(getViewer().getLocalPlayer(), runnable, RockSupply.class, 15);
			new SupplyGatheredTrigger(getViewer().getLocalPlayer(), runnable, IronSupply.class, 15);
		}

		// Defeat if neutrals eliminated
		new PlayerEleminatedTrigger(() -> getCampaign().defeated(getViewer(), i18n("game_over")), chieftain);
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
