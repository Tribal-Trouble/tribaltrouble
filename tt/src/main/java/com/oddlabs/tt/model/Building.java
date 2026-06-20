package com.oddlabs.tt.model;

import com.oddlabs.tt.gui.BuildSpinner;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.behaviour.AttackController;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.model.behaviour.NullController;
import com.oddlabs.tt.model.behaviour.StunController;
import com.oddlabs.tt.model.behaviour.TransferUnitController;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.model.weapon.ThrowingWeapon;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a static building structure in the game world.
 */
public final class Building extends Selectable<BuildingTemplate> implements Occupant {
    private static final float REMOVE_DELAY = 1f / 10f;

    private static final int PLACING_BORDER = 1;
    private static final int MAX_SUPPLY_COUNT = 200;

    public static final Cost COST_ROCK_WEAPON = new Cost(Map.of(
            SupplyType.WOOD, 2,
            SupplyType.ROCK, 1));
    public static final Cost COST_IRON_WEAPON = new Cost(Map.of(
            SupplyType.WOOD, 2,
            SupplyType.IRON, 1));
    public static final Cost COST_RUBBER_WEAPON = new Cost(Map.of(
            SupplyType.WOOD, 2,
            SupplyType.ROCK, 1,
            SupplyType.IRON, 1,
            SupplyType.RUBBER, 1));

    private static final Color.Linear DAMAGE_BASE_COLOR = new Color.Standard(0.3f, 0.8f).linear();
    private static final Color.Linear DAMAGE_FACTOR_END = new Color.Linear(0.3f, 1.0f);
    private static final Color.Linear SOOT_TINT = new Color.Linear(1.0f, 0.9f, 0.7f, 1.0f);
    private static final Color.LinearDelta SOOT_DELTA = Color.LinearDelta.red(0.05f);

    private final Map<@NonNull Class<?>, @NonNull SupplyContainer> supply_containers = new HashMap<>();
    private final Map<@NonNull SupplyType, @NonNull SupplyContainer> resource_containers
            = new EnumMap<>(SupplyType.class);
    private final Map<@NonNull Class<?>, @NonNull BuildProductionContainer> build_containers = new HashMap<>();
    private final Map<@NonNull DeployType, @NonNull DeployContainer> deploy_containers
            = new EnumMap<>(DeployType.class);

    private @Nullable ChieftainContainer chieftain_container = null;
    private @Nullable WeaponsProducer weapons_producer = null;
    private float remove_delay = 0;
    private int hit_points = 1;
    private int build_points = 0; // 0 = not placed, > 0 = placed
    private float[][] old_landscape_heights;

    private @NonNull Target rally_point = this;
    private boolean is_training_chieftain = false;

    /**
     * Represents the construction stages of a building in the game world.
     */
    public enum BuildStage {
        /** The building template has not yet been placed on the terrain grid. */
        UNPLACED,
        /** Construction has started but is under 50% completed. */
        START,
        /** Construction progress is between 50% and 99%. */
        HALFBUILT,
        /** Construction is completed (100% built). */
        BUILT
    }

    public Building(@NonNull Player owner, @NonNull BuildingTemplate template, int grid_x, int grid_y) {
        super(owner, template);
        setGridPosition(grid_x, grid_y);
        float x = UnitGrid.coordinateFromGrid(grid_x);
        float y = UnitGrid.coordinateFromGrid(grid_y);
        setPosition(x, y);
        pushController(new NullController(this));
    }

    public boolean hasRallyPoint() {
        return rally_point != this;
    }

    public @NonNull Target getRallyPoint() {
        return rally_point;
    }

    public boolean isProducing() {
        return weapons_producer != null && weapons_producer.isProducing();
    }

