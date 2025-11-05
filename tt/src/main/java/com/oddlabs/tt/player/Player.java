package com.oddlabs.tt.player;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Army;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.SupplyContainer;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.behaviour.NullController;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Player implements PlayerInterface {
	public final static int INITIAL_UNIT_COUNT = 20;
	public final static int MAX_BUILDING_COUNT = 20;
	public final static int DEFAULT_MAX_UNIT_COUNT = 250;

	public static final float[][] COLORS = {{1f, .75f, 0f, 1f},
											{0f, .5f, 1f, 1f},
											{1f, 0f, .25f, 1f},
											{0f, 1f, .75f, 1f},
											{.75f, 0f, 1f, 1f},
											{.75f, 1f, 0f, 1f}};

	private final @NonNull World world;
	private final PlayerInfo player_info;
	private final Army units = new Army();
	private final @NonNull SupplyContainer unit_count;
	private final SupplyContainer building_count = new SupplyContainer(MAX_BUILDING_COUNT);

	private final float[] color;

//	private final String team_tip;

	private @Nullable AI ai = null;

	private @Nullable Unit chieftain = null;
	private boolean training_chieftain = false;
	private float start_x;
	private float start_y;

	// stats
	private int units_lost;
	private int buildings_lost;
	private int units_killed;
	private int buildings_destroyed;
	private int units_moved;
	private int weapons_thrown;
	private int magics;

	private int tree_harvested;
	private int rock_harvested;
	private int iron_harvested;
	private int rubber_harvested;

	private boolean can_build_chieftains = true;
	private boolean can_repair = true;
	private boolean can_attack = true;
	private final boolean[] can_build = new boolean[Race.NUM_BUILDINGS];
	private boolean can_move = true;
	private boolean can_exit_towers = true;
	private boolean can_use_rubber = true;
	private boolean can_set_rally = true;
	private boolean can_harvest = true;
	private boolean can_build_armies = true;
	private boolean can_build_weapons = true;
	private boolean can_transport = true;
	private final boolean[] can_do_magic = new boolean[RacesResources.NUM_MAGIC];

	private float hit_bonus;

	private int preferred_speed = World.GAMESPEED_DONTCARE;

	public Player(@NonNull World world, PlayerInfo player_info, float[] color) {
		this.world = world;
		this.color = color;
        Arrays.fill(can_do_magic, true);
        Arrays.fill(can_build, true);
		this.player_info = player_info;
		this.unit_count = new SupplyContainer(world.getMaxUnitCount());
//		this.team_tip = Utils.getBundleString(bundle, "team", new Object[]{Integer.toString(player_info.getTeam() + 1)});
	}

        @Override
	public void changePreferredGamespeed(int delta) {
		int old_speed = getGamespeed();
		int new_speed = Math.max(Game.GAMESPEED_PAUSE, Math.min(old_speed + delta, Game.GAMESPEED_LUDICROUS));
		setPreferredGamespeed(new_speed);
	}

        @Override
	public void setPreferredGamespeed(int speed) {
		if (!World.isValidPreferredGamespeed(speed))
			return;
		if (preferred_speed != speed) {
			int old_speed = preferred_speed;
			this.preferred_speed = speed;
			if (World.isValidGamespeed(preferred_speed) && World.isValidGamespeed(old_speed))
				world.getNotificationListener().playerGamespeedChanged();
			world.gamespeedChanged();
		}
	}

	public int getGamespeed() {
		if (World.isValidGamespeed(preferred_speed))
			return preferred_speed;
		else
			return world.getGamespeed();
	}

	public int getPreferredGamespeed() {
		return preferred_speed;
	}

	public float getHitBonus() {
		return hit_bonus;
	}

	public void setHitBonus(float bonus) {
		this.hit_bonus = bonus;
	}

	public @NonNull World getWorld() {
		return world;
	}

	public void enableArmies(boolean enabled) {
		can_build_armies = enabled;
	}

	public void enableWeapons(boolean enabled) {
		can_build_weapons = enabled;
	}

	public void enableTransporting(boolean enabled) {
		can_transport = enabled;
	}

	public void enableHarvesting(boolean enabled) {
		can_harvest = enabled;
	}

	public void enableRubber(boolean enabled) {
		can_use_rubber = enabled;
	}

	public void enableChieftains(boolean enabled) {
		can_build_chieftains = enabled;
	}

	public void enableTowerExits(boolean enabled) {
		can_exit_towers = enabled;
	}

	public void enableRepairing(boolean enabled) {
		can_repair = enabled;
	}

	public void enableBuilding(int building, boolean enabled) {
		can_build[building] = enabled;
	}

	public void enableAttacking(boolean enabled) {
		can_attack = enabled;
	}

	public void enableRallyPoints(boolean enabled) {
		can_set_rally = enabled;
	}

	public void enableMoving(boolean enabled) {
		can_move = enabled;
	}

	public boolean canTransport() {
		return can_transport;
	}

	public boolean canBuildWeapons() {
		return can_build_weapons;
	}

	public boolean canHarvest() {
		return can_harvest;
	}

	public boolean canBuildArmies() {
		return can_build_armies;
	}

	public boolean canSetRallyPoints() {
		return can_set_rally;
	}

	public boolean canUseRubber() {
		return can_use_rubber;
	}

	public void enableMagic(int magic_index, boolean enabled) {
		can_do_magic[magic_index] = enabled;
	}

	public boolean canDoMagic(int magic_index) {
		return can_do_magic[magic_index];
	}

	public boolean canExitTowers() {
		return can_exit_towers;
	}

	public boolean canAttack() {
		return can_attack;
	}

	public boolean canMove() {
		return can_move;
	}

	public boolean canBuild(int building) {
		return can_build[building] && getBuildingCountContainer().getNumSupplies() < Player.MAX_BUILDING_COUNT;
	}

	public boolean canRepair() {
		return can_repair;
	}

	public boolean canBuildChieftains() {
		return can_build_chieftains;
	}

        @Override
	public String toString() {
		return player_info.toString();
	}

	public PlayerInfo getPlayerInfo() {
		return player_info;
	}

	public void setAI(AI ai) {
		this.ai = ai;
	}

	public @Nullable AI getAI() {
		return ai;
	}

	public @Nullable Building buildBuilding(int building_type, int grid_x, int grid_y) {
		BuildingSiteScanFilter filter = new BuildingSiteScanFilter(world.getUnitGrid(), getRace().getBuildingTemplate(building_type), 40, true);
		world.getUnitGrid().scan(filter, grid_x, grid_y);
		List<LandscapeTarget> target_list = filter.getResult();
		Building b = null;
		if (!target_list.isEmpty()) {
			Target t = target_list.getFirst();
			b = new Building(this, getRace().getBuildingTemplate(building_type), t.getGridX(), t.getGridY());
			b.place();
			b.repair(1000);
		}
		return b;
	}

	public void init(float @NonNull [] starting_location) {
		this.start_x = starting_location[0];
		this.start_y = starting_location[1];
	}

	public @Nullable Selectable findNearestEnemy(int start_x, int start_y) {
		return findNearestEnemy(start_x, start_y, null);
	}

	public @Nullable Selectable findNearestEnemy(int start_x, int start_y, Selectable target) {
		return findNearestEnemy(start_x, start_y, target, Selectable.class);
	}

	public int getStatus() {
		Set<Selectable> units = getUnits().getSet();
		Iterator<Selectable> it = units.iterator();
		int status = 0;
		while (it.hasNext()) {
			Selectable s = it.next();
			status += s.getStatusValue();
		}
		return status;
	}

	public @Nullable Selectable findNearestEnemy(int start_x, int start_y, Selectable target, @NonNull Class<? extends Selectable> type) {
		Player[] players = world.getPlayers();
		int best_dist_squared = Integer.MAX_VALUE;
		Selectable best_target = null;
            for (Player player : players) {
                if (isEnemy(player)) {
                    Set<Selectable> units = player.getUnits().getSet();
                    for (Selectable s : units) {
                        if (!(type.isInstance(s)) || s == target) {
                            continue;
                        }
                        int dx = s.getGridX() - start_x;
                        int dy = s.getGridY() - start_y;
                        int dist_squared = dx * dx + dy * dy;
                        if (best_dist_squared > dist_squared) {
                            best_dist_squared = dist_squared;
                            best_target = s;
                        }
                    }
                }
            }
		return best_target;
	}

	public @Nullable Selectable findNearestEnemyBuilding(int start_x, int start_y) {
		return findNearestEnemy(start_x, start_y, null, Building.class);
	}

	public Race getRace() {
		return getWorld().getRacesResources().getRace(player_info.getRace());
	}

	public @NonNull SupplyContainer getUnitCountContainer() {
		return unit_count;
	}

	public @NonNull SupplyContainer getBuildingCountContainer() {
		return building_count;
	}

	public void setActiveChieftain(Unit chieftain) {
		this.chieftain = chieftain;
	}

	public @Nullable Building getArmory() {
		Selectable[][] lists = classifyUnits();
            for (Selectable[] list : lists) {
                Selectable s = list[0];
                if (s.getPrimaryController() instanceof NullController && s.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
                    return (Building)s;
                }
            }
		return null;
	}

	public @Nullable Building getQuarters() {
		Selectable[][] lists = classifyUnits();
            for (Selectable[] list : lists) {
                Selectable s = list[0];
                if (s.getPrimaryController() instanceof NullController && s.getAbilities().hasAbilities(Abilities.REPRODUCE)) {
                    return (Building)s;
                }
            }
		return null;
	}

	public boolean isAlive() {
		int units = getUnitCountContainer().getNumSupplies();
		return units > 0 || hasActiveChieftain() || getQuarters() != null;
	}


	public boolean hasActiveChieftain() {
		return chieftain != null;
	}

	public @Nullable Unit getChieftain() {
		return chieftain;
	}

	public void setTrainingChieftain(boolean training_chieftain) {
		assert this.training_chieftain != training_chieftain;
		this.training_chieftain = training_chieftain;
	}

	public boolean isTrainingChieftain() {
		return training_chieftain;
	}

	public float[] getColor() {
		return color;
	}

        @Override
	public void deployUnits(@NonNull Building building, @NonNull DeployType type, int num_units) {
		if (isValid(building))
			building.deployUnits(type, num_units);
	}

        @Override
	public void createHarvesters(@NonNull Building building, int num_tree, int num_rock, int num_iron, int num_rubber) {
		if (isValid(building))
			building.createHarvesters(num_tree, num_rock, num_iron, num_rubber);
	}

        @Override
	public void buildRockWeapons(@NonNull Building building, int num_weapons, boolean infinite) {
		if (isValid(building))
			building.buildWeapons(RockAxeWeapon.class, num_weapons, infinite);
	}

        @Override
	public void buildIronWeapons(@NonNull Building building, int num_weapons, boolean infinite) {
		if (isValid(building))
			building.buildWeapons(IronAxeWeapon.class, num_weapons, infinite);
	}

        @Override
	public void buildRubberWeapons(@NonNull Building building, int num_weapons, boolean infinite) {
		if (isValid(building))
			building.buildWeapons(RubberAxeWeapon.class, num_weapons, infinite);
	}

        @Override
	public void doMagic(@NonNull Unit chieftain, int magic) {
		if (isValid(chieftain))
			chieftain.doMagic(magic, true);
	}

        @Override
	public void exitTower(@NonNull Building building) {
		if (isValid(building))
			building.exitTower();
	}

        @Override
	public void trainChieftain(@NonNull Building building, boolean start) {
		if (isValid(building))
			building.trainChieftain(start);
	}

        @Override
	public void placeBuilding(Selectable @NonNull [] selection, int template_id, int placing_grid_x, int placing_grid_y) {
		Building building = new Building(this, getRace().getBuildingTemplate(template_id), placing_grid_x, placing_grid_y);
            for (Selectable selection1 : selection) {
                if (isValid(selection1)) {
                    selection1.initTarget(building, Target.ACTION_DEFAULT, false);
                }
            }
	}

        @Override
	public void setRallyPoint(@NonNull Building building, @Nullable Target target) {
		if (isValid(building) && target != null)
			building.setRallyPoint(target);
	}

        @Override
	public void setRallyPoint(@NonNull Building building, int grid_x, int grid_y) {
		setRallyPoint(building, new LandscapeTarget(grid_x, grid_y));
	}

        @Override
	public void setTarget(Selectable @NonNull [] selection, Target target, int action, boolean aggressive) {
            for (Selectable selection1 : selection) {
                if (isValid(selection1)) {
                    selection1.initTarget(target, action, aggressive);
                }
            }
	}

	public void killSelection(Selectable @NonNull [] selection) {
            for (Selectable selection1 : selection) {
                if (selection1 != null) {
                    selection1.hit(10000, 0f, 1f, this);
                }
            }
	}

        @Override
	public void setLandscapeTarget(Selectable @NonNull [] selection, int grid_x, int grid_y, int action, boolean aggressive) {
		if (selection.length == 0)
			return;
		int grid_size = world.getUnitGrid().getGridSize();
		if (grid_x < 0 || grid_x >= grid_size || grid_y < 0 || grid_y >= grid_size)
			return;
		Target[] targets = world.getUnitGrid().findGridTargets(grid_x, grid_y, selection.length, selection.length != 1);
		for (int i = 0; i < selection.length; i++) {
			if (isValid(selection[i]))
				selection[i].initTarget(targets[i], action, aggressive);
		}
	}

	private boolean isValid(@Nullable Selectable s) {
		return s != null && !s.isDead() && s.getOwner() == this;
	}

	public float getStartX() {
		return start_x;
	}

	public void setStartX(float x) {
		start_x = x;
	}

	public float getStartY() {
		return start_y;
	}

	public void setStartY(float y) {
		start_y = y;
	}

	public boolean isEnemy(@NonNull Player other_player) {
		if (other_player.player_info.getTeam() == PlayerInfo.TEAM_NEUTRAL
				|| this.player_info.getTeam() == PlayerInfo.TEAM_NEUTRAL) {
			return false;
		}
		return other_player.player_info.getTeam() != this.player_info.getTeam();
	}

	public boolean teamHasBuilding() {
		Player[] players = world.getPlayers();
            for (Player player : players) {
                if (player.getPlayerInfo().getTeam() == player_info.getTeam() && player.getBuildingCountContainer().getNumSupplies() > 0) {
                    return true;
                }
            }
		return false;
	}

	public @NonNull Army getUnits() {
		return units;
	}

	public Selectable[] @NonNull [] classifyUnits() {
		Map<String, List<Selectable>> map = new HashMap<>();
		List<List<Selectable>> lists = new ArrayList<>();
            for (Selectable unit : units.getSet()) {
                String key = unit.getPrimaryController().getKey();
                List<Selectable> list = map.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    map.put(key, list);
                    lists.add(list);
                }
                list.add(unit);
            }
		Selectable[][] result = new Selectable[lists.size()][];
		for (int i = 0; i < result.length; i++) {
			List<Selectable> list = lists.get(i);
			Selectable[] array = new Selectable[list.size()];
			list.toArray(array);
			result[i] = array;
		}
		return result;
	}

	public void magicCast() {
		magics++;
	}

	public int getMagics() {
		return magics;
	}

	public void weaponThrown() {
		weapons_thrown++;
	}

	public int getWeaponsThrown() {
		return weapons_thrown;
	}

	public void unitMoved() {
		units_moved++;
	}

	public int getUnitsMoved() {
		return units_moved;
	}

	public void unitLost() {
		units_lost++;
	}

	public int getUnitsLost() {
		return units_lost;
	}

	public void buildingLost() {
		buildings_lost++;
	}

	public int getBuildingsLost() {
		return buildings_lost;
	}

	public void unitKilled() {
		units_killed++;
	}

	public int getUnitsKilled() {
		return units_killed;
	}

	public void buildingDestroyed() {
		buildings_destroyed++;
	}

	public int getBuildingsDestroyed() {
		return buildings_destroyed;
	}

	public void harvested(Class<?> type) {
		if (type == TreeSupply.class) {
			tree_harvested++;
		} else if (type == RockSupply.class) {
			rock_harvested++;
		} else if (type == IronSupply.class) {
			iron_harvested++;
		} else if (type == RubberSupply.class) {
			rubber_harvested++;
		} else
			throw new RuntimeException();
	}

	public int getTreeHarvested() {
		return tree_harvested;
	}

	public int getRockHarvested() {
		return rock_harvested;
	}

	public int getIronHarvested() {
		return iron_harvested;
	}

	public int getRubberHarvested() {
		return rubber_harvested;
	}
}
