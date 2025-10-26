package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
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
import com.oddlabs.tt.particle.LinearEmitter;
import com.oddlabs.tt.particle.RandomAccelerationEmitter;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class Building extends Selectable implements Occupant {
	private final static float REMOVE_DELAY = 1f/10f;

    public enum BuildState {
        START, HALFBUILT, BUILT
    }

    private final static int PLACING_BORDER = 1;
	private final static int MAX_SUPPLY_COUNT = 200;

	@SuppressWarnings({"unchecked"})
	public final static Cost COST_ROCK_WEAPON = new Cost(new Class[]{TreeSupply.class, RockSupply.class}, new int[]{2, 1});
	@SuppressWarnings({"unchecked"})
	public final static Cost COST_IRON_WEAPON = new Cost(new Class[]{TreeSupply.class, IronSupply.class}, new int[]{2, 1});
	@SuppressWarnings({"unchecked"})
	public final static Cost COST_RUBBER_WEAPON = new Cost(new Class[]{TreeSupply.class, RockSupply.class, IronSupply.class, RubberSupply.class}, new int[]{2, 1, 1, 1});

	private final static float DAMAGED_PARTICLE_ALPHA = 3f;

	private final Map<Class<?>, SupplyContainer> supply_containers = new HashMap<>();
	private final Map<Class<?>, BuildProductionContainer> build_containers = new HashMap<>();
	private final Map<DeployType, DeployContainer> deploy_containers = new EnumMap<>(DeployType.class);
	private final @NonNull LinearEmitter damaged_emitter;
	private final @NonNull LinearEmitter production_emitter;

	private @Nullable ChieftainContainer chieftain_container = null;
	private @Nullable WeaponsProducer weapons_producer = null;
	private float remove_delay = 0;
	private int hit_points = 1;
	private int build_points = 0;
	private float[][] old_landscape_heights;

	private Target rally_point = this;
	private boolean is_training_chieftain = false;

	public Building(@NonNull Player owner, BuildingTemplate template, int grid_x, int grid_y) {
		super(owner, template);
		setGridPosition(grid_x, grid_y);
		UnitGrid unit_grid = getUnitGrid();
		float x = UnitGrid.coordinateFromGrid(grid_x);
		float y = UnitGrid.coordinateFromGrid(grid_y);
		setPosition(x, y);
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
		damaged_emitter = new RandomVelocityEmitter(getOwner().getWorld(), new Vector3f(getPositionX(), getPositionY(), getPositionZ() + getHitOffsetZ()), 0f, 0f,
				0.01f, 0.01f, 0.5f, .7f,
				-1, 4f,
				new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, 0f),
				new Vector4f(.6f, .6f, .6f, DAMAGED_PARTICLE_ALPHA), new Vector4f(0f, 0f, 0f, 0f),
				new Vector3f(1.5f, 1.5f, 1.5f), new Vector3f(.6f, .6f, .6f), 1.5f, .75f,
				GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
				owner.getWorld().getRacesResources().getDamageSmokeTextures(),
				owner.getWorld().getAnimationManagerRealTime());
		damaged_emitter.stop();

		float xc = getPositionX() + getBuildingTemplate().getChimneyX();
		float yc = getPositionY() + getBuildingTemplate().getChimneyY();
		float zc = getPositionZ() + getBuildingTemplate().getChimneyZ();

		float energy = 4f;
		float alpha = .6f;
		production_emitter = new RandomAccelerationEmitter(owner.getWorld(), new Vector3f(xc, yc, zc), 0f,
				0.01f, 0.01f, 1.5f, 0.1f,
				-1, 6f,
				new Vector3f(0f, 0f, 1.3f), new Vector3f(0f, 0f, .25f), .7f,
				new Vector4f(.7f, .7f, .7f, alpha), new Vector4f(0f, 0f, 0f, -alpha/energy),
				new Vector3f(.3f, .3f, .3f), new Vector3f(.5f, .5f, .5f), energy, 1f,
				GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
				owner.getWorld().getRacesResources().getSmokeTextures(),
				owner.getWorld().getAnimationManagerRealTime());
		production_emitter.stop();
	}

        @Override
	public float getOffsetZ() {
		return 0;
	}

        @Override
	public void visit(@NonNull ElementVisitor visitor) {
		visitor.visitBuilding(this);
	}

	public BuildingTemplate getBuildingTemplate() {
		return (BuildingTemplate)getTemplate();
	}

	public boolean hasRallyPoint() {
		return rally_point != this;
	}

	public Target getRallyPoint() {
		return rally_point;
	}

        @Override
	protected void doAnimate(float t) {
		if (!isDead()) {
			UnitContainer unit_container = getUnitContainer();
			if (unit_container != null)
				unit_container.animate(t);
			if (weapons_producer != null)
				weapons_producer.animate(t);

			int num_deploying = 0;
                    for (DeployContainer deploy_container : deploy_containers.values()) {
                        if (deploy_container.getNumSupplies() > 0) {
                            num_deploying++;
                        }
                    }
			if (num_deploying > 0) {
				float amount = t/num_deploying;
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
				damaged_emitter.done();
				production_emitter.done();
				if (weapons_producer != null)
					weapons_producer.stopSound();
				float energy = 3f;
				float fade_speed = 2.5f;

				new RandomVelocityEmitter(getOwner().getWorld(), new Vector3f(getPositionX(), getPositionY(), getPositionZ()), 0f,
							getBuildingTemplate().getSmokeRadius(), getBuildingTemplate().getSmokeHeight(), 0.05f, (float)Math.PI,
							getBuildingTemplate().getNumFragments(), getBuildingTemplate().getNumFragments(),
							new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, -25f),
							new Vector4f(1f, 1f, 1f, energy*fade_speed), new Vector4f(0f, 0f, 0f, -fade_speed),
							new Vector3f(1f, 1f, 1f), new Vector3f(0f, 0f, 0f), energy, .75f,
							getOwner().getWorld().getRacesResources().getWoodFragments(),
							getOwner().getWorld().getAnimationManagerRealTime());
			}
		}
	}

	public UnitContainer getUnitContainer() {
		assert !isDead();
		return (UnitContainer)supply_containers.get(Unit.class);
	}

	public SupplyContainer getSupplyContainer(Class<?> key) {
		assert !isDead();
		return supply_containers.get(key);
	}

	public BuildSupplyContainer getBuildSupplyContainer(Class<?> key) {
		assert !isDead();
		return build_containers.get(key);
	}

	public DeployContainer getDeployContainer(DeployType type) {
		assert !isDead();
		return deploy_containers.get(type);
	}

	public ChieftainContainer getChieftainContainer() {
		assert !isDead();
		return chieftain_container;
	}

    @Override
	public boolean isEnabled() {
		return !isDead();
	}

	public int getUnitCount() {
		assert !isDead();
		return getUnitContainer().getNumSupplies();
	}

	public boolean canExitTower() {
		return !isDead() && getAbilities().hasAbilities(Abilities.ATTACK) && getUnitContainer().getNumSupplies() > 0 && getOwner().canExitTowers() &&
			!(((MountUnitContainer)getUnitContainer()).getUnit().getCurrentController() instanceof StunController);
	}

	public void exitTower() {
		assert !isDead();
		UnitContainer container = getUnitContainer();
		if (canExitTower()) {
//			Army selection = Selection.singleton.getCurrentSelection();
			Unit unit = container.exit();
/*			if (getOwner().isControllable()) {
				selection.clear();
				selection.add(unit);
			}*/
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
		createHarvesters(TreeSupply.class, num_tree);
		createHarvesters(RockSupply.class, num_rock);
		createHarvesters(IronSupply.class, num_iron);
		createHarvesters(RubberSupply.class, num_rubber);
	}

	private void createHarvesters(Class<? extends Supply> supply_type, int amount) {
		Race race = getOwner().getRace();
		for (int i = 0; i < amount; i++) {
			getUnitContainer().prepareDeploy(-1);
			getUnitContainer().exit();
			Unit unit = createUnit(null, race.getUnitTemplate(Race.UNIT_PEON));
			unit.pushController(new GatherController<>(unit, null, supply_type));
		}
	}

	public void buildWeapons(Class<? extends ThrowingWeapon> type, int num_weapons, boolean infinite) {
		assert !isDead();
		if (infinite)
			getOwner().getWorld().updateGlobalChecksum(num_weapons);
		else
			getOwner().getWorld().updateGlobalChecksum(1000000);
		((BuildProductionContainer)getBuildSupplyContainer(type)).orderSupply(num_weapons, infinite);
	}

	public boolean canBuildChieftain() {
		return !isDead() && chieftain_container != null && getOwner().canBuildChieftains() && !getOwner().hasActiveChieftain() && !getOwner().isTrainingChieftain();
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
		Unit chieftain = createUnit( null, getOwner().getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN));
		getOwner().setActiveChieftain(chieftain);
	}

	private @NonNull Unit createUnit(Target rally_point, @NonNull UnitTemplate template) {
		return new Unit(getOwner(), getPositionX(), getPositionY(), rally_point, template, null, true, true);
	}

	public void createArmy(int num_peon, int num_rock, int num_iron, int num_rubber) {
		assert !isDead();
		createArmy(num_peon, Race.UNIT_PEON);
		createArmy(num_rock, Race.UNIT_WARRIOR_ROCK);
		createArmy(num_iron, Race.UNIT_WARRIOR_IRON);
		createArmy(num_rubber, Race.UNIT_WARRIOR_RUBBER);
	}

	private void createArmy(int amount, int template) {
		Race race = getOwner().getRace();
		checkRallyPoint();
		for (int i = 0; i < amount; i++) {
			getUnitContainer().prepareDeploy(-1);
			getUnitContainer().exit();
			Unit unit = createUnit(hasRallyPoint() ? rally_point : null, race.getUnitTemplate(template));
			if (getAbilities().hasAbilities(Abilities.REPRODUCE) && !hasRallyPoint()) {
				unit.pushController(new TransferUnitController(unit));
			}
		}
	}

	public void createTransporters(int num_tree, int num_rock, int num_iron, int num_rubber) {
		assert !isDead();
		createTransporters(num_tree, TreeSupply.class);
		createTransporters(num_rock, RockSupply.class);
		createTransporters(num_iron, IronSupply.class);
		createTransporters(num_rubber, RubberSupply.class);
	}

	private void checkRallyPoint() {
		if (hasRallyPoint() && rally_point.isDead())
			rally_point = this;
	}

	private void createTransporters(int amount, Class<? extends Supply> supply) {
		Race race = getOwner().getRace();
		checkRallyPoint();
		for (int i = 0; i < amount; i++) {
			getUnitContainer().prepareDeploy(-1);
			getUnitContainer().exit();
			Unit unit = createUnit(hasRallyPoint() ? rally_point : null, race.getUnitTemplate(Race.UNIT_PEON));
			unit.getSupplyContainer().increaseSupply(unit.getSupplyContainer().getMaxSupplyCount(), supply);
		//	getSupplyContainer(supply).increaseSupply(-unit.getSupplyContainer().getMaxSupplyCount());
		//	getSupplyContainer(supply).prepareDeploy(-unit.getSupplyContainer().getMaxSupplyCount());
		}

	}

	public boolean isDamaged() {
		assert !isDead();
		return hit_points > 0 && hit_points < getBuildingTemplate().getMaxHitPoints();
	}

	public int getHitPoints() {
		return hit_points;
	}

	private void setHitPoints(int new_hit_points) {
		final float MIN_ENERGY = 3f;
		final float MAX_ENERGY = 5f;
		final int START_SMOKE = getBuildingTemplate().getMaxHitPoints()/2;
		hit_points = Math.max(Math.min(new_hit_points, getBuildingTemplate().getMaxHitPoints()), 0);
		if (build_points == getBuildingTemplate().getMaxHitPoints() && hit_points < START_SMOKE) {
			float energy = MIN_ENERGY + ((1 - (float)hit_points/(START_SMOKE))*(MAX_ENERGY - MIN_ENERGY));
			damaged_emitter.start();
			damaged_emitter.setDeltaColor(new Vector4f(0f, 0f, 0f, -DAMAGED_PARTICLE_ALPHA/energy));
			damaged_emitter.setEnergy(energy);
		} else
			damaged_emitter.stop();
	}

	public void repair(int amount) {
		assert !isDead();
		assert isPlaced();
		if (!isDamaged())
			return;

		setHitPoints(hit_points + amount);
		if (build_points < getBuildingTemplate().getMaxHitPoints()) {
			build_points = Math.min(build_points + amount, getBuildingTemplate().getMaxHitPoints());
			reinsert();
			if (build_points == getBuildingTemplate().getMaxHitPoints()) {
				getOwner().getWorld().getNotificationListener().newSelectableNotification(this);
				getAbilities().addAbilities(getTemplate().getAbilities());
				supply_containers.put(Unit.class, getBuildingTemplate().getUnitContainerFactory().createContainer(this));
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
					BuildProductionContainer rubber_axe_weapon = new BuildProductionContainer(BuildSpinner.INFINITE_LIMIT,
							rubber_weapon_container,
							this,
							COST_RUBBER_WEAPON,
							120f);
					build_containers.put(RockAxeWeapon.class, rock_axe_weapon);
					build_containers.put(IronAxeWeapon.class, iron_axe_weapon);
					build_containers.put(RubberAxeWeapon.class, rubber_axe_weapon);
					BuildProductionContainer[] production_containers = new BuildProductionContainer[]{rock_axe_weapon, iron_axe_weapon, rubber_axe_weapon};

					weapons_producer = new WeaponsProducer(this, (WorkerUnitContainer)getUnitContainer(), production_containers, production_emitter);

					deploy_containers.put(DeployType.ROCK_WARRIOR, new DeployContainer(this, 1f, DeployType.ROCK_WARRIOR, RockAxeWeapon.class));
					deploy_containers.put(DeployType.IRON_WARRIOR, new DeployContainer(this, 1.5f, DeployType.IRON_WARRIOR, IronAxeWeapon.class));
					deploy_containers.put(DeployType.RUBBER_WARRIOR, new DeployContainer(this, 2f, DeployType.RUBBER_WARRIOR, RubberAxeWeapon.class));
					deploy_containers.put(DeployType.PEON, new DeployContainer(this, .5f, DeployType.PEON, null));
					deploy_containers.put(DeployType.PEON_HARVEST_TREE, new DeployContainer(this, .5f, DeployType.PEON_HARVEST_TREE, null));
					deploy_containers.put(DeployType.PEON_TRANSPORT_TREE, new DeployContainer(this, .5f, DeployType.PEON_TRANSPORT_TREE, TreeSupply.class));
					deploy_containers.put(DeployType.PEON_HARVEST_ROCK, new DeployContainer(this, .5f, DeployType.PEON_HARVEST_ROCK, null));
					deploy_containers.put(DeployType.PEON_TRANSPORT_ROCK, new DeployContainer(this, .5f, DeployType.PEON_TRANSPORT_ROCK, RockSupply.class));
					deploy_containers.put(DeployType.PEON_HARVEST_IRON, new DeployContainer(this, .5f, DeployType.PEON_HARVEST_IRON, null));
					deploy_containers.put(DeployType.PEON_TRANSPORT_IRON, new DeployContainer(this, .5f, DeployType.PEON_TRANSPORT_IRON, IronSupply.class));
					deploy_containers.put(DeployType.PEON_HARVEST_RUBBER, new DeployContainer(this, .5f, DeployType.PEON_HARVEST_RUBBER, null));
					deploy_containers.put(DeployType.PEON_TRANSPORT_RUBBER, new DeployContainer(this, .5f, DeployType.PEON_TRANSPORT_RUBBER, RubberSupply.class));
				}
				else if (getAbilities().hasAbilities(Abilities.REPRODUCE)) {
					chieftain_container = new ChieftainContainer(this);
					deploy_containers.put(DeployType.PEON, new DeployContainer(this, .5f, DeployType.PEON, null));
				}
			}
		}
	}

	public static boolean isPlacingLegal(@NonNull UnitGrid unit_grid, @NonNull BuildingTemplate template, int grid_x, int grid_y) {
		return doIsPlacingLegal(unit_grid, grid_x, grid_y, template.getPlacingSize());
	}

	public boolean isPlacingLegal() {
		return !isDead() && getOwner().canBuild(getBuildingTemplate().getTemplateID()) &&
			doIsPlacingLegal(getUnitGrid(), getGridX(), getGridY(), getBuildingTemplate().getPlacingSize() - PLACING_BORDER);
	}

	public boolean isPlaced() {
		assert !isDead();
		return build_points != 0;
	}

	public boolean isComplete() {
		return build_points == getBuildingTemplate().getMaxHitPoints();
	}

    @Override
	public float getHitOffsetZ() {
		return getTemplate().getHitOffsetZ(getRenderLevel().ordinal());
	}

	public static boolean doIsPlacingLegal(@NonNull UnitGrid unit_grid, int grid_x, int grid_y, int size) {
		if (!unit_grid.getHeightMap().canBuild(grid_x, grid_y, size))
			return false;

		for (int y = 0; y < size*2 - 1; y++) {
            for (int x = 0; x < size*2 - 1; x++) {
                int current_grid_x = grid_x + x - (size - 1);
                int current_grid_y = grid_y + y - (size - 1);
                if (current_grid_x >= unit_grid.getGridSize() || current_grid_y >= unit_grid.getGridSize() ||
                        current_grid_x < 0 || current_grid_y < 0 || unit_grid.isGridOccupied(current_grid_x, current_grid_y))
                    return false;
            }
        }
		 return true;
	}

        @Override
	public int getAttackPriority() {
		if (getAbilities().hasAbilities(Abilities.ATTACK))
			return AttackScanFilter.PRIORITY_TOWER;
		else if (getAbilities().hasAbilities(Abilities.BUILD_ARMIES))
			return AttackScanFilter.PRIORITY_ARMORY;
		else
			return AttackScanFilter.PRIORITY_QUARTERS;
	}

        @Override
	protected void setTarget(@NonNull Target target, int action, boolean aggressive) {
		if (getAbilities().hasAbilities(Abilities.ATTACK)) {
			if (target != this) {
				Unit unit = ((MountUnitContainer)getUnitContainer()).getUnit();
				boolean kill_friendly = false;
				if (action == Target.ACTION_ATTACK)
					kill_friendly = true;
				if (unit != null && unit.canAttack(target, kill_friendly))
					unit.pushController(new AttackController(unit, (Selectable)target));
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
		assert (result == 1): "Too many buildings";
		build_points = 1;
		reinsert();
	}

        @Override
	public float getSize() {
		assert !isDead();
		float radius = (getBuildingTemplate().getPlacingSize() - 1);
		return (float)Math.sqrt(2)*radius + .1f;
	}

        @Override
	public int getPenalty() {
		assert !isDead();
		return Occupant.STATIC;
	}

        @Override
	protected void removeDying() {

		new RandomVelocityEmitter(getOwner().getWorld(), new Vector3f(getPositionX(), getPositionY(), getPositionZ()), 0f, 0f,
					getBuildingTemplate().getSmokeRadius(), getBuildingTemplate().getSmokeHeight(), 1f, 1f,
					30, 400f,
					new Vector3f(0f, 0f, .1f), new Vector3f(0f, 0f, -2.5f),
					new Vector4f(1f, .8f, .6f, 1f), new Vector4f(0f, 0f, 0f, -1f),
					new Vector3f(1f, 1f, 1f), new Vector3f(7.5f, 7.5f, 7.5f), 1, 0.75f,
					GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
					getOwner().getWorld().getRacesResources().getSmokeTextures(),
					getOwner().getWorld().getAnimationManagerRealTime());

		remove_delay = REMOVE_DELAY;
		getOwner().getWorld().getAudio().newAudio(new AudioParameters<>(getOwner().getWorld().getRacesResources().getBuildingCollapseSound(), getPositionX(), getPositionY(), getPositionZ(), AudioPlayer.AUDIO_RANK_BUILDING_COLLAPSE, AudioPlayer.AUDIO_DISTANCE_BUILDING_COLLAPSE, AudioPlayer.AUDIO_GAIN_BUILDING_COLLAPSE, AudioPlayer.AUDIO_RADIUS_BUILDING_COLLAPSE));
		if (getUnitContainer() != null) {
			while (getUnitContainer().getNumSupplies() > 0) {
				Unit unit = getUnitContainer().exit();
				if (unit != null)
					unit.removeNow();
			}
		}
		SupplyContainer worker_container = getBuildSupplyContainer(Unit.class);
		if (worker_container != null) {
			int result = getOwner().getUnitCountContainer().increaseSupply(-worker_container.getNumSupplies());
			assert result == -worker_container.getNumSupplies();
		}
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
		if (!(t instanceof Building))
			return false;
		Building b = (Building)t;
		return getOwner() == b.getOwner() && b.getAbilities().hasAbilities(Abilities.RALLY_TO);
	}

	public void setRallyPoint(@NonNull Target target) {
		if (!getOwner().canSetRallyPoints())
			return;
		if (isValidRallyPoint(target)) {
			rally_point = target;
		} else {
			rally_point = getUnitGrid().findGridTargets(target.getGridX(), target.getGridY(), 1, false)[0];
		}
	}

	public @NonNull BuildState getRenderLevel() {
		if (build_points == getBuildingTemplate().getMaxHitPoints())
			return BuildState.BUILT;
		else if ((float)build_points/getBuildingTemplate().getMaxHitPoints() < .5)
			return BuildState.START;
		else
			return BuildState.HALFBUILT;
	}

        @Override
	public SpriteKey getSpriteRenderer() {
		BuildState render_level = getRenderLevel();
		switch (render_level) {
			case START:
				return getBuildingTemplate().getStartRenderer();
			case HALFBUILT:
				return getBuildingTemplate().getHalfbuiltRenderer();
			case BUILT:
				return getBuildingTemplate().getBuiltRenderer();
			default:
				throw new IllegalStateException();
		}
	}

    @Override
	public void visit(@NonNull ToolTipVisitor visitor) {
		visitor.visitBuilding(this);
	}

	private void flattenLandscape() {
		int size = getBuildingTemplate().getPlacingSize();
		int height_points = (size - PLACING_BORDER)*2;
		int offset_x = getGridX() - (size - 1);
		int offset_y = getGridY() - (size - 1);
		float total_height = 0;
		old_landscape_heights = new float[height_points][height_points];
		for (int y = 0; y < height_points; y++) {
            for (int x = 0; x < height_points; x++) {
                float old_height = getOwner().getWorld().getHeightMap().getWrappedHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER);
                old_landscape_heights[y][x] = old_height;
                total_height += old_height;
            }
        }

		float new_height = total_height/(height_points*height_points);
		for (int y = 0; y < height_points; y++) {
            for (int x = 0; x < height_points; x++) {
                getOwner().getWorld().getHeightMap().editHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER, new_height);
            }
        }
	}

	private void undoLandscape() {
		int size = getBuildingTemplate().getPlacingSize();
		int offset_x = getGridX() - (size - 1);
		int offset_y = getGridY() - (size - 1);
		for (int y = 0; y < old_landscape_heights.length; y++) {
            for (int x = 0; x < old_landscape_heights[y].length; x++) {
                getOwner().getWorld().getHeightMap().editHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER, old_landscape_heights[y][x]);
            }
        }
	}

    @SuppressWarnings("unchecked")
	private void occupy() {
		UnitGrid grid = getUnitGrid();
		grid.getRegion(getGridX(), getGridY()).registerObject((Class<Building>) getClass(), this);
		int size = getBuildingTemplate().getPlacingSize()*2 - 1;
		for (int y = PLACING_BORDER; y < size - PLACING_BORDER; y++) {
            for (int x = PLACING_BORDER; x < size - PLACING_BORDER; x++) {
                grid.occupyGrid(getGridX() - size/2 + x, getGridY() - size/2 + y, this);
            }
        }
	}

    @SuppressWarnings("unchecked")
	private void free() {
		UnitGrid grid = getUnitGrid();
		grid.getRegion(getGridX(), getGridY()).unregisterObject((Class<Building>) getClass(), this);
		int size = getBuildingTemplate().getPlacingSize()*2 - 1;
		for (int y = PLACING_BORDER; y < size - PLACING_BORDER; y++) {
            for (int x = PLACING_BORDER; x < size - PLACING_BORDER; x++) {
                grid.freeGrid(getGridX() - size/2 + x, getGridY() - size/2 + y, this);
            }
        }
	}

        @Override
	public void hit(int damage, float dir_x, float dir_y, @NonNull Player owner) {
		super.hit(damage, dir_x, dir_y, owner);
		if (!isDead()) {
			setHitPoints(hit_points - damage);
			World world = getOwner().getWorld();
			world.getAudio().newAudio(new AudioParameters<>(world.getRacesResources().getBuildingHitSound(world.getRandom()), getPositionX(), getPositionY(), getPositionZ(), AudioPlayer.AUDIO_RANK_WEAPON_HIT, AudioPlayer.AUDIO_DISTANCE_WEAPON_HIT, AudioPlayer.AUDIO_GAIN_WEAPON_HIT, AudioPlayer.AUDIO_RADIUS_WEAPON_HIT));
			if (hit_points == 0) {
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

        @Override
	public float getAnimationTicks() {
		return 0;
	}

        @Override
	public int getAnimation() {
		return 0;
	}

	public void fillSupplies(Class<?> key, int max) {
		SupplyContainer container = getSupplyContainer(key);
		if (container != null) {
			container.increaseSupply(Math.min(container.getMaxSupplyCount() - container.getNumSupplies(), max));
		}
	}

	public void removeSupplies(Class<?> key) {
		SupplyContainer container = getSupplyContainer(key);
		if (container != null) {
			container.increaseSupply(-container.getNumSupplies());
		}
	}

    @Override
	public int getStatusValue() {
		if (getAbilities().hasAbilities(Abilities.REPRODUCE)) {
			return getUnitContainer().getNumSupplies();
		} else if (getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
			return getUnitContainer().getNumSupplies() + getSupplyContainer(RockAxeWeapon.class).getNumSupplies() + getSupplyContainer(IronAxeWeapon.class).getNumSupplies()*3 + getSupplyContainer(RubberAxeWeapon.class).getNumSupplies()*8;
		} else
			return 0;
	}

	public void printDebugInfo() {
		System.out.println("-----------------------------------");
		if (getAbilities().hasAbilities(Abilities.REPRODUCE)) {
			System.out.println("Units = " + getUnitContainer().getNumSupplies());
		} else if (getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
			System.out.println("Units = " + getUnitContainer().getNumSupplies());
			System.out.println("Tree = " + getSupplyContainer(TreeSupply.class).getNumSupplies());
			System.out.println("Rock = " + getSupplyContainer(RockSupply.class).getNumSupplies());
			System.out.println("Iron = " + getSupplyContainer(IronSupply.class).getNumSupplies());
			System.out.println("Rubber = " + getSupplyContainer(RubberSupply.class).getNumSupplies());
			System.out.println("Rock Weapons = " + getSupplyContainer(RockAxeWeapon.class).getNumSupplies());
			System.out.println("Iron Weapons = " + getSupplyContainer(IronAxeWeapon.class).getNumSupplies());
			System.out.println("Rubber Weapons = " + getSupplyContainer(RubberAxeWeapon.class).getNumSupplies());
		}
	}
}