    @Override
    protected void doAnimate(float t) {
        if (!isDead()) {
            getUnitContainer().ifPresent(unit_container -> unit_container.animate(t));
            if (weapons_producer != null) {
                weapons_producer.animate(t);
            }

            int num_deploying = 0;
            for (DeployContainer deploy_container : deploy_containers.values()) {
                if (deploy_container.getNumSupplies() > 0) {
                    num_deploying++;
                }
            }
            if (num_deploying > 0) {
                float amount = t / num_deploying;
                for (DeployContainer deploy_container : deploy_containers.values()) {
                    if (deploy_container.getNumSupplies() > 0) {
                        deploy_container.deploy(amount);
                    }
                }
            }
        }

        if (remove_delay > 0) {
            remove_delay -= t;
            if (remove_delay <= 0) {
                remove();
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
        getClientState(ModelClient.class).ifPresent(ModelClient::close);
    }

    public @NonNull Optional<UnitContainer> getUnitContainer() {
        assert !isDead();
        return getSupplyContainer(Unit.class).map(c -> (UnitContainer) c);
    }

    public @NonNull Optional<SupplyContainer> getSupplyContainer(@NonNull Class<?> key) {
        assert !isDead();
        SupplyContainer container = supply_containers.get(key);
        if (container == null) {
            SupplyType type = SupplyType.fromClass(key);
            if (type != null) {
                container = resource_containers.get(type);
            }
        }
        return Optional.ofNullable(container);
    }

    public @NonNull Optional<SupplyContainer> getSupplyContainer(@NonNull SupplyType key) {
        assert !isDead();
        return Optional.ofNullable(resource_containers.get(key));
    }

    public @NonNull Optional<BuildSupplyContainer> getBuildSupplyContainer(@NonNull Class<?> key) {
        assert !isDead();
        return Optional.ofNullable(build_containers.get(key));
    }

    public DeployContainer getDeployContainer(DeployType type) {
        assert !isDead();
        return deploy_containers.get(type);
    }

    public @NonNull Optional<ChieftainContainer> getChieftainContainer() {
        assert !isDead();
        return Optional.ofNullable(chieftain_container);
    }

    @Override
    public boolean isEnabled() {
        return !isDead();
    }

    public int getUnitCount() {
        assert !isDead();
        return getUnitContainer().map(SupplyContainer::getNumSupplies).orElse(0);
    }

    public boolean canExitTower() {
        return !isDead() && getAbilities().hasAbilities(Abilities.ATTACK)
                && getUnitContainer().map(c -> c.getNumSupplies() > 0).orElse(false)
                && getOwner().canExitTowers() &&
                getUnitContainer().map(c -> !(((MountUnitContainer) c).getUnit()
                        .getCurrentController() instanceof StunController)).orElse(false);
    }

    public void exitTower() {
        assert !isDead();
        if (canExitTower()) {
            getUnitContainer().ifPresent(UnitContainer::exit);
        }
    }

    public void deployUnits(@NonNull DeployType type, int num_units) {
        assert !isDead();
        getOwner().getWorld().updateGlobalChecksum(type.ordinal());
        getOwner().getWorld().updateGlobalChecksum(num_units);
        getDeployContainer(type).orderSupply(num_units);
    }

    public void createHarvesters(int num_tree, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createHarvesters(SupplyType.WOOD, num_tree);
        createHarvesters(SupplyType.ROCK, num_rock);
        createHarvesters(SupplyType.IRON, num_iron);
        createHarvesters(SupplyType.RUBBER, num_rubber);
    }

    private void createHarvesters(@NonNull SupplyType supplyType, int amount) {
        RaceInfo raceInfo = getOwner().getRaceInfo();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().ifPresent(c -> {
                c.prepareDeploy(-1);
                c.exit();
            });
            Unit unit = createUnit(null, raceInfo.getUnitTemplate(UnitType.PEON));
            unit.pushController(new GatherController<>(unit, null, supplyType));
        }
    }

    public void buildWeapons(@NonNull Class<? extends ThrowingWeapon> type, int num_weapons, boolean infinite) {
        assert !isDead();
        if (infinite)
            getOwner().getWorld().updateGlobalChecksum(num_weapons);
        else
            getOwner().getWorld().updateGlobalChecksum(1000000);
        getBuildSupplyContainer(type).ifPresent(c -> ((BuildProductionContainer) c).orderSupply(num_weapons, infinite));
    }

    public boolean canBuildChieftain() {
        return !isDead() && chieftain_container != null && getOwner().canBuildChieftains() && !getOwner()
                .hasActiveChieftain() && !getOwner().isTrainingChieftain();
    }

    public boolean canStopChieftain() {
        return !isDead() && chieftain_container != null && chieftain_container.isTraining();
    }

    public void trainChieftain(boolean start) {
        if (canBuildChieftain() && start) {
            chieftain_container.startTraining();
            getOwner().setTrainingChieftain(true);
            is_training_chieftain = true;
        } else if (canStopChieftain() && !start) {
            chieftain_container.stopTraining();
            getOwner().setTrainingChieftain(false);
            is_training_chieftain = false;
        }
    }

    public void deployChieftain() {
        chieftain_container.stopTraining();
        getOwner().setTrainingChieftain(false);
        is_training_chieftain = false;
        Unit chieftain = createUnit(null, getOwner().getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
        getOwner().setActiveChieftain(chieftain);
    }

    private @NonNull Unit createUnit(@Nullable Target rally_point, @NonNull UnitTemplate template) {
        return new Unit(getOwner(), getPositionX(), getPositionY(), rally_point, template, null, true, true);
    }

    public void createArmy(int num_peon, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createArmy(num_peon, UnitType.PEON);
        createArmy(num_rock, UnitType.WARRIOR_ROCK);
        createArmy(num_iron, UnitType.WARRIOR_IRON);
        createArmy(num_rubber, UnitType.WARRIOR_RUBBER);
    }

    private void createArmy(int amount, @NonNull UnitType template) {
        RaceInfo raceInfo = getOwner().getRaceInfo();
        checkRallyPoint();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().ifPresent(c -> {
                c.prepareDeploy(-1);
                c.exit();
            });
            Unit unit = createUnit(hasRallyPoint() ? rally_point : null, raceInfo.getUnitTemplate(template));
            if (getAbilities().hasAbilities(Abilities.REPRODUCE) && !hasRallyPoint()) {
                unit.pushController(new TransferUnitController(unit));
            }
        }
    }

    public void createTransporters(int num_tree, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createTransporters(num_tree, SupplyType.WOOD);
        createTransporters(num_rock, SupplyType.ROCK);
        createTransporters(num_iron, SupplyType.IRON);
        createTransporters(num_rubber, SupplyType.RUBBER);
    }

    private void checkRallyPoint() {
        if (hasRallyPoint() && rally_point.isDead())
            rally_point = this;
    }

    private void createTransporters(int amount, SupplyType supply) {
        RaceInfo raceInfo = getOwner().getRaceInfo();
        checkRallyPoint();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().ifPresent(c -> {
                c.prepareDeploy(-1);
                c.exit();
            });
            Unit unit = createUnit(hasRallyPoint() ? rally_point : null, raceInfo.getUnitTemplate(UnitType.PEON));
            unit.getSupplyContainer().increaseSupply(unit.getSupplyContainer().getMaxSupplyCount(), supply);
        }

    }

