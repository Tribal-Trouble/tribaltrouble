package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.PlayerEleminatedTrigger;
import com.oddlabs.tt.trigger.campaign.TimeTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class VikingIsland1 extends Island {
	private static final ResourceBundle bundle = ResourceBundle.getBundle(VikingIsland1.class.getName());
	
	public VikingIsland1(@NonNull Campaign campaign) {
		super(campaign);
	}

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}
	@Override
	public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
		String[] ai_names = IntStream.range(0,6)
				.mapToObj(i -> i18n( "name" + i))
				.toArray(String[]::new);
		GameNetwork game_network = startNewGame(network, gui_root, 256, Landscape.TerrainType.NATIVE, .75f, 1f, .5f, 97455, 1, VikingCampaign.MAX_UNITS, ai_names);
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
		game_network.getClient().getServerInterface().setPlayerSlot(1,
				PlayerSlot.AI,
				RacesResources.RACE_VIKINGS,
				PlayerInfo.TEAM_NEUTRAL,
				true,
				PlayerSlot.AI_NEUTRAL_CAMPAIGN);
		game_network.getClient().setUnitInfo(1, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
		game_network.getClient().getServerInterface().setPlayerSlot(2,
				PlayerSlot.AI,
				RacesResources.RACE_NATIVES,
				1,
				true,
				PlayerSlot.AI_PASSIVE_CAMPAIGN);
		game_network.getClient().setUnitInfo(2, new UnitInfo(true, true, 0, false, 0, 10, 5, 0));
		game_network.getClient().getServerInterface().startServer();
	}

	@Override
	protected void start() {
		final Player local_player = getViewer().getLocalPlayer();
		final Player captives = getViewer().getWorld().getPlayers()[1];
		final Player enemy = getViewer().getWorld().getPlayers()[2];

		// Introduction
		new GameStartedTrigger(getViewer().getWorld(), () -> {
			CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
					i18n("dialog0"),
					getCampaign().getIcons().getFaces()[0],
					Origin.AT_START);
			addModalForm(dialog);
		});

		// Winning condition
		new VictoryTrigger(getViewer(), () -> {
			CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
					i18n("dialog1"),
					getCampaign().getIcons().getFaces()[3],
					Origin.AT_END,
					() -> {
						// Winner prize
						getCampaign().getState().setIslandState(1, CampaignState.ISLAND_COMPLETED);
						getCampaign().getState().setIslandState(2, CampaignState.ISLAND_AVAILABLE);
						getCampaign().getState().setNumPeons(getCampaign().getState().getNumPeons() + captives.getUnitCountContainer().getNumSupplies());
						getCampaign().victory(getViewer());
					});
			addModalForm(dialog);
		});

		// Place prisoners
		placePrisoners(captives, enemy, 10, 0, 0, 0, false);

		final int attack1 = 5;
		final int attack2 = 10;
		final int defense = 10;

		// Attack1
		Runnable attack1_runnable = () -> {
                    Building armory = local_player.getArmory();
                    Unit chieftain = local_player.getChieftain();
                    if (armory != null && !armory.isDead()) {
                        attack(enemy, armory, attack1);
                    } else if (chieftain != null && !chieftain.isDead()) {
                        attack(enemy, chieftain, attack1);
                    }
                    refillArmory(enemy);
                    deploy(enemy, attack2);
                };

		// Attack2
		Runnable attack2_runnable = () -> {
                    Building armory = local_player.getArmory();
                    Unit chieftain = local_player.getChieftain();
                    if (armory != null && !armory.isDead()) {
                        attack(enemy, armory, attack2);
                    } else if (chieftain != null && !chieftain.isDead()) {
                        attack(enemy, chieftain, attack1);
                    }
                    refillArmory(enemy);
                    deploy(enemy, defense);
                };

		switch (getCampaign().getState().getDifficulty()) {
			case CampaignState.DIFFICULTY_EASY:
				new TimeTrigger(getViewer().getWorld(), 8f*60f, attack1_runnable);
				new TimeTrigger(getViewer().getWorld(), 25f*60f, attack2_runnable);
				break;
			case CampaignState.DIFFICULTY_NORMAL:
				new TimeTrigger(getViewer().getWorld(), 5f*60f, attack1_runnable);
				new TimeTrigger(getViewer().getWorld(), 8.5f*60f, attack2_runnable);
				break;
			case CampaignState.DIFFICULTY_HARD:
				new TimeTrigger(getViewer().getWorld(), 4f*60f, attack1_runnable);
				new TimeTrigger(getViewer().getWorld(), 7f*60f, attack2_runnable);
				break;
			default:
				throw new IllegalArgumentException("unexpected difficulty");
		}

		// Defeat if neutrals eliminated
		new PlayerEleminatedTrigger(() -> getCampaign().defeated(getViewer(), i18n("game_over")), captives);
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
