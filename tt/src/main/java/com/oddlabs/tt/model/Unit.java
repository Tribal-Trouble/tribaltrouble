package com.oddlabs.tt.model;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.behaviour.DefendController;
import com.oddlabs.tt.model.behaviour.DieBehaviour;
import com.oddlabs.tt.model.behaviour.DieController;
import com.oddlabs.tt.model.behaviour.EnterController;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.model.behaviour.HuntController;
import com.oddlabs.tt.model.behaviour.IdleController;
import com.oddlabs.tt.model.behaviour.MagicController;
import com.oddlabs.tt.model.behaviour.PlaceBuildingController;
import com.oddlabs.tt.model.behaviour.RepairController;
import com.oddlabs.tt.model.behaviour.StunController;
import com.oddlabs.tt.model.behaviour.WalkBehaviour;
import com.oddlabs.tt.model.behaviour.WalkController;
import com.oddlabs.tt.model.weapon.WeaponFactory;
import com.oddlabs.tt.particle.BalancedParametricEmitter;
import com.oddlabs.tt.particle.StunFunction;
import com.oddlabs.tt.pathfinder.Movable;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.PathTracker;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.util.Target;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Unit extends Selectable<UnitTemplate> implements Occupant, Movable {

    private static final float IDLE_SPEED = 1f / 2.5f;
    private static final float TRANSPORT_SPEED_SCALE = 4f / 5f;

    private static final int PENALTY_INCREMENT = 3;
    private static final int INITIAL_PATH_PENALTY = 5;
    private static final float[] MAX_MAGIC_ENERGY = new float[]{40f, 70f};

    public enum Animation {
        IDLING,
        MOVING,
        THROWING,
        DYING,
        MAGIC,
        THOR
    }

    public static final int SPEAR_RELEASE_FRAME = 29;

    private final @Nullable UnitSupplyContainer supply_container;
    private final @Nullable String name;
    private final @NonNull PathTracker path_tracker;
    private final float[] magic_energy = new float[2];
    private int last_magic_index = -1;

    private @Nullable BalancedParametricEmitter stun_marker;
    private int hit_points;
    private @NonNull Animation animation = Animation.IDLING;
    private float anim_speed;
    private float anim_time;
    private int path_penalty;
    /** unit is in a tower */
    private boolean mounted;
    private float mount_offset = 0;
    private Building mounted_building;
    private float range_bonus;

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point, @NonNull UnitTemplate unit_template) {
        this(owner, x, y, rally_point, unit_template, null);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point, @NonNull UnitTemplate unit_template, @Nullable String name) {
        this(owner, x, y, rally_point, unit_template, name, true);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point, @NonNull UnitTemplate unit_template, @Nullable String name, boolean notify_by_chieftain) {
        this(owner, x, y, rally_point, unit_template, name, notify_by_chieftain, false);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point, @NonNull UnitTemplate unit_template, @Nullable String name, boolean notify_by_chieftain, boolean grid_targets_only) {
        super(owner, unit_template);
        this.name = name;
        getAbilities().addAbilities(unit_template.getAbilities());
        register();
        hit_points = unit_template.getMaxHitPoints();
        this.path_tracker = new PathTracker(getUnitGrid(), this);
        UnitSupplyContainerFactory factory = unit_template.getUnitSupplyContainerFactory();
        supply_container = factory != null ? (UnitSupplyContainer) factory.createContainer(this) : null;

        findInitialPosition(x, y, grid_targets_only);
        pushController(new IdleController(this, new AttackScanFilter(getOwner(), AttackScanFilter.UNIT_RANGE), true));
        if (!getAbilities().hasAbilities(Abilities.MAGIC)) {
            int result = getOwner().getUnitCountContainer().increaseSupply(1);
            assert (result == 1) : "No room for new unit in player unit container.";
        } else
            if (notify_by_chieftain) {
                owner.getWorld().getNotificationListener().newSelectableNotification(this);
            }
        if (rally_point != null) {
            Target unit_target;
            if (rally_point instanceof LandscapeTarget) {
                UnitGrid grid = getUnitGrid();
                List<Target> temp_occupants = new ArrayList<>();
                for (var s : getOwner().getUnits().getSet()) {
                    if (s.getCurrentController() instanceof WalkController) {
                        Target target = ((WalkController) s.getCurrentController()).getTarget();
                        if (!grid.isGridOccupied(target.getGridX(), target.getGridY())) {
                            grid.occupyGrid(target.getGridX(), target.getGridY(), this);
                            temp_occupants.add(target);
                        }
                    }
                }
                unit_target = grid.findGridTargets(rally_point.getGridX(), rally_point.getGridY(), 1, true)[0];
                for (Target target : temp_occupants) {
                    grid.freeGrid(target.getGridX(), target.getGridY(), this);
                }
            } else
                unit_target = rally_point;

            boolean aggressive = unit_template.getAbilities().hasAbilities(Abilities.THROW);
            setTarget(unit_target, Action.DEFAULT, aggressive);
        }
    }

    @Override
    protected @NonNull Unit self() {
        return this;
    }

    @Override
    protected final float getZError() {
        return getLandscapeError();
    }

    @Override
    public final void visit(@NonNull ElementVisitor visitor) {
        visitor.visitUnit(this);
    }

    public final @Nullable UnitSupplyContainer getSupplyContainer() {
        return supply_container;
    }

    @Override
    public final String toString() {
        if (!isDead())
            return "Unit: " + hashCode() + " | getOwner() = " + getOwner() + " | mounted = " + mounted + " | getGridX() = " + getGridX() + " | getGridY() = " + getGridY();
        else
            return super.toString();
    }

    private void findInitialPosition(float x, float y, boolean grid_targets_only) {
        UnitGrid unit_grid = getUnitGrid();
        Target reserved_target = unit_grid.findGridTargets(UnitGrid.toGridCoordinate(x), UnitGrid.toGridCoordinate(y), 1, grid_targets_only)[0];
        setGridPosition(reserved_target.getGridX(), reserved_target.getGridY());
        setPosition(reserved_target.getPositionX(), reserved_target.getPositionY());
        
        // Orient initially towards world center
        float center = getOwner().getWorld().getHeightMap().getMetersPerWorld() / 2f;
        float dx = center - reserved_target.getPositionX();
        float dy = center - reserved_target.getPositionY();
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len > 0) {
            setDirection(dx/len, dy/len);
        }
        
        occupy();
        reinsert();
    }

    @Override
    public final int getStatusValue() {
        int tower_factor = 1;
        if (mounted)
            tower_factor = 3;
        return getTemplate().getStatusValue() * tower_factor;
    }

    public final void increaseRange(float amount) {
        assert !isDead();
        range_bonus += amount;
    }

	@Override
	public final AttackScanFilter.@NonNull Priority getAttackPriority() {
        assert !isDead();
        return getAbilities().hasAbilities(Abilities.BUILD)
				? AttackScanFilter.Priority.PEON
				: AttackScanFilter.Priority.WARRIOR;
	}

    @Override
    public final void visit(@NonNull ToolTipVisitor visitor) {
        visitor.visitUnit(this);
    }

    public final @Nullable String getName() {
        return name;
    }

    public final int getHitPoints() {
        return hit_points;
    }

    public final void unmount() {
        assert !isDead();
        clearControllerStack();
        swapController(new IdleController(this, new AttackScanFilter(getOwner(), AttackScanFilter.UNIT_RANGE), true));
        mounted = false;
        mount_offset = 0;
        enable();
        findInitialPosition(getPositionX(), getPositionY(), true);
    }

    public final void mount(@NonNull Building building) {
        assert !isDead();
        mounted_building = building;
        mount_offset = building.getTemplate().getMountOffset();
        disable();
        free();
        setPosition(building.getPositionX(), building.getPositionY());
        mounted = true;
        clearControllerStack();
        swapController(new IdleController(this, new AttackScanFilter(getOwner(), AttackScanFilter.TOWER_RANGE), false));
    }

    public final boolean isMounted() {
        return mounted;
    }

    @Override
    public final boolean isEnabled() {
        return !isDead() && !mounted;
    }

    public final float getMetersPerSecond() {
        assert !isDead();
        if (getAbilities().hasAbilities(Abilities.HARVEST) && supply_container.getNumSupplies() > 0)
            return TRANSPORT_SPEED_SCALE * getTemplate().getMetersPerSecond();
        else
            return getTemplate().getMetersPerSecond();
    }

    public final void aimAtTarget(@NonNull Target target) {
        assert !isDead();
        float dx = target.getPositionX() - getPositionX();
        float dy = target.getPositionY() - getPositionY();
        float dir_len_inv = 1f / (float) Math.sqrt(dx * dx + dy * dy);
        dx *= dir_len_inv;
        dy *= dir_len_inv;
        setDirection(dx, dy);
    }

    public final void switchToIdleAnimation() {
        assert !isDead();
        switchAnimation(IDLE_SPEED, Animation.IDLING);
    }

    public final @NonNull WeaponFactory getWeaponFactory() {
        assert !isDead();
        return getTemplate().getWeaponFactory();
    }

    public final float getRange(@NonNull Target target) {
        assert !isDead();
        return getWeaponFactory().getRange() + range_bonus + target.getSize();
    }

    @Override
    public final float getSize() {
        assert !isDead();
        return 1.9f;
    }

    @Override
    public final @NonNull SpriteKey getSpriteRenderer() {
        return getTemplate().getSpriteRenderer();
    }

    @Override
    public final void doAnimate(float t) {
        anim_time += anim_speed * t;
        if (isDead() || mounted)
            reinsert();
        getOwner().getWorld().updateGlobalChecksum(animation.ordinal());

        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
            for (int i = 0; i < magic_energy.length; i++) {
                increaseMagicEnergy(i, t);
            }
        }
    }

    public final void increaseMagicEnergy(int index, float amount) {
        magic_energy[index] += amount;
        if (magic_energy[index] > MAX_MAGIC_ENERGY[index]) {
            magic_energy[index] = MAX_MAGIC_ENERGY[index];
        }
    }

    @Override
    public final @NonNull PathTracker getTracker() {
        assert !isDead();
        return path_tracker;
    }

    @Override
    public final void markBlocking() {
        assert !isDead();
        path_penalty = Math.min(path_penalty + PENALTY_INCREMENT, STATIC - 1); // never gets STATIC
    }

    @Override
    public final int getPenalty() {
        assert !isDead();
        return isBlocking() ? Occupant.STATIC : path_penalty;
    }

    @Override
    protected final void removeDying() {
        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
            getOwner().setActiveChieftain(null);
        }
        free();
        if (!getAbilities().hasAbilities(Abilities.MAGIC)) {
            int result = getOwner().getUnitCountContainer().increaseSupply(-1);
            assert result == -1;
        }
        if (stun_marker != null) {
            stun_marker.done();
            stun_marker = null;
        }
        super.removeDying();
    }

    public final void removeNow() {
        assert !isDead();
        removeDying();
        remove();
    }

    @Override
    public final void free() {
        assert !isDead();
        UnitGrid unit_grid = getUnitGrid();
        unit_grid.freeGrid(getGridX(), getGridY(), this);
        path_penalty = INITIAL_PATH_PENALTY;
    }

    @Override
    public final void occupy() {
        assert !isDead();
        UnitGrid unit_grid = getUnitGrid();
        unit_grid.occupyGrid(getGridX(), getGridY(), this);

        // stats
        getOwner().unitMoved();
    }

    @Override
    public final boolean isMoving() {
        return getCurrentBehaviour() instanceof WalkBehaviour;
    }

    /*	public final void moveNextAnimate() {
		WalkBehaviour behaviour = (WalkBehaviour)getCurrentBehaviour();
		behaviour.moveNextAnimate();
	}
     */
    @Override
    public final void hit(int damage, float direction_x, float direction_y, @NonNull Player owner) {
        super.hit(damage, direction_x, direction_y, owner);
        if (mounted) {
            mounted_building.hit(damage, direction_x, direction_y, owner);
        } else
            if (!isDead()) {
                hit_points = Math.clamp(hit_points - damage, 0, getTemplate().getMaxHitPoints());
                if (hit_points == 0) {
                    // stats
                    owner.unitKilled();
                    getOwner().unitLost();

                    pushController(new DieController(this));
                    forceDecide();
                    /*
				new AudioPlayer(getPositionX(), getPositionY(), getPositionZ(),
						RacesResources.getUnitHitSound(),
						AudioPlayer.AUDIO_RANK_DEATH,
						AudioPlayer.AUDIO_DISTANCE_DEATH,
						AudioPlayer.AUDIO_GAIN_DEATH,
						AudioPlayer.AUDIO_RADIUS_DEATH,
						1f + (World.getRandom().nextFloat() - .5f)*getUnitTemplate().getDeathPitch());
                     */
                    getOwner().getWorld().getAudio().newAudio(new AudioParameters<>(getTemplate().getDeathSound(), getPositionX(), getPositionY(), getPositionZ(),
                            AudioPlayer.AUDIO_RANK_DEATH,
                            AudioPlayer.AUDIO_DISTANCE_DEATH,
                            AudioPlayer.AUDIO_GAIN_DEATH,
                            AudioPlayer.AUDIO_RADIUS_DEATH,
                            1f + (getOwner().getWorld().getRandom().nextFloat() - .5f) * getTemplate().getDeathPitch()));
                    setDirection(-direction_x, -direction_y);
                    removeDying();
                }
            }
    }

    public final void stun(float time) {
        float x = getPositionX() + getTemplate().getStunX() * getDirectionX() + getTemplate().getStunY() * (-getDirectionY());
        float y = getPositionY() + getTemplate().getStunX() * getDirectionY() + getTemplate().getStunY() * getDirectionX();
        float z = getOwner().getWorld().getHeightMap().getNearestHeight(x, y) + getTemplate().getStunZ() + mount_offset;

        if (stun_marker != null) {
            stun_marker.done();
        }
        stun_marker = createStunStar(x, y, z, time, (float) Math.PI / 2);
        pushController(new StunController(this, time));
        forceDecide();
    }

    private @NonNull BalancedParametricEmitter createStunStar(float x, float y, float z, float time, float velocity) {
        int num_particles = 5;
        return new BalancedParametricEmitter(getOwner().getWorld(), new StunFunction(.4f, .15f), new Vector3f(x, y, z),
                velocity, 5f, (float) Math.PI * 2, (float) Math.PI * 2,
                num_particles, 0f, 2f,
                new Vector4f(1f, 1f, 1f, 1f), new Vector4f(0f, 0f, 0f, 0f),
                new Vector3f(.1f, .1f, .1f), new Vector3f(0f, 0f, 0f), time,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, getOwner().getWorld().getRacesResources().getStarTextures(),
                getOwner().getWorld().getAnimationManagerGameTime());
    }

    public final boolean canAttack(@NonNull Target target, boolean kill_friendly) {
        assert !isDead();
        if (!(target instanceof Selectable<?> selectable) || !getAbilities().hasAbilities(Abilities.ATTACK))
            return false;
        Player target_player = selectable.getOwner();
        return kill_friendly || getOwner().isEnemy(target_player);
    }

    private boolean canBuild(@NonNull Target target) {
        return target instanceof Building building &&
                getAbilities().hasAbilities(Abilities.BUILD) &&
                !building.isPlaced();
    }

    private boolean canGather(@NonNull Target target) {
        return target instanceof Supply && getAbilities().hasAbilities(Abilities.BUILD);
    }

    private boolean canRepair(@NonNull Target target, boolean action_repair) {
        return target instanceof Building building &&
                getAbilities().hasAbilities(Abilities.BUILD) &&
                (action_repair || !building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER) || !building.isComplete()) &&
                // getOwner() == building.getOwner() && building.isPlaced() && building.isDamaged();
                !getOwner().isEnemy(building.getOwner()) && building.isPlaced() && building.isDamaged();
    }

    private boolean canEnter(@NonNull Target target) {
        return target instanceof Building building &&
                !getAbilities().hasAbilities(Abilities.MAGIC) &&
                building.getUnitContainer() != null &&
                getOwner() == building.getOwner() &&
                building.getUnitContainer().canEnter(this);
    }

    @Override
    public final float getDefenseChance() {
        return getCurrentController() instanceof StunController ? 0 : super.getDefenseChance();
    }

    private void walkToTarget(@NonNull Target target, boolean scan_attack) {
        Target walkable_target = getUnitGrid().findGridTargets(target.getGridX(), target.getGridY(), 1, false)[0];
        pushController(new WalkController(this, walkable_target, scan_attack));
    }

    @Override
    public void setTarget(@NonNull Target target, @NonNull Action action, boolean aggressive) {
        if (target == this)
            return;
        assert !target.isDead() : "Setting dead target";
        assert !mounted;
        switch (action) {
            case DEFAULT:
                if (canBuild(target)) {
                    pushController(new PlaceBuildingController(this, (Building) target));
                } else
                    if (canGather(target)) {
                        pushController(new GatherController(this, (Supply) target, ((Supply) target).getClass()));
                    } else
                        if (canRepair(target, false)) {
                            pushController(new RepairController(this, (Building) target));
                        } else
                            if (canEnter(target)) {
                                pushController(new EnterController(this, (Building) target));
                            } else
                                if (canAttack(target, false)) {
                                    pushController(new HuntController(this, (Selectable<?>) target));
                                } else {
                                    walkToTarget(target, aggressive);
                                }
                break;
            case MOVE:
                if (canEnter(target)) {
                    pushController(new EnterController(this, (Building) target));
                } else {
                    walkToTarget(target, false);
                }
                break;
            case ATTACK:
                if (canAttack(target, true)) {
                    pushController(new HuntController(this, (Selectable<?>) target));
                } else {
                    walkToTarget(target, true);
                }
                break;
            case GATHER_REPAIR:
                if (canGather(target)) {
                    pushController(new GatherController(this, (Supply) target, ((Supply) target).getClass()));
                } else if (canRepair(target, true)) {
                    pushController(new RepairController(this, (Building) target));
                }
                break;
            case DEFEND:
                pushController(new DefendController(this, target));
                break;
            default:
				IO.println("Invalid action: " + action);
                break;
        }
    }

    public final void printDebugInfo() {
		IO.println("-----------------------------------");
		IO.println("Primary Controller = " + getPrimaryController());
        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
			IO.println("Hit Points = " + hit_points);
			IO.println("Magic Energy 0 = " + magic_energy[0]);
			IO.println("Magic Energy 1 = " + magic_energy[1]);
			IO.println("Controller = " + getPrimaryController());
        }
    }

    public final boolean canDoMagic(int magic_index) {
        return !isDead() && magic_index >= 0 && magic_index < RacesResources.NUM_MAGIC && getOwner().canDoMagic(magic_index) && magic_energy[magic_index] == MAX_MAGIC_ENERGY[magic_index];
    }

    public final void doMagic(int magic_index, boolean clear_stack) {
        if (canDoMagic(magic_index)) {
            if (clear_stack)
                clearControllerStack();
            pushController(new MagicController(this, getOwner().getRace().getMagicFactory(magic_index)));
            Arrays.fill(magic_energy, 0f);
            last_magic_index = magic_index;

            // stats
            getOwner().magicCast();
        }
    }

    public final int getLastMagicIndex() {
        return last_magic_index; // for tutorial
    }

    public final float getMagicProgress(int magic_index) {
        return magic_energy[magic_index] / MAX_MAGIC_ENERGY[magic_index];
    }

    public final void switchAnimation(float anim_speed, Animation animation) {
        assert !isDead();
        this.anim_speed = anim_speed;
        if (this.animation != animation) {
            this.animation = animation;
            this.anim_time = 0f;
        } else if (getTemplate().getSpriteRenderer().getAnimationType(animation.ordinal()) == AnimationInfo.AnimationType.PLAIN.ordinal()) {
            this.anim_time = 0f;
        }
    }

    @Override
    public final int getAnimation() {
        return animation.ordinal();
    }

    @Override
    public final float getAnimationTicks() {
        return anim_time;
    }

    public final float getMountOffset() {
        assert !isDead();
        return mount_offset;
    }

    @Override
    public final float getOffsetZ() {
        if (mounted)
            return mounted_building.getOffsetZ() + mount_offset;
        else {
            if (isDead()) {
                DieBehaviour die_behaviour = (DieBehaviour) getCurrentBehaviour();
                return die_behaviour.getOffsetZ();
            } else
                return 0;
        }
    }

    public final void debugRender() {
        path_tracker.debugRender();
    }
}