    public boolean isDamaged() {
        assert !isDead();
        return hit_points > 0 && hit_points < getTemplate().getMaxHitPoints();
    }

    public int getHitPoints() {
        return hit_points;
    }

    private void adjustHitPoints(int amount) {
        setHitPoints(hit_points + amount);
    }

    private void setHitPoints(int new_hit_points) {
        hit_points = Math.clamp(new_hit_points, 0, Math.min(build_points, getTemplate().getMaxHitPoints()));
    }

    public void repair(int amount) {
        assert !isDead();
        assert isPlaced();
        if (!isDamaged())
            return;

        if (!isBuilt()) {
            var max_hitpoints = getTemplate().getMaxHitPoints();
            build_points = Math.min(build_points + amount, max_hitpoints);
            reinsert();
            if (build_points == max_hitpoints) {
                buildingCompleted();
            }
        }

        adjustHitPoints(amount);
    }

    public static boolean isPlacingLegal(@NonNull UnitGrid unit_grid, @NonNull BuildingTemplate template, int grid_x,
            int grid_y) {
        return doIsPlacingLegal(unit_grid, grid_x, grid_y, template.getPlacingSize());
    }

    public boolean isPlacingLegal() {
        return !isDead() && getOwner().canBuild(getTemplate().getBuildingType()) &&
                doIsPlacingLegal(getUnitGrid(), getGridX(), getGridY(), getTemplate().getPlacingSize()
                        - PLACING_BORDER);
    }

