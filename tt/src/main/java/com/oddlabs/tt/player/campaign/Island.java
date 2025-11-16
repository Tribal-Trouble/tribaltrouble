package com.oddlabs.tt.player.campaign;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.UnitTemplate;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.WorldInitAction;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.AI;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.trigger.campaign.DefeatTrigger;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class Island {
	private static final float CAMPAIGN_DIFFICULTY_BONUS = .75f;

	private final Campaign campaign;

	private WorldViewer world_viewer;

	public Island(Campaign campaign) {
		this.campaign = campaign;
	}

	protected final Campaign getCampaign() {
		return campaign;
	}

	public final void chosen(NetworkSelector network, GUIRoot gui_root) {
		init(network, gui_root);
	}

	protected final void addModalForm(@NonNull Form form) {
		world_viewer.getGUIRoot().addModalForm(form);
	}

	protected final @NonNull GameNetwork startNewGame(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, int meters_per_world, Landscape.@NonNull TerrainType terrain, float hills, float vegetation_amount, float supplies_amount, int seed, int campaign_num, int initial_units, String[] ai_names) {
		InGameInfo ingame_info = new CampaignInGameInfo(campaign);
		WorldInitAction init_action = (WorldViewer viewer) -> {
                    world_viewer = viewer;
                    Menu.completeGameSetupHack(world_viewer);
                    if (!campaign.getState().hasRubberWeapons()) {
                        viewer.getLocalPlayer().enableRubber(false);
                    }
                    if (!campaign.getState().hasMagic0()) {
                        viewer.getLocalPlayer().enableMagic(0, false);
                    }
                    if (!campaign.getState().hasMagic1()) {
                        viewer.getLocalPlayer().enableMagic(1, false);
                    }
                    Player[] players = viewer.getWorld().getPlayers();
                    switch (campaign.getState().getDifficulty()) {
                        case CampaignState.DIFFICULTY_EASY:
                            for (Player player : players) {
                                if (player.isEnemy(viewer.getLocalPlayer())) {
                                    viewer.getLocalPlayer().setHitBonus(CAMPAIGN_DIFFICULTY_BONUS);
                                }
                            }
                            break;
                        case CampaignState.DIFFICULTY_NORMAL:
                            break;
                        case CampaignState.DIFFICULTY_HARD:
                            players = viewer.getWorld().getPlayers();
                            for (Player player : players) {
                                if (player.isEnemy(viewer.getLocalPlayer())) {
                                    player.setHitBonus(CAMPAIGN_DIFFICULTY_BONUS);
                                }
                            }
                            break;
                        default:
                            throw new RuntimeException();
                    }
                    start();
                    new DefeatTrigger(world_viewer, campaign, viewer.getLocalPlayer().getChieftain());
                };
		return Menu.startNewGame(network, gui_root, null, new WorldParameters(Game.GAMESPEED_NORMAL,
					"Campaign" + campaign_num, initial_units,
					Player.DEFAULT_MAX_UNIT_COUNT),
					ingame_info,
					init_action,
					null, meters_per_world, terrain, hills, vegetation_amount, supplies_amount, seed, ai_names);
	}

	protected final WorldViewer getViewer() {
		return world_viewer;
	}

	protected abstract void init(NetworkSelector network, GUIRoot gui_root);
	protected abstract void start();
	protected abstract CharSequence getHeader();
	protected abstract CharSequence getDescription();
	protected abstract CharSequence getCurrentObjective();

	protected final @Nullable Unit changeOwner(@NonNull Unit unit, @NonNull Player owner) {
		float x = unit.getPositionX();
		float y = unit.getPositionY();
		UnitTemplate template = (UnitTemplate)unit.getTemplate();
		unit.removeNow();
		if (!owner.getUnitCountContainer().isSupplyFull()) {
			Unit new_unit = new Unit(owner, x, y, null, template);
			world_viewer.getPicker().getRespondManager().addResponder(new_unit);
			return new_unit;
		} else
			return null;
	}

	protected final void insertGuardTower(@NonNull Player owner, int warrior_type, int grid_x, int grid_y) {
		Building tower = owner.buildBuilding(Race.BUILDING_TOWER, grid_x, grid_y);
		Unit unit = new Unit(owner,
				UnitGrid.coordinateFromGrid(grid_x),
				UnitGrid.coordinateFromGrid(grid_y),
				null,
				owner.getRace().getUnitTemplate(warrior_type));
		unit.setTarget(tower, Target.ACTION_DEFAULT, false);
	}

	protected final void placePrisoners(@NonNull Player captive, @NonNull Player enemy, int peons, int rock_warriors, int iron_warriors, int rubber_warriors, boolean chieftain) {
		int ox = UnitGrid.toGridCoordinate(enemy.getStartX());
		int oy = UnitGrid.toGridCoordinate(enemy.getStartY());
		int center = captive.getWorld().getHeightMap().getGridUnitsPerWorld()/2;
		int dx = center - ox;
		int dy = center - oy;
		float inv_dist = 1f/(float)Math.sqrt(dx*dx + dy*dy);
		int tx = (int)(ox - 5f*dx*inv_dist);
		int ty = (int)(oy - 5f*dy*inv_dist);
		for (int i = 0; i < peons; i++) {
			new Unit(captive, UnitGrid.coordinateFromGrid(tx), UnitGrid.coordinateFromGrid(ty),
					null, captive.getRace().getUnitTemplate(Race.UNIT_PEON));
		}
		for (int i = 0; i < rock_warriors; i++) {
			new Unit(captive, UnitGrid.coordinateFromGrid(tx), UnitGrid.coordinateFromGrid(ty),
					null, captive.getRace().getUnitTemplate(Race.UNIT_PEON));
		}
		for (int i = 0; i < iron_warriors; i++) {
			new Unit(captive, UnitGrid.coordinateFromGrid(tx), UnitGrid.coordinateFromGrid(ty),
					null, captive.getRace().getUnitTemplate(Race.UNIT_PEON));
		}
		for (int i = 0; i < rubber_warriors; i++) {
			new Unit(captive, UnitGrid.coordinateFromGrid(tx), UnitGrid.coordinateFromGrid(ty),
					null, captive.getRace().getUnitTemplate(Race.UNIT_PEON));
		}
		if (chieftain) {
			captive.setActiveChieftain(new Unit(captive, UnitGrid.coordinateFromGrid(tx), UnitGrid.coordinateFromGrid(ty),
					null, captive.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN)));
		}
	}

	protected final void deploy(@NonNull Player enemy, int num_units) {
		if (enemy.getArmory() != null && !enemy.getArmory().isDead()) {
			enemy.deployUnits(enemy.getArmory(), DeployType.IRON_WARRIOR, num_units);
		}
	}

	protected final void attack(@NonNull Player enemy, @NonNull Target target, int num_units) {
		//int ordered =
		AI.attackLandscape(enemy, target, num_units);
	}

	protected final @Nullable Unit getWarrior(@NonNull Player player) {
		return AI.getWarrior(player);
	}

	protected final void refillArmory(@NonNull Player enemy) {
		if (enemy.getQuarters() == null || enemy.getArmory() == null)
			return;

		enemy.getQuarters().removeSupplies(Unit.class);
		enemy.getArmory().fillSupplies(Unit.class, enemy.getWorld().getMaxUnitCount() - enemy.getUnitCountContainer().getNumSupplies());
		enemy.getArmory().fillSupplies(IronAxeWeapon.class, Integer.MAX_VALUE);
	}

	public final void updateChecksum(StateChecksum checksum) {}
}
