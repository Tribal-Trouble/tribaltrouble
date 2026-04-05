package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.model.behaviour.NullController;
import com.oddlabs.tt.model.behaviour.SailBehaviour;
import com.oddlabs.tt.model.behaviour.SailController;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.particle.LinearEmitter;
import com.oddlabs.tt.particle.RandomAccelerationEmitter;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.pathfinder.Movable;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.PathTracker;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Vector3f;
import com.oddlabs.util.Vector4f;

import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public final strictfp class Ship extends Building implements Movable {
    private static final float REMOVE_DELAY = 1f / 10f;

    public static final int RENDER_START = 0;
    public static final int RENDER_HALFBUILT = 1;
    public static final int RENDER_BUILT = 2;

    private static final int MAX_SUPPLY_COUNT = 200;
    private static final float OCCUPY_LENGTH_CELLS = 6f;
    private static final float OCCUPY_WIDTH_CELLS = 2f;

    public static final Cost COST_ROCK_WEAPON =
            new Cost(new Class[] {TreeSupply.class, RockSupply.class}, new int[] {2, 1});
    public static final Cost COST_IRON_WEAPON =
            new Cost(new Class[] {TreeSupply.class, IronSupply.class}, new int[] {2, 1});
    public static final Cost COST_RUBBER_WEAPON =
            new Cost(
                    new Class[] {
                        TreeSupply.class, RockSupply.class, IronSupply.class, RubberSupply.class
                    },
                    new int[] {2, 1, 1, 1});

    public static final int KEY_DEPLOY_ROCK_WARRIOR = 0;
    public static final int KEY_DEPLOY_IRON_WARRIOR = 1;
    public static final int KEY_DEPLOY_RUBBER_WARRIOR = 2;
    public static final int KEY_DEPLOY_PEON = 3;
    public static final int KEY_DEPLOY_PEON_HARVEST_TREE = 4;
    public static final int KEY_DEPLOY_PEON_TRANSPORT_TREE = 5;
    public static final int KEY_DEPLOY_PEON_HARVEST_ROCK = 6;
    public static final int KEY_DEPLOY_PEON_TRANSPORT_ROCK = 7;
    public static final int KEY_DEPLOY_PEON_HARVEST_IRON = 8;
    public static final int KEY_DEPLOY_PEON_TRANSPORT_IRON = 9;
    public static final int KEY_DEPLOY_PEON_HARVEST_RUBBER = 10;
    public static final int KEY_DEPLOY_PEON_TRANSPORT_RUBBER = 11;

    public static Class deployTypeToGatherSupply(int deploy_type) {
        switch (deploy_type) {
            case KEY_DEPLOY_PEON_HARVEST_TREE:
                return TreeSupply.class;
            case KEY_DEPLOY_PEON_HARVEST_ROCK:
                return RockSupply.class;
            case KEY_DEPLOY_PEON_HARVEST_IRON:
                return IronSupply.class;
            case KEY_DEPLOY_PEON_HARVEST_RUBBER:
                return RubberSupply.class;
            default:
                return null;
        }
    }

    private static final float DAMAGED_PARTICLE_ALPHA = 3f;

    private final Map supply_containers = new HashMap();
    private final ShipDeployContainer[] deploy_containers = new ShipDeployContainer[12];
    private final LinearEmitter damaged_emitter;
    private final LinearEmitter production_emitter;

    private ChieftainContainer chieftain_container = null;
    private float remove_delay = 0;
    private int hit_points = 1;
    private int build_points = 0;
    private float[][] old_landscape_heights;

    private Target rally_point = this;
    private boolean is_training_chieftain = false;

    private final PathTracker path_tracker;

    private final ShipHR ship_hr = new ShipHR();

    private float anim_time;

    public Ship(Player owner, BuildingTemplate template, int grid_x, int grid_y) {
        super(owner, template);
        setGridPosition(grid_x, grid_y);
        UnitGrid unit_grid = getUnitGrid();
        float x = UnitGrid.coordinateFromGrid(grid_x);
        float y = UnitGrid.coordinateFromGrid(grid_y);
        super.setPosition(x, y);
        setPositionZ(
                Math.max(
                                unit_grid.getHeightMap().getSeaLevelMeters(),
                                unit_grid.getHeightMap().getNearestHeight(x, y))
                        + getOffsetZ());

        setInitialShipDirection();

        pushController(new NullController(this));
        /*
           Vector3f position, float offset_z, float uv_angle,
           float emitter_radius, float emitter_height, float angle_bound, float angle_max_jump,
           int num_particles, float particles_per_second,
           Vector3f velocity, Vector3f acceleration,
           Vector4f color, Vector4f delta_color,
           Vector3f particle_radius, Vector3f growth_rate, int energy, float friction,
           int src_blend_func, int dst_blend_func,
           Texture texture
        */
        damaged_emitter =
                new RandomVelocityEmitter(
                        getOwner().getWorld(),
                        new Vector3f(
                                getPositionX(), getPositionY(), getPositionZ() + getHitOffsetZ()),
                        0f,
                        0f,
                        0.01f,
                        0.01f,
                        0.5f,
                        .7f,
                        -1,
                        4f,
                        new Vector3f(0f, 0f, 5f),
                        new Vector3f(0f, 0f, 0f),
                        new Vector4f(.6f, .6f, .6f, DAMAGED_PARTICLE_ALPHA),
                        new Vector4f(0f, 0f, 0f, 0f),
                        new Vector3f(1.5f, 1.5f, 1.5f),
                        new Vector3f(.6f, .6f, .6f),
                        1.5f,
                        .75f,
                        GL11.GL_SRC_ALPHA,
                        GL11.GL_ONE_MINUS_SRC_ALPHA,
                        owner.getWorld().getRacesResources().getDamageSmokeTextures(),
                        owner.getWorld().getAnimationManagerRealTime());
        damaged_emitter.stop();

        float xc = getPositionX() + getBuildingTemplate().getChimneyX();
        float yc = getPositionY() + getBuildingTemplate().getChimneyY();
        float zc = getPositionZ() + getBuildingTemplate().getChimneyZ();

        float energy = 4f;
        float alpha = .6f;
        production_emitter =
                new RandomAccelerationEmitter(
                        owner.getWorld(),
                        new Vector3f(xc, yc, zc),
                        0f,
                        0.01f,
                        0.01f,
                        1.5f,
                        0.1f,
                        -1,
                        6f,
                        new Vector3f(0f, 0f, 1.3f),
                        new Vector3f(0f, 0f, .25f),
                        .7f,
                        new Vector4f(.7f, .7f, .7f, alpha),
                        new Vector4f(0f, 0f, 0f, -alpha / energy),
                        new Vector3f(.3f, .3f, .3f),
                        new Vector3f(.5f, .5f, .5f),
                        energy,
                        1f,
                        GL11.GL_SRC_ALPHA,
                        GL11.GL_ONE_MINUS_SRC_ALPHA,
                        owner.getWorld().getRacesResources().getSmokeTextures(),
                        owner.getWorld().getAnimationManagerRealTime());
        production_emitter.stop();

        this.path_tracker = new PathTracker(getUnitGrid(), this, UnitGrid.SEA);
    }

    public final void setInitialShipDirection() {
        UnitGrid grid = getUnitGrid();
        int cx = getGridX();
        int x0 = cx - 8;
        int x1 = x0 + 16;
        int cy = getGridY();
        int y0 = cy - 8;
        int y1 = y0 + 16;
        int samples = 20;
        int best_gap = 0;
        double best_gap_dx = 0.0f;
        double best_gap_dy = 0.0f;
        double delta = Math.toRadians(360.0f / samples);
        for (int i = 0; i < samples; i++) {
            double angle = delta * i;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            int weight_a = 0;
            int weight_b = 0;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int h = grid.isWater(x, y) ? 0 : 1;
                    int dx = x - cx;
                    int dy = y - cy;
                    double cross = dx * sin - dy * cos;
                    if (cross > 0) {
                        weight_a += h;
                    } else {
                        weight_b += h;
                    }
                }
            }
            int gap = weight_b - weight_a;
            if (i == 0 || gap < best_gap) {
                best_gap = gap;
                best_gap_dx = cos;
                best_gap_dy = sin;
            }
        }
        setDirection((float) -best_gap_dy, (float) best_gap_dx);
    }

    public final float getOffsetZ() {
        return 0;
    }

    public final void visit(ElementVisitor visitor) {
        visitor.visitBuilding(this);
    }

    public final BuildingTemplate getBuildingTemplate() {
        return (BuildingTemplate) getTemplate();
    }

    public final boolean hasRallyPoint() {
        return rally_point != this;
    }

    public final Target getRallyPoint() {
        return rally_point;
    }

    protected final void doAnimate(float t) {
        if (!isDead()) {

            anim_time += t * 0.25f;

            UnitContainer unit_container = getUnitContainer();
            if (unit_container != null) unit_container.animate(t);

            int num_deploying = 0;
            for (int i = 0; i < deploy_containers.length; i++) {
                if (deploy_containers[i] != null && deploy_containers[i].getNumSupplies() > 0)
                    num_deploying++;
            }
            if (num_deploying > 0 && getLayer() == UnitGrid.LAND) {
                float amount = t / num_deploying;
                for (int i = 0; i < deploy_containers.length; i++) {
                    if (deploy_containers[i] != null && deploy_containers[i].getNumSupplies() > 0)
                        deploy_containers[i].deploy(amount);
                }
            }
        }

        if (remove_delay > 0) {
            remove_delay -= t;
            if (remove_delay <= 0) {
                remove();
                damaged_emitter.done();
                production_emitter.done();
                float energy = 3f;
                float fade_speed = 2.5f;

                new RandomVelocityEmitter(
                        getOwner().getWorld(),
                        new Vector3f(getPositionX(), getPositionY(), getPositionZ()),
                        0f,
                        getBuildingTemplate().getSmokeRadius(),
                        getBuildingTemplate().getSmokeHeight(),
                        0.05f,
                        (float) StrictMath.PI,
                        getBuildingTemplate().getNumFragments(),
                        getBuildingTemplate().getNumFragments(),
                        new Vector3f(0f, 0f, 5f),
                        new Vector3f(0f, 0f, -25f),
                        new Vector4f(1f, 1f, 1f, energy * fade_speed),
                        new Vector4f(0f, 0f, 0f, -fade_speed),
                        new Vector3f(1f, 1f, 1f),
                        new Vector3f(0f, 0f, 0f),
                        energy,
                        .75f,
                        getOwner().getWorld().getRacesResources().getWoodFragments(),
                        getOwner().getWorld().getAnimationManagerRealTime());
            }
        }
    }

    public final UnitContainer getUnitContainer() {
        assert !isDead();
        return (UnitContainer) supply_containers.get(Unit.class);
    }

    public final SupplyContainer getSupplyContainer(Class key) {
        assert !isDead();
        return (SupplyContainer) supply_containers.get(key);
    }

    public final DeployContainer getDeployContainer(int key) {
        assert !isDead();
        return deploy_containers[key];
    }

    public final ChieftainContainer getChieftainContainer() {
        assert !isDead();
        return chieftain_container;
    }

    public final boolean isEnabled() {
        return !isDead();
    }

    public final ShipHR getShipHR() {
        return ship_hr;
    }

    public final boolean canExitTower() {
        return false;
    }

    public final void exitTower() {}

    public final void deployUnits(int type, int num_units) {
        assert !isDead();
        getOwner().getWorld().updateGlobalChecksum(type);
        getOwner().getWorld().updateGlobalChecksum(num_units);
        getDeployContainer(type).orderSupply(num_units);
    }

    public final void createHarvesters(int num_tree, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createHarvesters(TreeSupply.class, num_tree);
        createHarvesters(RockSupply.class, num_rock);
        createHarvesters(IronSupply.class, num_iron);
        createHarvesters(RubberSupply.class, num_rubber);
    }

    private final void createHarvesters(Class supply_type, int amount) {
        Race race = getOwner().getRace();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().prepareDeploy(-1);
            getUnitContainer().exit();
            Unit unit = createUnit(null, race.getUnitTemplate(Race.UNIT_PEON));
            unit.pushController(new GatherController(unit, null, supply_type));
        }
    }

    public final boolean canBuildChieftain() {
        return !isDead()
                && chieftain_container != null
                && getOwner().canBuildChieftains()
                && !getOwner().hasActiveChieftain()
                && !getOwner().isTrainingChieftain();
    }

    public final boolean canStopChieftain() {
        return !isDead() && chieftain_container != null && chieftain_container.isTraining();
    }

    public final void trainChieftain(boolean start) {
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

    public final void deployChieftain() {
        chieftain_container.stopTraining();
        getOwner().setTrainingChieftain(false);
        is_training_chieftain = false;
        Unit chieftain =
                createUnit(null, getOwner().getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN));
        getOwner().setActiveChieftain(chieftain);
    }

    private final Unit createUnit(Target rally_point, UnitTemplate template) {
        Unit unit = ship_hr.exitUnit(template);
        if (unit != null && rally_point != null) {
            unit.setTarget(rally_point, Target.ACTION_MOVE, false);
        }
        return unit;
    }

    public final void createArmy(int num_peon, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createArmy(num_peon, Race.UNIT_PEON);
        createArmy(num_rock, Race.UNIT_WARRIOR_ROCK);
        createArmy(num_iron, Race.UNIT_WARRIOR_IRON);
        createArmy(num_rubber, Race.UNIT_WARRIOR_RUBBER);
    }

    private final void createArmy(int amount, int template) {
        Race race = getOwner().getRace();
        checkRallyPoint();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().prepareDeploy(-1);
            getUnitContainer().exit();
            Unit unit =
                    createUnit(
                            hasRallyPoint() ? rally_point : null, race.getUnitTemplate(template));
        }
    }

    public void createTransporters(int num_tree, int num_rock, int num_iron, int num_rubber) {
        assert !isDead();
        createTransporters(num_tree, TreeSupply.class);
        createTransporters(num_rock, RockSupply.class);
        createTransporters(num_iron, IronSupply.class);
        createTransporters(num_rubber, RubberSupply.class);
    }

    private final void checkRallyPoint() {
        if (hasRallyPoint() && rally_point.isDead()) rally_point = this;
    }

    private final void createTransporters(int amount, Class supply) {
        Race race = getOwner().getRace();
        checkRallyPoint();
        for (int i = 0; i < amount; i++) {
            getUnitContainer().prepareDeploy(-1);
            getUnitContainer().exit();
            Unit unit =
                    createUnit(
                            hasRallyPoint() ? rally_point : null,
                            race.getUnitTemplate(Race.UNIT_PEON));
            if (unit != null) {
                unit.getSupplyContainer()
                        .increaseSupply(unit.getSupplyContainer().getMaxSupplyCount(), supply);
            }
        }
    }

    public final boolean isDamaged() {
        assert !isDead();
        return hit_points > 0 && hit_points < getBuildingTemplate().getMaxHitPoints();
    }

    public final int getHitPoints() {
        return hit_points;
    }

    private final void setHitPoints(int new_hit_points) {
        final float MIN_ENERGY = 3f;
        final float MAX_ENERGY = 5f;
        final int START_SMOKE = getBuildingTemplate().getMaxHitPoints() / 2;
        hit_points =
                StrictMath.max(
                        StrictMath.min(new_hit_points, getBuildingTemplate().getMaxHitPoints()), 0);
        if (build_points == getBuildingTemplate().getMaxHitPoints() && hit_points < START_SMOKE) {
            float energy =
                    MIN_ENERGY
                            + ((1 - (float) hit_points / (START_SMOKE))
                                    * (MAX_ENERGY - MIN_ENERGY));
            damaged_emitter.start();
            damaged_emitter.setDeltaColor(
                    new Vector4f(0f, 0f, 0f, -DAMAGED_PARTICLE_ALPHA / energy));
            damaged_emitter.setEnergy(energy);
        } else damaged_emitter.stop();
    }

    public final void repair(int amount) {
        assert !isDead();
        assert isPlaced();
        if (!isDamaged()) return;

        setHitPoints(hit_points + amount);
        if (build_points < getBuildingTemplate().getMaxHitPoints()) {
            build_points =
                    StrictMath.min(build_points + amount, getBuildingTemplate().getMaxHitPoints());
            reinsert();
            if (build_points == getBuildingTemplate().getMaxHitPoints()) {
                getOwner().getWorld().getNotificationListener().newSelectableNotification(this);
                getAbilities().addAbilities(getTemplate().getAbilities());
                supply_containers.put(Unit.class, new ShipUnitContainer(this));
                if (getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
                    SupplyContainer tree_supply = new SupplyContainer(MAX_SUPPLY_COUNT);
                    SupplyContainer rock_supply = new SupplyContainer(MAX_SUPPLY_COUNT);
                    SupplyContainer iron_supply = new SupplyContainer(MAX_SUPPLY_COUNT);
                    SupplyContainer rubber_supply = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(TreeSupply.class, tree_supply);
                    supply_containers.put(RockSupply.class, rock_supply);
                    supply_containers.put(IronSupply.class, iron_supply);
                    supply_containers.put(RubberSupply.class, rubber_supply);

                    SupplyContainer rock_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(RockAxeWeapon.class, rock_weapon_container);
                    supply_containers.put(RockSpearWeapon.class, rock_weapon_container);
                    SupplyContainer iron_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(IronAxeWeapon.class, iron_weapon_container);
                    supply_containers.put(IronSpearWeapon.class, iron_weapon_container);
                    SupplyContainer rubber_weapon_container = new SupplyContainer(MAX_SUPPLY_COUNT);
                    supply_containers.put(RubberAxeWeapon.class, rubber_weapon_container);
                    supply_containers.put(RubberSpearWeapon.class, rubber_weapon_container);

                    ShipDeployContainer rock_warrior_container =
                            new ShipDeployContainer(
                                    this, 1f, KEY_DEPLOY_ROCK_WARRIOR, RockAxeWeapon.class, false);
                    ShipDeployContainer iron_warrior_container =
                            new ShipDeployContainer(
                                    this,
                                    1.5f,
                                    KEY_DEPLOY_IRON_WARRIOR,
                                    IronAxeWeapon.class,
                                    false);
                    ShipDeployContainer rubber_warrior_container =
                            new ShipDeployContainer(
                                    this,
                                    2f,
                                    KEY_DEPLOY_RUBBER_WARRIOR,
                                    RubberAxeWeapon.class,
                                    false);
                    ShipDeployContainer peon_container =
                            new ShipDeployContainer(this, .5f, KEY_DEPLOY_PEON, null, false);
                    ShipDeployContainer peon_harvest_tree_container =
                            new ShipDeployContainer(
                                    this, .5f, KEY_DEPLOY_PEON_HARVEST_TREE, null, false);
                    ShipDeployContainer peon_transport_tree_container =
                            new ShipDeployContainer(
                                    this,
                                    .5f,
                                    KEY_DEPLOY_PEON_TRANSPORT_TREE,
                                    TreeSupply.class,
                                    false);
                    ShipDeployContainer peon_harvest_rock_container =
                            new ShipDeployContainer(
                                    this, .5f, KEY_DEPLOY_PEON_HARVEST_ROCK, null, false);
                    ShipDeployContainer peon_transport_rock_container =
                            new ShipDeployContainer(
                                    this,
                                    .5f,
                                    KEY_DEPLOY_PEON_TRANSPORT_ROCK,
                                    RockSupply.class,
                                    false);
                    ShipDeployContainer peon_harvest_iron_container =
                            new ShipDeployContainer(
                                    this, .5f, KEY_DEPLOY_PEON_HARVEST_IRON, null, false);
                    ShipDeployContainer peon_transport_iron_container =
                            new ShipDeployContainer(
                                    this,
                                    .5f,
                                    KEY_DEPLOY_PEON_TRANSPORT_IRON,
                                    IronSupply.class,
                                    false);
                    ShipDeployContainer peon_harvest_rubber_container =
                            new ShipDeployContainer(
                                    this, .5f, KEY_DEPLOY_PEON_HARVEST_RUBBER, null, false);
                    ShipDeployContainer peon_transport_rubber_container =
                            new ShipDeployContainer(
                                    this,
                                    .5f,
                                    KEY_DEPLOY_PEON_TRANSPORT_RUBBER,
                                    RubberSupply.class,
                                    true);
                    deploy_containers[KEY_DEPLOY_ROCK_WARRIOR] = rock_warrior_container;
                    deploy_containers[KEY_DEPLOY_IRON_WARRIOR] = iron_warrior_container;
                    deploy_containers[KEY_DEPLOY_RUBBER_WARRIOR] = rubber_warrior_container;
                    deploy_containers[KEY_DEPLOY_PEON] = peon_container;
                    deploy_containers[KEY_DEPLOY_PEON_HARVEST_TREE] = peon_harvest_tree_container;
                    deploy_containers[KEY_DEPLOY_PEON_TRANSPORT_TREE] =
                            peon_transport_tree_container;
                    deploy_containers[KEY_DEPLOY_PEON_HARVEST_ROCK] = peon_harvest_rock_container;
                    deploy_containers[KEY_DEPLOY_PEON_TRANSPORT_ROCK] =
                            peon_transport_rock_container;
                    deploy_containers[KEY_DEPLOY_PEON_HARVEST_IRON] = peon_harvest_iron_container;
                    deploy_containers[KEY_DEPLOY_PEON_TRANSPORT_IRON] =
                            peon_transport_iron_container;
                    deploy_containers[KEY_DEPLOY_PEON_HARVEST_RUBBER] =
                            peon_harvest_rubber_container;
                    deploy_containers[KEY_DEPLOY_PEON_TRANSPORT_RUBBER] =
                            peon_transport_rubber_container;
                }
            }
        }
    }

    public static final boolean isPlacingLegal(
            UnitGrid unit_grid, BuildingTemplate template, int grid_x, int grid_y) {
        return doIsPlacingLegal(unit_grid, grid_x, grid_y, 1);
    }

    public final boolean isPlacingLegal() {
        return !isDead()
                && getOwner().canBuild(getBuildingTemplate().getTemplateID())
                && doIsPlacingLegal(getUnitGrid(), getGridX(), getGridY(), 1);
    }

    public final boolean isPlaced() {
        assert !isDead();
        return build_points != 0;
    }

    public final boolean isComplete() {
        return build_points == getBuildingTemplate().getMaxHitPoints();
    }

    public final float getHitOffsetZ() {
        return getTemplate().getHitOffsetZ(getRenderLevel());
    }

    public static final boolean doIsPlacingLegal(
            UnitGrid unit_grid, int grid_x, int grid_y, int size) {
        for (int y = 0; y < size * 2 - 1; y++)
            for (int x = 0; x < size * 2 - 1; x++) {
                int current_grid_x = grid_x + x - (size - 1);
                int current_grid_y = grid_y + y - (size - 1);
                if (current_grid_x >= unit_grid.getGridSize()
                        || current_grid_y >= unit_grid.getGridSize()
                        || current_grid_x < 0
                        || current_grid_y < 0) return false;
                boolean occupied =
                        unit_grid.isGridOccupied(current_grid_x, current_grid_y, UnitGrid.LAND);
                if (occupied) {
                    return false;
                }
            }
        return true;
    }

    public final int getAttackPriority() {
        if (getAbilities().hasAbilities(Abilities.ATTACK)) return AttackScanFilter.PRIORITY_TOWER;
        else return AttackScanFilter.PRIORITY_QUARTERS;
    }

    protected final void setTarget(Target target, int action, boolean aggressive) {
        forceDecide();
        clearControllerStack();
        pushController(new SailController(this, target));
        free();
        setLayer(UnitGrid.SEA);
        occupy();
    }

    public final void place() {
        assert !isDead();
        assert isPlacingLegal();
        register();
        occupy();
        int result = getOwner().getBuildingCountContainer().increaseSupply(1);
        assert (result == 1) : "Too many buildings";
        build_points = 1;
        reinsert();
    }

    public final float getSize() {
        assert !isDead();
        float radius = (getBuildingTemplate().getPlacingSize() - 1);
        return (float) StrictMath.sqrt(2) * radius + .1f;
    }

    public final int getPenalty() {
        assert !isDead();
        return Occupant.STATIC;
    }

    protected final void removeDying() {

        // If it's a ship and it's destroyed, kill everyone on board
        ship_hr.killCrew();

        new RandomVelocityEmitter(
                getOwner().getWorld(),
                new Vector3f(getPositionX(), getPositionY(), getPositionZ()),
                0f,
                0f,
                getBuildingTemplate().getSmokeRadius(),
                getBuildingTemplate().getSmokeHeight(),
                1f,
                1f,
                30,
                400f,
                new Vector3f(0f, 0f, .1f),
                new Vector3f(0f, 0f, -2.5f),
                new Vector4f(1f, .8f, .6f, 1f),
                new Vector4f(0f, 0f, 0f, -1f),
                new Vector3f(1f, 1f, 1f),
                new Vector3f(7.5f, 7.5f, 7.5f),
                1,
                0.75f,
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                getOwner().getWorld().getRacesResources().getSmokeTextures(),
                getOwner().getWorld().getAnimationManagerRealTime());

        remove_delay = REMOVE_DELAY;
        getOwner()
                .getWorld()
                .getAudio()
                .newAudio(
                        new AudioParameters(
                                getOwner()
                                        .getWorld()
                                        .getRacesResources()
                                        .getBuildingCollapseSound(),
                                getPositionX(),
                                getPositionY(),
                                getPositionZ(),
                                AudioPlayer.AUDIO_RANK_BUILDING_COLLAPSE,
                                AudioPlayer.AUDIO_DISTANCE_BUILDING_COLLAPSE,
                                AudioPlayer.AUDIO_GAIN_BUILDING_COLLAPSE,
                                AudioPlayer.AUDIO_RADIUS_BUILDING_COLLAPSE));
        if (getUnitContainer() != null) {
            while (getUnitContainer().getNumSupplies() > 0) {
                Unit unit = getUnitContainer().exit();
                if (unit != null) unit.removeNow();
            }
        }
        for (int i = 0; i < deploy_containers.length; i++) {
            if (deploy_containers[i] != null) {
                int result =
                        getOwner()
                                .getUnitCountContainer()
                                .increaseSupply(-deploy_containers[i].getNumSupplies());
                assert result == -deploy_containers[i].getNumSupplies();
            }
        }
        free();
        int result = getOwner().getBuildingCountContainer().increaseSupply(-1);
        assert result == -1;
        super.removeDying();
    }

    public final boolean isValidRallyPoint(Target t) {
        if (!(t instanceof Building || t instanceof Ship)) return false;
        Building b = (Building) t;
        if (b != null) {
            return getOwner() == b.getOwner() && b.getAbilities().hasAbilities(Abilities.RALLY_TO);
        }
        Ship s = (Ship) t;
        return getOwner() == s.getOwner() && s.getAbilities().hasAbilities(Abilities.RALLY_TO);
    }

    public final void setRallyPoint(Target target) {
        if (!getOwner().canSetRallyPoints()) return;
        if (isValidRallyPoint(target)) {
            rally_point = target;
        } else {
            rally_point =
                    getUnitGrid()
                            .findGridTargets(
                                    target.getGridX(), target.getGridY(), 1, false, UnitGrid.LAND)[
                            0];
        }
    }

    public final int getRenderLevel() {
        if (build_points == getBuildingTemplate().getMaxHitPoints()) return RENDER_BUILT;
        else if ((float) build_points / getBuildingTemplate().getMaxHitPoints() < .5)
            return RENDER_START;
        else return RENDER_HALFBUILT;
    }

    public final SpriteKey getSpriteRenderer() {
        int render_level = getRenderLevel();
        switch (render_level) {
            case RENDER_START:
                return getBuildingTemplate().getStartRenderer();
            case RENDER_HALFBUILT:
                return getBuildingTemplate().getHalfbuiltRenderer();
            case RENDER_BUILT:
                return getBuildingTemplate().getBuiltRenderer();
            default:
                throw new RuntimeException();
        }
    }

    public final void visit(ToolTipVisitor visitor) {
        visitor.visitBuilding(this);
    }

    private boolean isInsideOrientedFootprint(
            int grid_x,
            int grid_y,
            float center_x,
            float center_y,
            float dir_x,
            float dir_y,
            float half_length_meters,
            float half_width_meters) {
        float cell_x = UnitGrid.coordinateFromGrid(grid_x);
        float cell_y = UnitGrid.coordinateFromGrid(grid_y);
        float rel_x = cell_x - center_x;
        float rel_y = cell_y - center_y;

        float along = rel_x * dir_x + rel_y * dir_y;
        float side = rel_x * (-dir_y) + rel_y * dir_x;

        return StrictMath.abs(along) <= half_length_meters
                && StrictMath.abs(side) <= half_width_meters;
    }

    public final void occupy() {
        UnitGrid grid = getUnitGrid();

        float center_x = getPositionX();
        float center_y = getPositionY();
        float dir_x = getDirectionX();
        float dir_y = getDirectionY();
        float dir_len = (float) StrictMath.sqrt(dir_x * dir_x + dir_y * dir_y);
        if (dir_len > 0.0001f) {
            dir_x /= dir_len;
            dir_y /= dir_len;
        } else {
            dir_x = 1.0f;
            dir_y = 0.0f;
        }

        float half_length_meters = OCCUPY_LENGTH_CELLS * HeightMap.METERS_PER_UNIT_GRID * 0.5f;
        float half_width_meters = OCCUPY_WIDTH_CELLS * HeightMap.METERS_PER_UNIT_GRID * 0.5f;
        float half_diagonal_meters =
                (float)
                        StrictMath.sqrt(
                                half_length_meters * half_length_meters
                                        + half_width_meters * half_width_meters);
        int radius_cells =
                (int) StrictMath.ceil(half_diagonal_meters / HeightMap.METERS_PER_UNIT_GRID) + 1;
        int grid_size = grid.getGridSize();
        int start_x = StrictMath.max(0, getGridX() - radius_cells);
        int end_x = StrictMath.min(grid_size - 1, getGridX() + radius_cells);
        int start_y = StrictMath.max(0, getGridY() - radius_cells);
        int end_y = StrictMath.min(grid_size - 1, getGridY() + radius_cells);

        boolean found_land = false;

        for (int y = start_y; y <= end_y && !found_land; y++) {
            for (int x = start_x; x <= end_x && !found_land; x++) {
                if (isInsideOrientedFootprint(
                        x,
                        y,
                        center_x,
                        center_y,
                        dir_x,
                        dir_y,
                        half_length_meters,
                        half_width_meters)) {
                    Region region = grid.getRegion(x, y, UnitGrid.LAND);
                    if (region != null) {
                        found_land = true;
                        setLayer(UnitGrid.LAND);
                        region.registerObject(Building.class, this);
                    }
                }
            }
        }

        if (!found_land) {
            Region region = grid.getRegion(getGridX(), getGridY(), UnitGrid.SEA);
            if (region != null) {
                setLayer(UnitGrid.SEA);
                region.registerObject(Building.class, this);
            }
        }

        for (int y = start_y; y <= end_y; y++) {
            for (int x = start_x; x <= end_x; x++) {
                if (isInsideOrientedFootprint(
                        x,
                        y,
                        center_x,
                        center_y,
                        dir_x,
                        dir_y,
                        half_length_meters,
                        half_width_meters)) {
                    grid.occupyGrid(x, y, this, getLayer());
                }
            }
        }
    }

    public final void free() {
        UnitGrid grid = getUnitGrid();
        Region region = grid.getRegion(getGridX(), getGridY(), getLayer());
        if (region != null) {
            region.unregisterObject(Building.class, this);
        }

        float center_x = getPositionX();
        float center_y = getPositionY();
        float dir_x = getDirectionX();
        float dir_y = getDirectionY();
        float dir_len = (float) StrictMath.sqrt(dir_x * dir_x + dir_y * dir_y);
        if (dir_len > 0.0001f) {
            dir_x /= dir_len;
            dir_y /= dir_len;
        } else {
            dir_x = 1.0f;
            dir_y = 0.0f;
        }

        float half_length_meters = OCCUPY_LENGTH_CELLS * HeightMap.METERS_PER_UNIT_GRID * 0.5f;
        float half_width_meters = OCCUPY_WIDTH_CELLS * HeightMap.METERS_PER_UNIT_GRID * 0.5f;
        float half_diagonal_meters =
                (float)
                        StrictMath.sqrt(
                                half_length_meters * half_length_meters
                                        + half_width_meters * half_width_meters);
        int radius_cells =
                (int) StrictMath.ceil(half_diagonal_meters / HeightMap.METERS_PER_UNIT_GRID) + 1;
        int grid_size = grid.getGridSize();
        int start_x = StrictMath.max(0, getGridX() - radius_cells);
        int end_x = StrictMath.min(grid_size - 1, getGridX() + radius_cells);
        int start_y = StrictMath.max(0, getGridY() - radius_cells);
        int end_y = StrictMath.min(grid_size - 1, getGridY() + radius_cells);

        for (int y = start_y; y <= end_y; y++) {
            for (int x = start_x; x <= end_x; x++) {
                if (isInsideOrientedFootprint(
                        x,
                        y,
                        center_x,
                        center_y,
                        dir_x,
                        dir_y,
                        half_length_meters,
                        half_width_meters)) {
                    grid.freeGrid(x, y, this, getLayer());
                }
            }
        }
    }

    public final void hit(int damage, float dir_x, float dir_y, Player owner) {
        super.hit(damage, dir_x, dir_y, owner);
        if (!isDead()) {
            setHitPoints(hit_points - damage);
            World world = getOwner().getWorld();
            world.getAudio()
                    .newAudio(
                            new AudioParameters(
                                    world.getRacesResources()
                                            .getBuildingHitSound(world.getRandom()),
                                    getPositionX(),
                                    getPositionY(),
                                    getPositionZ(),
                                    AudioPlayer.AUDIO_RANK_WEAPON_HIT,
                                    AudioPlayer.AUDIO_DISTANCE_WEAPON_HIT,
                                    AudioPlayer.AUDIO_GAIN_WEAPON_HIT,
                                    AudioPlayer.AUDIO_RADIUS_WEAPON_HIT));
            if (hit_points == 0) {
                // stats
                getOwner().buildingLost();
                owner.buildingDestroyed();
                if (is_training_chieftain) getOwner().setTrainingChieftain(false);
                removeDying();
            }
        }
    }

    public final String toString() {
        return "Ship: isDead() = " + isDead();
    }

    public final float getAnimationTicks() {
        return anim_time;
    }

    public final int getAnimation() {
        return 0;
    }

    public final void fillSupplies(Class key, int max) {
        SupplyContainer container = getSupplyContainer(key);
        if (container != null) {
            container.increaseSupply(
                    (int)
                            StrictMath.min(
                                    container.getMaxSupplyCount() - container.getNumSupplies(),
                                    max));
        }
    }

    public final void removeSupplies(Class key) {
        SupplyContainer container = getSupplyContainer(key);
        if (container != null) {
            container.increaseSupply(-container.getNumSupplies());
        }
    }

    public final int getStatusValue() {
        return 0;
    }

    public final void printDebugInfo() {
        System.out.println("-----------------------------------");
        System.out.println("Units = " + getUnitContainer().getNumSupplies());
        System.out.println("Units = " + getUnitContainer().getNumSupplies());
        System.out.println("Tree = " + getSupplyContainer(TreeSupply.class).getNumSupplies());
        System.out.println("Rock = " + getSupplyContainer(RockSupply.class).getNumSupplies());
        System.out.println("Iron = " + getSupplyContainer(IronSupply.class).getNumSupplies());
        System.out.println("Rubber = " + getSupplyContainer(RubberSupply.class).getNumSupplies());
        System.out.println(
                "Rock Weapons = " + getSupplyContainer(RockAxeWeapon.class).getNumSupplies());
        System.out.println(
                "Iron Weapons = " + getSupplyContainer(IronAxeWeapon.class).getNumSupplies());
        System.out.println(
                "Rubber Weapons = " + getSupplyContainer(RubberAxeWeapon.class).getNumSupplies());
    }

    public final float getHealth() {
        return build_points / (float) getBuildingTemplate().getMaxHitPoints();
    }

    public final boolean isMoving() {
        return (getCurrentBehaviour() instanceof SailBehaviour);
    }

    public final PathTracker getTracker() {
        return path_tracker;
    }

    public final void endTrip() {
        forceDecide();
        clearControllerStack();
        if (getUnitGrid().getRegion(getGridX(), getGridY(), UnitGrid.LAND) != null) {
            free();
            setLayer(UnitGrid.LAND);
            updateBounds();
            reregister();
            occupy();
        }
    }

    public final void setPosition(float x, float y) {
        if (isDead()) return;
        super.setPosition(x, y);
        float xc = x + getBuildingTemplate().getChimneyX();
        float yc = y + getBuildingTemplate().getChimneyY();
        float zc = getPositionZ() + getBuildingTemplate().getChimneyZ();
        production_emitter.setPosition(new Vector3f(xc, yc, zc));
    }

    public final void setPosition(int x, int y) {
        if (isDead()) return;
        free();
        super.setPosition(x, y);
        updateBounds();
        reregister();
        occupy();
    }

    public final void markBlocking() {}

    public final BuildSupplyContainer getBuildSupplyContainer(Class key) {
        return null;
    }

    public final void buildWeapons(Class type, int num_weapons, boolean infinite) {}
}