    /** {@return true if the building is placed, false otherwise} */
    public boolean isPlaced() {
        assert !isDead();
        return getBuildStage() != BuildStage.UNPLACED;
    }

    /** {@return true if construction is complete, false otherwise} */
    public boolean isBuilt() {
        return build_points == getTemplate().getMaxHitPoints();
    }

    public int getBuildPoints() {
        return build_points;
    }

    public @NonNull BuildStage getBuildStage() {
        var max_points = getTemplate().getMaxHitPoints();
        return build_points == max_points
                ? BuildStage.BUILT
                : (float) build_points / max_points > .5f
                        ? BuildStage.HALFBUILT
                : build_points > 0 ? BuildStage.START : BuildStage.UNPLACED;
    }

    private void buildingCompleted() {
        getOwner().getWorld().getNotificationListener().newSelectableNotification(this);
        getAbilities().addAbilities(getTemplate().getAbilities());
        supply_containers.put(Unit.class, getTemplate().getUnitContainerFactory().createContainer(this));
        if (getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
            resource_containers.put(SupplyType.WOOD, new SupplyContainer(MAX_SUPPLY_COUNT));
            resource_containers.put(SupplyType.ROCK, new SupplyContainer(MAX_SUPPLY_COUNT));
            resource_containers.put(SupplyType.IRON, new SupplyContainer(MAX_SUPPLY_COUNT));
            resource_containers.put(SupplyType.RUBBER, new SupplyContainer(MAX_SUPPLY_COUNT));

            SupplyContainer rock_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
            supply_containers.put(RockAxeWeapon.class, rock_weapon_container);
            supply_containers.put(RockSpearWeapon.class, rock_weapon_container);
            SupplyContainer iron_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
            supply_containers.put(IronAxeWeapon.class, iron_weapon_container);
            supply_containers.put(IronSpearWeapon.class, iron_weapon_container);
            SupplyContainer rubber_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
            supply_containers.put(RubberAxeWeapon.class, rubber_weapon_container);
            SupplyContainer rubber_spear_weapon = new SupplyContainer(MAX_SUPPLY_COUNT);
            supply_containers.put(RubberSpearWeapon.class, rubber_spear_weapon);

            BuildProductionContainer rock_axe_weapon = new BuildProductionContainer(BuildSpinner.INFINITE_LIMIT,
                    rock_weapon_container,
                    this,
                    COST_ROCK_WEAPON,
                    40f);
            BuildProductionContainer iron_axe_weapon = new BuildProductionContainer(BuildSpinner.INFINITE_LIMIT,
                    iron_weapon_container,
                    this,
                    COST_IRON_WEAPON,
                    80f);
            BuildProductionContainer rubber_axe_weapon = new BuildProductionContainer(
                    BuildSpinner.INFINITE_LIMIT,
                    rubber_weapon_container,
                    this,
                    COST_RUBBER_WEAPON,
                    120f);
            build_containers.put(RockAxeWeapon.class, rock_axe_weapon);
            build_containers.put(IronAxeWeapon.class, iron_axe_weapon);
            build_containers.put(RubberAxeWeapon.class, rubber_axe_weapon);
            BuildProductionContainer[] production_containers = new BuildProductionContainer[]{rock_axe_weapon,
                    iron_axe_weapon, rubber_axe_weapon};

            weapons_producer = new WeaponsProducer(this, (WorkerUnitContainer) getUnitContainer().orElseThrow(),
                    production_containers);

            deploy_containers.put(DeployType.ROCK_WARRIOR, new DeployContainer(this, 1f,
                    DeployType.ROCK_WARRIOR, RockAxeWeapon.class));
            deploy_containers.put(DeployType.IRON_WARRIOR, new DeployContainer(this, 1.5f,
                    DeployType.IRON_WARRIOR, IronAxeWeapon.class));
            deploy_containers.put(DeployType.RUBBER_WARRIOR, new DeployContainer(this, 2f,
                    DeployType.RUBBER_WARRIOR, RubberAxeWeapon.class));
            deploy_containers.put(DeployType.PEON, new DeployContainer(this, .5f, DeployType.PEON, null));
            deploy_containers.put(DeployType.PEON_HARVEST_TREE, new DeployContainer(this, .5f,
                    DeployType.PEON_HARVEST_TREE, null));
            deploy_containers.put(DeployType.PEON_TRANSPORT_TREE, new DeployContainer(this, .5f,
                    DeployType.PEON_TRANSPORT_TREE, TreeSupply.class));
            deploy_containers.put(DeployType.PEON_HARVEST_ROCK, new DeployContainer(this, .5f,
                    DeployType.PEON_HARVEST_ROCK, null));
            deploy_containers.put(DeployType.PEON_TRANSPORT_ROCK, new DeployContainer(this, .5f,
                    DeployType.PEON_TRANSPORT_ROCK, RockSupply.class));
            deploy_containers.put(DeployType.PEON_HARVEST_IRON, new DeployContainer(this, .5f,
                    DeployType.PEON_HARVEST_IRON, null));
            deploy_containers.put(DeployType.PEON_TRANSPORT_IRON, new DeployContainer(this, .5f,
                    DeployType.PEON_TRANSPORT_IRON, IronSupply.class));
            deploy_containers.put(DeployType.PEON_HARVEST_RUBBER, new DeployContainer(this, .5f,
                    DeployType.PEON_HARVEST_RUBBER, null));
            deploy_containers.put(DeployType.PEON_TRANSPORT_RUBBER, new DeployContainer(this, .5f,
                    DeployType.PEON_TRANSPORT_RUBBER, RubberSupply.class));
        } else if (getAbilities().hasAbilities(Abilities.REPRODUCE)) {
            chieftain_container = new ChieftainContainer(this);
            deploy_containers.put(DeployType.PEON, new DeployContainer(this, .5f, DeployType.PEON, null));
        }
    }

