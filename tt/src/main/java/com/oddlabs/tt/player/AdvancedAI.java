package com.oddlabs.tt.player;

import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.Difficulty;

import java.util.EnumMap;
import java.util.Map;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.model.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class AdvancedAI extends AI {

    private static final int SCORE_PEON = 1;
    private static final int SCORE_WARRIOR_ROCK = 4;
    private static final int SCORE_WARRIOR_IRON = 5;
    private static final int SCORE_WARRIOR_RUBBER = 10;
    private static final int SCORE_CHIEFTAIN = 25;

    private record DifficultyParams(
                                    float defenseFactor,
                                    int minUnitsBuildingWeapons,
                                    int minWeaponsInStock,
                                    int minUnitsReproducing,
                                    int maxUnitsGatheringTree,
                                    int maxUnitsGatheringRock,
                                    int maxUnitsGatheringIron,
                                    int maxUnitsGatheringRubber,
                                    int unitsPerTower1,
                                    int unitsPerTower2,
                                    int numWarriorsDefault,
                                    int numWarriorsIncrease,
                                    int numWarriorsMax,
                                    int numWarriorsForChieftain
    ) {
    }

    private static final EnumMap<Difficulty, DifficultyParams> PARAMS = new EnumMap<>(Map.of(
            Difficulty.EASY, new DifficultyParams(
                    1f,    // defenseFactor
                    0,     // minUnitsBuildingWeapons
                    10,    // minWeaponsInStock
                    0,     // minUnitsReproducing
                    2,     // maxUnitsGatheringTree
                    1,     // maxUnitsGatheringRock
                    1,     // maxUnitsGatheringIron
                    0,     // maxUnitsGatheringRubber
                    1000,  // unitsPerTower1
                    1000,  // unitsPerTower2
                    3,     // numWarriorsDefault
                    1,     // numWarriorsIncrease
                    10,    // numWarriorsMax
                    1000   // numWarriorsForChieftain
            ),
            Difficulty.NORMAL, new DifficultyParams(
                    1.5f,  // defenseFactor
                    3,     // minUnitsBuildingWeapons
                    5,     // minWeaponsInStock
                    5,     // minUnitsReproducing
                    5,     // maxUnitsGatheringTree
                    3,     // maxUnitsGatheringRock
                    3,     // maxUnitsGatheringIron
                    0,     // maxUnitsGatheringRubber
                    100,   // unitsPerTower1
                    130,   // unitsPerTower2
                    7,     // numWarriorsDefault
                    3,     // numWarriorsIncrease
                    25,    // numWarriorsMax
                    20     // numWarriorsForChieftain
            ),
            Difficulty.HARD, new DifficultyParams(
                    2f,    // defenseFactor
                    8,     // minUnitsBuildingWeapons
                    0,     // minWeaponsInStock
                    20,    // minUnitsReproducing
                    15,    // maxUnitsGatheringTree
                    9,     // maxUnitsGatheringRock
                    10,    // maxUnitsGatheringIron
                    3,     // maxUnitsGatheringRubber
                    90,    // unitsPerTower1
                    120,   // unitsPerTower2
                    10,    // numWarriorsDefault
                    5,     // numWarriorsIncrease
                    40,    // numWarriorsMax
                    15     // numWarriorsForChieftain
            )
    ));

    private final DifficultyParams params;
    private final @NonNull Difficulty difficulty;

    private int numWarriors;

    private @Nullable LandscapeTarget defense_target = null;

    public AdvancedAI(@NonNull Player owner, UnitInfo unit_info, @NonNull Difficulty difficulty) {
        super(owner, unit_info);
        this.difficulty = difficulty;
        this.params = PARAMS.get(difficulty);
        this.numWarriors = params.numWarriorsDefault();
    }

    public @NonNull Difficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public void animate(float t) {
        if (!shouldDoAction(t))
            return;
        reclassify();
        nodeDefendBase();
        reclassify();
        if (getOwner().getUnitCountContainer().getNumSupplies() > params.unitsPerTower2())
            nodeGuardTowers(2);
        else if (getOwner().getUnitCountContainer().getNumSupplies() > params.unitsPerTower1())
            nodeGuardTowers(1);

        reclassify();
        nodeAttackWithWarriorsAndChieftain(numWarriors, numWarriors
                >= params.numWarriorsForChieftain());
        nodeAssignIdlePeons();
        if (getOwner().hasActiveChieftain()) {
            getOwner().getRaceInfo().getChieftainAI().decide(getOwner().getChieftain().orElseThrow());
        }
    }

    private void nodeDefendBase() {
        int enemy_score = 0;
        if (getQuarters() != null) {
            enemy_score = scanForEnemies(getQuarters()[0]);
        }
        if (getArmory() != null && enemy_score == 0) {
            enemy_score = scanForEnemies(getArmory()[0]);
        }
        enemy_score = (int) (params.defenseFactor() * enemy_score);
        if (getDefendingUnits() != null) {
            for (Selectable<?> defendingUnit : getDefendingUnits()) {
                enemy_score -= getUnitScore((Unit) defendingUnit);
            }
        }
        if (enemy_score > 0) {
            nodeDeployArmy();
            nodeDefend(enemy_score);
        }
    }

    private void nodeDeployArmy() {
        if (getArmory() != null) {
            Building armory = (Building) getArmory()[0];
            int num_units = armory.getUnitContainer().orElseThrow().getNumSupplies()
                    - params.minUnitsBuildingWeapons();
            int num_weapons = numWeapons(armory) - params.minWeaponsInStock();
            if (num_units <= 0 || num_weapons <= 0)
                return;

            int num_warriors = Math.min(num_units, num_weapons);
            int num_rubber_units = Math.min(num_warriors, armory.getSupplyContainer(RubberAxeWeapon.class)
                    .orElseThrow().getNumSupplies());
            int num_iron_units = Math.min(num_warriors - num_rubber_units, armory.getSupplyContainer(
                    IronAxeWeapon.class).orElseThrow().getNumSupplies());
            int num_rock_units = Math.min(num_warriors - num_rubber_units - num_iron_units, armory.getSupplyContainer(
                    RockAxeWeapon.class).orElseThrow().getNumSupplies());
            if (num_rubber_units > 0) {
                getOwner().deployUnits(armory, DeployType.RUBBER_WARRIOR, num_rubber_units);
//				deployed += num_rubber_units*SCORE_WARRIOR_RUBBER;
            }
            if (num_iron_units > 0) {
                getOwner().deployUnits(armory, DeployType.IRON_WARRIOR, num_iron_units);
//				deployed += num_iron_units*SCORE_WARRIOR_IRON;
            }
            if (num_rock_units > 0) {
                getOwner().deployUnits(armory, DeployType.ROCK_WARRIOR, num_rock_units);
//				deployed += num_rock_units*SCORE_WARRIOR_ROCK;
            }
            num_units = armory.getUnitContainer().orElseThrow().getNumSupplies();
            if (num_units > 0) {
                getOwner().deployUnits(armory, DeployType.PEON, num_units);
//				deployed += num_units*SCORE_PEON;
            }
//			result += deployed;
        }
    }

    private void nodeDefend(int score) {
        List<Unit> unit_list = new ArrayList<>();

        int result = 0;
        if (getIdleWarriors() != null && result < score) {
            result = addFromList(getIdleWarriors(), unit_list, result, score);
        }
        if (getIdlePeons() != null && result < score) {
            result = addFromList(getIdlePeons(), unit_list, result, score);
        }
        if (getGatherTreePeons() != null && result < score) {
            result = addFromList(getGatherTreePeons(), unit_list, result, score);
        }
        if (getGatherRockPeons() != null && result < score) {
            result = addFromList(getGatherRockPeons(), unit_list, result, score);
        }
        if (getGatherIronPeons() != null && result < score) {
            result = addFromList(getGatherIronPeons(), unit_list, result, score);
        }
        if (getGatherRubberPeons() != null && result < score) {
            result = addFromList(getGatherRubberPeons(), unit_list, result, score);
        }

        if (result > 0) {
            Unit[] units = new Unit[unit_list.size()];
            unit_list.toArray(units);
            getOwner().setLandscapeTarget(units, defense_target.getGridX(), defense_target.getGridY(), Action.DEFEND,
                    true);
        }
    }

    private int addFromList(Selectable<?> @NonNull [] list, @NonNull List<Unit> new_list, int progress, int score) {
        int result = progress;
        for (Selectable<?> list1 : list) {
            Unit unit = (Unit) list1;
            new_list.add(unit);
            result += getUnitScore(unit);
            if (result > score)
                break;
        }
        return result;
    }

    private int scanForEnemies(@NonNull Selectable<?> src) {
        FindOccupantFilter<Unit> filter = new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), 30f, src,
                Unit.class);
        getUnitGrid().scan(filter, src.getGridX(), src.getGridY());
        int score = 0;
        defense_target = null;
        for (Unit unit : filter.getResult()) {
            if (!unit.isDead() && getOwner().isEnemy(unit.getOwner())) {
                score += getUnitScore(unit);
                if (defense_target == null)
                    defense_target = new LandscapeTarget(unit.getGridX(), unit.getGridY());
            }
        }
        return score;
    }

    private int getUnitScore(@NonNull Unit unit) {
        if (unit.getAbilities().hasAbilities(Abilities.HARVEST)) {
            return SCORE_PEON;
        } else if (unit.getAbilities().hasAbilities(Abilities.MAGIC)) {
            return SCORE_CHIEFTAIN;
        } else {
            var type = unit.getWeaponFactory().getType().orElse(null);
            if (type == RockAxeWeapon.class || type == RockSpearWeapon.class) {
                return SCORE_WARRIOR_ROCK;
            } else if (type == IronAxeWeapon.class || type == IronSpearWeapon.class) {
                return SCORE_WARRIOR_IRON;
            } else if (type == RubberAxeWeapon.class || type == RubberSpearWeapon.class) {
                return SCORE_WARRIOR_RUBBER;
            }
        }
        throw new IllegalArgumentException("Unit has no valid weapon or magic abilities");
    }

    private void nodeGuardTowers(int num_towers) {
        if ((getTowers() == null && num_towers > 0) || (getTowers() != null && num_towers > getTowers().length)) {
            nodeBuildTower(num_towers);
        } else if (num_towers > 0) {
            for (int i = 0; i < getTowers().length; i++) {
                if (!((Building) getTowers()[i]).getUnitContainer().orElseThrow().isSupplyFull() && getIdleWarriors()
                        != null
                        && getIdleWarriors().length > i) {
                    getOwner().setTarget(Selectable.newArray(getIdleWarriors()[i]), getTowers()[i], Action.DEFAULT,
                            false);
                    nodeDeployUnitsInArmory(1);
                }
            }
        }
    }

    private void nodeBuildTower(int number) {
        if (!towerUnderConstruction() && ((getTowers() == null && number == 1) || (getTowers() != null
                && getTowers().length < number)) && getQuarters() != null && getArmory() != null) {
            Selectable<?>[] builders = getPeons(10);
            if (builders.length == 0)
                return;

            Building origin = number % 2 == 1 ? (Building) getQuarters()[0] : (Building) getArmory()[0];
            int ox = origin.getGridX();
            int oy = origin.getGridY();
            int center = getOwner().getWorld().getHeightMap().getGridUnitsPerWorld() / 2;
            int dx = center - ox;
            int dy = center - oy;
            float inv_dist = 1f / (float) Math.sqrt(dx * dx + dy * dy);
            int tx = (int) (ox + 10f * dx * inv_dist);
            int ty = (int) (oy + 10f * dy * inv_dist);
            setTowerUnderConstruction(buildBuilding(BuildingType.TOWER, builders, tx, ty));
        }
    }

    private void nodeAssignIdlePeons() {
        if (getIdlePeons() != null) {
            if (quartersUnderConstruction() && getConstructionSites() != null) {
                getOwner().setTarget(getIdlePeons(), getConstructionSites()[0], Action.DEFAULT, false);
            } else if (armoryUnderConstruction() && getConstructionSites() != null) {
                getOwner().setTarget(getIdlePeons(), getConstructionSites()[0], Action.DEFAULT, false);
            } else if (towerUnderConstruction() && getConstructionSites() != null) {
                getOwner().setTarget(getIdlePeons(), getConstructionSites()[0], Action.DEFAULT, false);
            } else if (getQuarters() != null && !getQuarters()[0].isDead()) {
                getOwner().setTarget(getIdlePeons(), getQuarters()[0], Action.DEFAULT, false);
            }
        }
    }

    private void nodeAttackWithWarriorsAndChieftain(int num_warriors, boolean use_chieftain) {
        /*
        System.out.print("nodeAttackWithWarriorsAndChieftain");
        if (getIdleWarriors() == null)
        	System.out.println(" | no idling warriors");
        else
        	System.out.println(" | " + getIdleWarriors().length + " idling warriors");
        */
        if (getIdleWarriors() != null && getIdleWarriors().length >= num_warriors
                && (!use_chieftain || getOwner().hasActiveChieftain())) {
            boolean idle_chieftain = getIdleChieftains() != null && getIdleChieftains().length >= 1;
            Selectable<?>[] warriors;
            if (idle_chieftain && use_chieftain) {
                warriors = Selectable.newArray(num_warriors + 1);
                warriors[num_warriors] = getIdleChieftains()[0];
            } else {
                warriors = Selectable.newArray(num_warriors);
            }

            System.arraycopy(getIdleWarriors(), 0, warriors, 0, num_warriors);
            Target target = findTarget(warriors[0].getGridX(), warriors[0].getGridY());
            if (target != null) {
                getOwner().setLandscapeTarget(warriors, target.getGridX(), target.getGridY(), Action.ATTACK, true);
                if (numWarriors < params.numWarriorsMax())
                    numWarriors += params.numWarriorsIncrease();
            }
        } else {
            if (getIdleWarriors() != null) {
                nodeDeployUnitsInArmory(num_warriors - getIdleWarriors().length);
            } else {
                nodeDeployUnitsInArmory(num_warriors);
            }
            if (use_chieftain)
                nodeTrainChieftain();
        }
    }

    private void nodeTrainChieftain() {
        if (!getOwner().hasActiveChieftain() && !getOwner().isTrainingChieftain()) {
            if (getQuarters() != null) {
                getOwner().trainChieftain((Building) getQuarters()[0], true);
            }
        }
    }

    private void nodeDeployUnitsInArmory(int num_warriors) {
        Building armory = null;
        if (getArmory() != null && getArmory().length > 0) {
            armory = (Building) getArmory()[0];
        }
        if (armory != null) {
            if (!armory.isDead()) {
                int num_units = armory.getUnitContainer().orElseThrow().getNumSupplies()
                        - params.minUnitsBuildingWeapons();
                int num_weapons = numWeapons(armory) - params.minWeaponsInStock();

                if (num_units >= num_warriors && num_weapons >= num_warriors) {
                    int num_rubber_units = Math.min(num_warriors, armory.getSupplyContainer(RubberAxeWeapon.class)
                            .orElseThrow().getNumSupplies());
                    int num_iron_units = Math.min(num_warriors - num_rubber_units, armory.getSupplyContainer(
                            IronAxeWeapon.class).orElseThrow().getNumSupplies());
                    int num_rock_units = Math.min(num_warriors - num_rubber_units - num_iron_units, armory
                            .getSupplyContainer(RockAxeWeapon.class).orElseThrow().getNumSupplies());
                    if (num_rubber_units > 0)
                        getOwner().deployUnits(armory, DeployType.RUBBER_WARRIOR, num_rubber_units);
                    if (num_iron_units > 0)
                        getOwner().deployUnits(armory, DeployType.IRON_WARRIOR, num_iron_units);
                    if (num_rock_units > 0)
                        getOwner().deployUnits(armory, DeployType.ROCK_WARRIOR, num_rock_units);
                } else {
                    if (num_units < num_warriors) {
                        nodeTransferUnits(num_warriors - num_units, armory);
                    }
                    if (num_weapons < num_warriors) {
                        nodeGather(armory, num_units);
                    }
                }
            }
        } else {
            nodeBuildArmory();
        }
    }

    private void nodeGather(@NonNull Building armory, int num_units) {
        int tree = 0;
        int rock = 0;
        int iron = 0;
        int rubber = 0;

        if (getGatherTreePeons() != null)
            tree = getGatherTreePeons().length;
        if (getGatherRockPeons() != null)
            rock = getGatherRockPeons().length;
        if (getGatherIronPeons() != null)
            iron = getGatherIronPeons().length;
        if (getGatherRubberPeons() != null)
            rubber = getGatherRubberPeons().length;

        if (tree >= params.maxUnitsGatheringTree())
            tree = Integer.MAX_VALUE;
        if (rock >= params.maxUnitsGatheringRock())
            rock = Integer.MAX_VALUE;
        if (iron >= params.maxUnitsGatheringIron())
            iron = Integer.MAX_VALUE;
        if (rubber >= params.maxUnitsGatheringRubber())
            rubber = Integer.MAX_VALUE;

        boolean deployed;
        do {
            deployed = false;
            if (num_units > 0 && tree < params.maxUnitsGatheringTree() && tree <= rock && tree <= iron
                    && tree
                            <= rubber) {
                getOwner().deployUnits(armory, DeployType.PEON_HARVEST_TREE, 1);
                deployed = true;
                tree++;
            } else if (num_units > 0 && rock < params.maxUnitsGatheringRock() && rock <= tree && rock
                    <= iron
                    && rock <= rubber) {
                        getOwner().deployUnits(armory, DeployType.PEON_HARVEST_ROCK, 1);
                        deployed = true;
                        rock++;
                    } else if (num_units > 0 && iron < params.maxUnitsGatheringIron() && iron <= tree
                            && iron
                                    <= rock && iron <= rubber) {
                                        getOwner().deployUnits(armory, DeployType.PEON_HARVEST_IRON, 1);
                                        deployed = true;
                                        iron++;
                                    } else if (num_units > 0 && rubber < params.maxUnitsGatheringRubber()
                                            && rubber
                                                    <= tree && rubber <= rock && rubber <= iron) {
                                                        getOwner().deployUnits(armory, DeployType.PEON_HARVEST_RUBBER,
                                                                1);
                                                        deployed = true;
                                                        rubber++;
                                                    }
            num_units--;
        } while (deployed);
    }

    private void nodeTransferUnits(int num_units, @NonNull Building armory) {
        Building quarters = null;
        if (getQuarters() != null && getQuarters().length > 0) {
            quarters = (Building) getQuarters()[0];
        }
        if (quarters != null) {
            if (!quarters.isDead()) {
                quarters.setRallyPoint(armory);
                if (quarters.getUnitContainer().orElseThrow().getNumSupplies() > params.minUnitsReproducing()) {
                    int units = Math.min(num_units, quarters.getUnitContainer().orElseThrow().getNumSupplies()
                            - params.minUnitsReproducing());
                    getOwner().deployUnits(quarters, DeployType.PEON, units);
                }
            }
        } else {
            nodeBuildQuarters();
        }
    }

    private void nodeBuildArmory() {
        if (!quartersUnderConstruction() && getQuarters() == null) {
            nodeBuildQuarters();
        }
        Building quarters = null;
        if (getQuarters() != null && getQuarters().length > 0) {
            quarters = (Building) getQuarters()[0];
        }
        if (!armoryUnderConstruction() && getArmory() == null
                && getQuarters() != null && getQuarters()[0].getAbilities().hasAbilities(Abilities.REPRODUCE)) {
            Selectable<?>[] builders = getPeons(20);
            if (builders.length < 20) {
                if (quarters != null && !quarters.isDead() && quarters.getUnitContainer().orElseThrow().getNumSupplies()
                        >= 20)
                    getOwner().deployUnits(quarters, DeployType.PEON, 20);
            }
            if (builders.length == 0)
                return;

            // TODO: Should use Quarters as origin, if it exists
            setArmoryUnderConstruction(buildBuilding(
                    BuildingType.ARMORY, builders, builders[0].getGridX(), builders[0]
                            .getGridY()));
            reclassify();
        }
    }

    private void nodeBuildQuarters() {
        if (!quartersUnderConstruction() && getQuarters() == null) {
            Selectable<?>[] builders = getPeons(params.minUnitsReproducing());
            if (builders.length == 0)
                return;

            // TODO: Should use Armory as origin, if it exists
            setQuartersUnderConstruction(buildBuilding(
                    BuildingType.QUARTERS, builders, builders[0].getGridX(),
                    builders[0].getGridY()));
            reclassify();
        }
    }

    private @NonNull Selectable<?> @NonNull [] getPeons(int min_num_peons) {
        var idle = getIdlePeons();
        int idleCount = null != idle ? idle.length : 0;
        var active = Stream.of((Supplier<Selectable<?>[]>) this::getGatherIronPeons, this::getGatherRockPeons,
                this::getGatherTreePeons, this::getGatherRubberPeons)
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .limit(Math.max(min_num_peons - idleCount, 0));
        return (null != idle ? Stream.concat(Arrays.stream(idle), active) : active)
                .toArray(Selectable[]::new);
    }

    /*	private final int getNumUnitsDeploying() {
            int result = 0;
            if (getArmory() != null) {
                Building armory = (Building)getArmory()[0];
                result += armory.getDeployContainer(DeployType.ROCK_WARRIOR).getNumSupplies();
                result += armory.getDeployContainer(DeployType.IRON_WARRIOR).getNumSupplies();
                result += armory.getDeployContainer(DeployType.RUBBER_WARRIOR).getNumSupplies();
                result += armory.getDeployContainer(DeployType.PEON).getNumSupplies();
            }
            return result;
        }
    */
    private int numWeapons(@NonNull Building armory) {
        return armory.getSupplyContainer(RockAxeWeapon.class).orElseThrow().getNumSupplies()
                + armory.getSupplyContainer(IronAxeWeapon.class).orElseThrow().getNumSupplies()
                + armory.getSupplyContainer(RubberAxeWeapon.class).orElseThrow().getNumSupplies();
    }

    private @Nullable Target findTarget(int start_x, int start_y) {
        Target best_building = getOwner().findNearestEnemyBuilding(start_x, start_y).orElse(null);
        Target best_target = getOwner().findNearestEnemy(start_x, start_y).orElse(null);
        if (best_building == null) {
            return best_target;
        }
        if (best_target == null) {
            return null;
        }

        int squared_dist_building = (best_building.getGridX() - start_x) * (best_building.getGridX() - start_x) + (best_building.getGridY() - start_y) * (best_building.getGridY() - start_y);
        int squared_dist_target = (best_target.getGridX() - start_x) * (best_target.getGridX() - start_x) + (best_target.getGridY() - start_y) * (best_target.getGridY() - start_y);

        return squared_dist_target < squared_dist_building / 2 ? best_target : best_building;
    }

    private boolean buildBuilding(@NonNull BuildingType building_type, Selectable<?> @NonNull [] selection, int grid_x,
            int grid_y) {
        BuildingSiteScanFilter filter = new BuildingSiteScanFilter(getUnitGrid(), getOwner().getRaceInfo()
                .getBuildingTemplate(building_type), 40, true);
        getUnitGrid().scan(filter, grid_x, grid_y);
        List<? extends Target> target_list = filter.getResult();
        if (!target_list.isEmpty()) {
            Target target = target_list.getFirst();
            getOwner().placeBuilding(selection, building_type, target.getGridX(), target.getGridY());
            return true;
        } else {
            return false;
        }
    }
}
