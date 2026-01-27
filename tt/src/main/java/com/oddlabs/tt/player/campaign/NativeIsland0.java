package com.oddlabs.tt.player.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.camera.FirstPersonCamera;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.camera.JumpCamera;
import com.oddlabs.tt.camera.MapCamera;
import com.oddlabs.tt.delegate.JumpDelegate;
import com.oddlabs.tt.form.CampaignDialogForm;
import com.oddlabs.tt.form.InGameCampaignDialogForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.GameStartedTrigger;
import com.oddlabs.tt.trigger.campaign.NearArmyTrigger;
import com.oddlabs.tt.trigger.campaign.VictoryTrigger;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class NativeIsland0 extends Island {
	private static final  ResourceBundle bundle = ResourceBundle.getBundle(NativeIsland0.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	private int objective = 0;

	public NativeIsland0(Campaign campaign) {
		super(campaign);
	}

	@Override
	public void init(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
		String[] ai_names = IntStream.range(0,6)
				.mapToObj(i -> i18n( "name" + i))
				.toArray(String[]::new);
		GameNetwork game_network = startNewGame(network, gui_root, 1024, Landscape.TerrainType.NATIVE, .75f, .65f, .85f, 25, 0, NativeCampaign.MAX_UNITS, ai_names);
		game_network.getClient().getServerInterface().setPlayerSlot(0,
				PlayerSlot.HUMAN,
				RacesResources.RACE_NATIVES,
				0,
				true,
				PlayerSlot.AI_NONE);
		game_network.getClient().setUnitInfo(0, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
		game_network.getClient().getServerInterface().setPlayerSlot(1,
				PlayerSlot.AI,
				RacesResources.RACE_NATIVES,
				PlayerInfo.TEAM_NEUTRAL,
				true,
				PlayerSlot.AI_NEUTRAL_CAMPAIGN);
		game_network.getClient().setUnitInfo(1, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
		switch (getCampaign().getState().getDifficulty()) {
			case CampaignState.DIFFICULTY_EASY:
				break;
			case CampaignState.DIFFICULTY_NORMAL:
				break;
			case CampaignState.DIFFICULTY_HARD:
				break;
			default:
				throw new RuntimeException();
		}
		game_network.getClient().getServerInterface().setPlayerSlot(2,
				PlayerSlot.AI,
				RacesResources.RACE_VIKINGS,
				1,
				true,
				PlayerSlot.AI_HARD);
		game_network.getClient().setUnitInfo(2, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
		game_network.getClient().getServerInterface().setPlayerSlot(3,
				PlayerSlot.AI,
				RacesResources.RACE_NATIVES,
				0,
				true,
				PlayerSlot.AI_NEUTRAL_CAMPAIGN);
		game_network.getClient().setUnitInfo(3, new UnitInfo(false, false, 0, false, 0, 0, 0, 0));
		game_network.getClient().getServerInterface().startServer();
	}

	@Override
	protected void start() {
		final Player local_player = getViewer().getLocalPlayer();
		final Player reinforcements = getViewer().getWorld().getPlayers()[1];
		final Player enemy = getViewer().getWorld().getPlayers()[2];
		final Player natives = getViewer().getWorld().getPlayers()[3];

		final int chief_start_x = 140*2;
		final int chief_start_y = 117*2;
		final int viking_start_x = 179*2;//236*2;
		final int viking_start_y = 195*2;//362*2;

		// Move start position (for the camera)
		getViewer().getCamera().reset(viking_start_x, viking_start_y);

		// Introduction
		final Runnable dialog1 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header1"),
                            i18n("dialog1"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START);
                    addModalForm(dialog);
                };
		final Runnable camera_jump0 = () -> getViewer().getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), getViewer().getCamera(), chief_start_x + 7, chief_start_y + 7, 200f, 3f, dialog1));
		final Runnable dialog0 = () -> {
                    CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header0"),
                            i18n("dialog0"),
                            getCampaign().getIcons().getFaces()[0],
                            Origin.AT_START,
                            camera_jump0);
                    addModalForm(dialog);
                };
		new GameStartedTrigger(getViewer().getWorld(), dialog0);

		// Insert initial natives
		ResourceBundle player_bundle = ResourceBundle.getBundle(Player.class.getName());
		local_player.setActiveChieftain(new Unit(local_player, chief_start_x, chief_start_y, null, local_player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(player_bundle, "native_chieftain_name"), false));
//		local_player.getChieftain().increaseMagicEnergy(0, 1000);
//		local_player.getChieftain().increaseMagicEnergy(1, 1000);

		natives.buildBuilding(Race.BUILDING_QUARTERS, 135, 128);
		natives.buildBuilding(Race.BUILDING_ARMORY, 143, 124);
		new Unit(natives, 145*2, 127*2, null, natives.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(natives, 149*2, 122*2, null, natives.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(natives, 145*2, 119*2, null, natives.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
		new Unit(natives, 150*2, 125*2, null, natives.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));

		// Insert reinforcements
		int num_reinforcements = switch (getCampaign().getState().getDifficulty()) {
            case CampaignState.DIFFICULTY_EASY -> 15;
            case CampaignState.DIFFICULTY_NORMAL -> 10;
            case CampaignState.DIFFICULTY_HARD -> 6;
            default -> throw new IllegalArgumentException();
        };
        final Unit[] reinforcement_peons = new Unit[num_reinforcements];

		reinforcement_peons[0] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
		reinforcement_peons[1] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
		reinforcement_peons[2] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
		reinforcement_peons[3] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
		reinforcement_peons[4] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
		reinforcement_peons[5] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
		if (getCampaign().getState().getDifficulty() == CampaignState.DIFFICULTY_EASY || getCampaign().getState().getDifficulty() == CampaignState.DIFFICULTY_NORMAL) {
			reinforcement_peons[6] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
			reinforcement_peons[7] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
			reinforcement_peons[8] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
			reinforcement_peons[9] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
		}
		if (getCampaign().getState().getDifficulty() == CampaignState.DIFFICULTY_EASY) {
			reinforcement_peons[10] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
			reinforcement_peons[11] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
			reinforcement_peons[12] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
			reinforcement_peons[13] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
			reinforcement_peons[14] = new Unit(reinforcements, 230*2, 108*2, null, reinforcements.getRace().getUnitTemplate(Race.UNIT_PEON));
		}

		// Insert viking men
		enemy.setActiveChieftain(new Unit(enemy, viking_start_x, viking_start_y, null, enemy.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(player_bundle, "chieftain_name"), false));
		enemy.getChieftain().increaseMagicEnergy(0, 1000);
		enemy.getChieftain().increaseMagicEnergy(1, 1000);

		int num_iron = 45;
		for (int i = 0; i < num_iron; i++) {
			new Unit(enemy, viking_start_x, viking_start_y, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
		}
		int num_rubber = 15;
		for (int i = 0; i < num_rubber; i++) {
			new Unit(enemy, viking_start_x, viking_start_y, null, enemy.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
		}
		// Initiate attack
		Building armory = natives.getArmory();
		attack(enemy, armory, num_iron + num_rubber + 1);

		// Winning condition
		final Runnable dialog7 = () -> {
			CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header7"),
					i18n("dialog7"),
					getCampaign().getIcons().getFaces()[6],
					Origin.AT_END,
					() -> {
						// Winner prize
						getCampaign().getState().setIslandState(0, CampaignState.ISLAND_COMPLETED);
						getCampaign().getState().setIslandState(1, CampaignState.ISLAND_AVAILABLE);
						getCampaign().victory(getViewer());
					});
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
		new VictoryTrigger(getViewer(), () -> {
			CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header5"),
					i18n("dialog5"),
					getCampaign().getIcons().getFaces()[6],
					Origin.AT_END,
					dialog6);
			addModalForm(dialog);
		});

		// Insert treasures
		final SceneryModel[] scenery_models = new SceneryModel[14];
		float dir = (float)Math.sin(Math.PI/4);
		float offset = HeightMap.METERS_PER_UNIT_GRID/2f;
		float shadow_diameter = 4.5f;
		var treasures = getViewer().getWorld().getRacesResources().getTreasures();
		scenery_models[0] = new SceneryModel(getViewer().getWorld(), 163*2 + offset, 126*2 + offset, 0, 1, treasures[0], shadow_diameter, true, i18n("statue"));

		shadow_diameter = 2.6f;
		scenery_models[1] = new SceneryModel(getViewer().getWorld(), 130*2 + offset, 124*2 + offset, -dir, -dir, treasures[3], shadow_diameter, true, i18n("statue"));
		scenery_models[2] = new SceneryModel(getViewer().getWorld(), 152*2 + offset, 138*2 + offset, dir, dir, treasures[1], shadow_diameter, true, i18n("statue"));
		scenery_models[3] = new SceneryModel(getViewer().getWorld(), 152*2 + offset, 144*2 + offset, 0, 1, treasures[3], shadow_diameter, true, i18n("statue"));
		scenery_models[4] = new SceneryModel(getViewer().getWorld(), 140*2 + offset, 140*2 + offset, 0, 1, treasures[4], shadow_diameter, true, i18n("statue"));
		scenery_models[5] = new SceneryModel(getViewer().getWorld(), 143*2 + offset, 116*2 + offset, 0, -1, treasures[1], shadow_diameter, true, i18n("statue"));
		scenery_models[6] = new SceneryModel(getViewer().getWorld(), 142*2 + offset, 131*2 + offset, dir, -dir, treasures[5], shadow_diameter, true, i18n("statue"));

		scenery_models[7] = new SceneryModel(getViewer().getWorld(), 423*2 + offset, 174*2 + offset, 0, 1, treasures[1], shadow_diameter, true, i18n("statue"));
		scenery_models[8] = new SceneryModel(getViewer().getWorld(), 408*2 + offset, 161*2 + offset, -1, 0, treasures[3], shadow_diameter, true, i18n("statue"));
		scenery_models[9] = new SceneryModel(getViewer().getWorld(), 426*2 + offset, 156*2 + offset, dir, -dir, treasures[5], shadow_diameter, true, i18n("statue"));
		scenery_models[10] = new SceneryModel(getViewer().getWorld(), 418*2 + offset, 165*2 + offset, 0, 1, treasures[1], shadow_diameter, true, i18n("statue"));
		scenery_models[11] = new SceneryModel(getViewer().getWorld(), 430*2 + offset, 165*2 + offset, 1, 0, treasures[3], shadow_diameter, true, i18n("statue"));
		scenery_models[12] = new SceneryModel(getViewer().getWorld(), 419*2 + offset, 170*2 + offset, -dir, dir, treasures[4], shadow_diameter, true, i18n("statue"));
		scenery_models[13] = new SceneryModel(getViewer().getWorld(), 416*2 + offset, 156*2 + offset, 0, -1, treasures[5], shadow_diameter, true, i18n("statue"));

		final Runnable dialog4 = () -> {
			CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header4"),
					i18n("dialog4"),
					getCampaign().getIcons().getFaces()[0],
					Origin.AT_START);
			addModalForm(dialog);
			for (Unit reinforcement_peon : reinforcement_peons) {
				if (!reinforcement_peon.isDead()) {
					changeOwner(reinforcement_peon, local_player);
				}
			}
		};
		final Runnable dialog3 = () -> {
			changeObjective(1);
			// Remove statues
			for (SceneryModel scenery_model : scenery_models) {
				scenery_model.remove();
			}
			// Remove Vikings
			Selectable<?>[] viking_units = enemy.getUnits().getSet().toArray(Selectable.newArray(0));
			for (Selectable<?> viking_unit : viking_units) {
				if (viking_unit instanceof Unit unit && !unit.isDead()) {
					unit.removeNow();
				}
			}
			// Insert new Vikings
			int new_viking_start_x = 437*2;
			int new_viking_start_y = 140*2;
			int num_peons = switch (getCampaign().getState().getDifficulty()) {
				case CampaignState.DIFFICULTY_EASY -> 5;
				case CampaignState.DIFFICULTY_NORMAL -> 10;
				case CampaignState.DIFFICULTY_HARD -> 15;
				default -> throw new IllegalArgumentException();
			};
			for (int i = 0; i < num_peons; i++) {
				new Unit(enemy, new_viking_start_x, new_viking_start_y, null, enemy.getRace().getUnitTemplate(Race.UNIT_PEON));
			}
			// Remove natives
			Selectable<?>[] native_selectables = Selectable.newArray(natives.getUnits().getSet().size());
			natives.getUnits().getSet().toArray(native_selectables);
			for (Selectable<?> native_selectable : native_selectables) {
				if (!native_selectable.isDead()) {
					native_selectable.hit(10000, 0, 1, enemy);
				}
			}
			CampaignDialogForm dialog = new InGameCampaignDialogForm(getViewer(), i18n("header3"),
					i18n("dialog3"),
					getCampaign().getIcons().getFaces()[2],
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
		new NearArmyTrigger(reinforcement_peons, 10f, local_player, () -> {
			int x = 230*2;
			int y = 108*2;
			Camera camera = getViewer().getGUIRoot().getDelegate().getCamera();
			if (camera instanceof GameCamera gameCamera) {
				getViewer().getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), gameCamera, x, y, 200f, 3f, dialog2));
			} else if (camera instanceof MapCamera mapCamera) {
				mapCamera.mapGoto(x, y, true);
				dialog2.run();
			} else if (camera instanceof JumpCamera || camera instanceof FirstPersonCamera) {
				getViewer().getGUIRoot().getDelegate().pop();
				getViewer().getGUIRoot().pushDelegate(new JumpDelegate(getViewer(), getViewer().getCamera(), x, y, 200f, 3f, dialog2));
			} else {
				throw new RuntimeException("Camera = " + camera);
			}
		});
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
		return i18n("objective" + objective);
	}

	private void changeObjective(int objective) {
		this.objective = objective;
	}
}