    @Override
    public float getHitOffsetZ() {
        int index = switch (getBuildStage()) {
            case START -> 0;
            case HALFBUILT -> 1;
            case UNPLACED, BUILT -> 2;
        };
        return getTemplate().getHitOffsetZ(index);
    }

    public static boolean doIsPlacingLegal(@NonNull UnitGrid unit_grid, int grid_x, int grid_y, int size) {
        if (!unit_grid.getHeightMap().canBuild(grid_x, grid_y, size))
            return false;

        for (int y = 0; y < size * 2 - 1; y++) {
            for (int x = 0; x < size * 2 - 1; x++) {
                int current_grid_x = grid_x + x - (size - 1);
                int current_grid_y = grid_y + y - (size - 1);
                if (current_grid_x >= unit_grid.getGridSize() || current_grid_y >= unit_grid.getGridSize() ||
                        current_grid_x < 0 || current_grid_y < 0 || unit_grid.isGridOccupied(current_grid_x,
                                current_grid_y))
                    return false;
            }
        }
        return true;
    }

    @Override
    public AttackScanFilter.@NonNull Priority getAttackPriority() {
        return getAbilities().hasAbilities(Abilities.ATTACK)
                ? AttackScanFilter.Priority.TOWER
                : getAbilities().hasAbilities(Abilities.BUILD_ARMIES)
                        ? AttackScanFilter.Priority.ARMORY
                : AttackScanFilter.Priority.QUARTERS;
    }

    @Override
    protected void setTarget(@NonNull Target target, @NonNull Action action, boolean aggressive) {
        if (getAbilities().hasAbilities(Abilities.ATTACK)) {
            if (target != this) {
                Unit unit = ((MountUnitContainer) getUnitContainer().orElseThrow()).getUnit();
                boolean kill_friendly = action == Action.ATTACK;
                if (unit != null && unit.canAttack(target, kill_friendly))
                    unit.pushController(new AttackController(unit, (Selectable<?>) target));
            }
        } else {
            setRallyPoint(target);
        }
    }

    public void place() {
        assert !isDead();
        assert isPlacingLegal();
        register();
        occupy();
        flattenLandscape();
        int result = getOwner().getBuildingCountContainer().increaseSupply(1);
        assert (result == 1) : "Too many buildings";
        build_points = 1;
        reinsert();
    }

    @Override
    public float getSize() {
        assert !isDead();
        float radius = (getTemplate().getPlacingSize() - 1);
        return (float) Math.sqrt(2) * radius + .1f;
    }

    @Override
    public int getPenalty() {
        assert !isDead();
        return Occupant.STATIC;
    }

    @Override
    protected void removeDying() {
        remove_delay = REMOVE_DELAY;
        getUnitContainer().ifPresent(c -> {
            while (c.getNumSupplies() > 0) {
                Unit unit = c.exit();
                if (unit != null)
                    unit.removeNow();
            }
        });
        getBuildSupplyContainer(Unit.class).ifPresent(worker_container -> {
            int result = getOwner().getUnitCountContainer().increaseSupply(-worker_container.getNumSupplies());
            assert result == -worker_container.getNumSupplies();
        });
        for (DeployContainer deploy_container : deploy_containers.values()) {
            int result = getOwner().getUnitCountContainer().increaseSupply(-deploy_container.getNumSupplies());
            assert result == -deploy_container.getNumSupplies();
        }
        free();
        undoLandscape();
        int result = getOwner().getBuildingCountContainer().increaseSupply(-1);
        assert result == -1;
        super.removeDying();
    }

    public boolean isValidRallyPoint(Target t) {
        return t instanceof Building b &&
                getOwner() == b.getOwner() &&
                b.getAbilities().hasAbilities(Abilities.RALLY_TO);
    }

    public void setRallyPoint(@NonNull Target target) {
        if (!getOwner().canSetRallyPoints())
            return;
        rally_point = isValidRallyPoint(target)
                ? target
                : getUnitGrid().findGridTargets(target.getGridX(), target.getGridY(), 1, false)[0];
    }

    @Override
    public float getShadowOpacity() {
        return switch (getBuildStage()) {
            case UNPLACED, START -> 0.0f;
            case HALFBUILT, BUILT -> super.getShadowOpacity();
        };
    }

    @Override
    protected @NonNull BoundingBox @NonNull [] getLocalBounds() {
        return switch (getBuildStage()) {
            case START -> getTemplate().getStartBounds();
            case HALFBUILT -> getTemplate().getHalfbuiltBounds();
            case UNPLACED, BUILT -> getTemplate().getBuiltBounds();
        };
    }

    private void flattenLandscape() {
        int size = getTemplate().getPlacingSize();
        int height_points = (size - PLACING_BORDER) * 2;
        int offset_x = getGridX() - (size - 1);
        int offset_y = getGridY() - (size - 1);
        float total_height = 0;
        old_landscape_heights = new float[height_points][height_points];
        for (int y = 0; y < height_points; y++) {
            for (int x = 0; x < height_points; x++) {
                float old_height = getOwner().getWorld().getHeightMap().getWrappedHeight(offset_x + x + PLACING_BORDER,
                        offset_y + y + PLACING_BORDER);
                old_landscape_heights[y][x] = old_height;
                total_height += old_height;
            }
        }

        float new_height = total_height / (height_points * height_points);
        for (int y = 0; y < height_points; y++) {
            for (int x = 0; x < height_points; x++) {
                getOwner().getWorld().getHeightMap().editHeight(offset_x + x + PLACING_BORDER, offset_y + y
                        + PLACING_BORDER, new_height);
            }
        }
    }

    private void undoLandscape() {
        int size = getTemplate().getPlacingSize();
        int offset_x = getGridX() - (size - 1);
        int offset_y = getGridY() - (size - 1);
        for (int y = 0; y < old_landscape_heights.length; y++) {
            for (int x = 0; x < old_landscape_heights[y].length; x++) {
                getOwner().getWorld().getHeightMap().editHeight(offset_x + x + PLACING_BORDER, offset_y + y
                        + PLACING_BORDER, old_landscape_heights[y][x]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void occupy() {
        UnitGrid grid = getUnitGrid();
        grid.getRegion(getGridX(), getGridY()).registerObject(Building.class, this);
        int size = getTemplate().getPlacingSize() * 2 - 1;
        for (int y = PLACING_BORDER; y < size - PLACING_BORDER; y++) {
            for (int x = PLACING_BORDER; x < size - PLACING_BORDER; x++) {
                grid.occupyGrid(getGridX() - size / 2 + x, getGridY() - size / 2 + y, this);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void free() {
        UnitGrid grid = getUnitGrid();
        grid.getRegion(getGridX(), getGridY()).unregisterObject(Building.class, this);
        int size = getTemplate().getPlacingSize() * 2 - 1;
        for (int y = PLACING_BORDER; y < size - PLACING_BORDER; y++) {
            for (int x = PLACING_BORDER; x < size - PLACING_BORDER; x++) {
                grid.freeGrid(getGridX() - size / 2 + x, getGridY() - size / 2 + y, this);
            }
        }
    }

    @Override
    public void hit(int damage, float dir_x, float dir_y, @NonNull Player owner) {
        super.hit(damage, dir_x, dir_y, owner);
        if (!isDead()) {
            adjustHitPoints(-damage);
            World world = getOwner().getWorld();
            world.getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(),
                    AudioAssets.BUILDING_HITS[ThreadLocalRandom.current()
                            .nextInt(AudioAssets.BUILDING_HITS.length)]);
            if (hit_points <= 0) {
                // stats
                getOwner().buildingLost();
                owner.buildingDestroyed();
                if (is_training_chieftain)
                    getOwner().setTrainingChieftain(false);
                removeDying();
            }
        }
    }

    @Override
    public @NonNull String toString() {
        return "Building: isDead() = " + isDead();
    }

    public void fillSupplies(@NonNull Class<?> key, int max) {
        getSupplyContainer(key).ifPresent(container -> {
            container.increaseSupply(Math.min(container.getMaxSupplyCount() - container.getNumSupplies(), max));
        });
    }

    public void removeSupplies(@NonNull Class<?> key) {
        getSupplyContainer(key).ifPresent(container -> {
            container.increaseSupply(-container.getNumSupplies());
        });
    }

    @Override
    public int getStatusValue() {
        return getAbilities().hasAbilities(Abilities.REPRODUCE)
                ? getUnitContainer().map(SupplyContainer::getNumSupplies).orElse(0)
                : getAbilities().hasAbilities(Abilities.BUILD_ARMIES)
                        ? getUnitContainer().map(SupplyContainer::getNumSupplies).orElse(0) +
                                getSupplyContainer(RockAxeWeapon.class).map(SupplyContainer::getNumSupplies).orElse(0) +
                                getSupplyContainer(IronAxeWeapon.class).map(SupplyContainer::getNumSupplies).orElse(0)
                                        * 3 +
                                getSupplyContainer(RubberAxeWeapon.class).map(SupplyContainer::getNumSupplies).orElse(0)
                                        * 8
                : 0;
    }

    public void printDebugInfo() {
        IO.println("-----------------------------------");
        if (getAbilities().hasAbilities(Abilities.REPRODUCE)) {
            IO.println("Units = " + getUnitContainer().map(SupplyContainer::getNumSupplies).orElse(0));
        } else if (getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
            IO.println("Units = " + getUnitContainer().map(SupplyContainer::getNumSupplies).orElse(0));
            IO.println("Tree = " + getSupplyContainer(TreeSupply.class).map(SupplyContainer::getNumSupplies).orElse(0));
            IO.println("Rock = " + getSupplyContainer(RockSupply.class).map(SupplyContainer::getNumSupplies).orElse(0));
            IO.println("Iron = " + getSupplyContainer(IronSupply.class).map(SupplyContainer::getNumSupplies).orElse(0));
            IO.println("Rubber = " + getSupplyContainer(RubberSupply.class).map(SupplyContainer::getNumSupplies).orElse(
                    0));
            IO.println("Rock Weapons = " + getSupplyContainer(RockAxeWeapon.class).map(SupplyContainer::getNumSupplies)
                    .orElse(0));
            IO.println("Iron Weapons = " + getSupplyContainer(IronAxeWeapon.class).map(SupplyContainer::getNumSupplies)
                    .orElse(0));
            IO.println("Rubber Weapons = " + getSupplyContainer(RubberAxeWeapon.class).map(
                    SupplyContainer::getNumSupplies).orElse(0));
        }
    }
}
