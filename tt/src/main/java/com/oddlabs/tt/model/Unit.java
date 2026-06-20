package com.oddlabs.tt.model;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.audio.AudioParameters;
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
import com.oddlabs.tt.pathfinder.Movable;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.PathTracker;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.resource.AudioAssets;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a mobile unit in the game world.
 */
public final class Unit extends Selectable<UnitTemplate> implements Occupant, Movable {

    private static final float IDLE_SPEED = 1f / 2.5f;
    private static final float TRANSPORT_SPEED_SCALE = 4f / 5f;

    private static final int PENALTY_INCREMENT = 3;
    private static final int INITIAL_PATH_PENALTY = 5;
    private static final EnumMap<MagicType, Float> MAX_MAGIC_ENERGY = new EnumMap<>(Map.of(
            MagicType.POISON_FOG, 40f,
            MagicType.LIGHTNING_CLOUD, 70f,
            MagicType.STUN, 40f,
            MagicType.SONIC_BLAST, 70f
    ));

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
    private final @NonNull EnumMap<MagicType, Float> magic_energy = new EnumMap<>(MagicType.class);
    private @Nullable MagicType last_magic_type = null;

    private int hit_points;
    private @NonNull Animation animation = Animation.IDLING;
    private float anim_speed;
    private float anim_time;
    private int path_penalty;
    /**
     * unit is in a tower
     */
    private boolean mounted;
    private float mount_offset = 0;
    private Building mounted_building;
    private float range_bonus;

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template) {
        this(owner, x, y, rally_point, unit_template, null);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template, @Nullable String name) {
        this(owner, x, y, rally_point, unit_template, name, true);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template, @Nullable String name, boolean notify_by_chieftain) {
        this(owner, x, y, rally_point, unit_template, name, notify_by_chieftain, false);
    }

    public Unit(@NonNull Player owner, float x, float y, @Nullable Target rally_point,
            @NonNull UnitTemplate unit_template, @Nullable String name, boolean notify_by_chieftain,
            boolean grid_targets_only) {
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
        } else if (notify_by_chieftain) {
            owner.getWorld().getNotificationListener().newSelectableNotification(this);
        }
        if (rally_point != null) {
            Target unit_target;
            if (rally_point instanceof LandscapeTarget lt) {
                UnitGrid grid = getUnitGrid();
                List<Target> temp_occupants = new ArrayList<>();
                for (var s : getOwner().getUnits().getSet()) {
                    if (s.getCurrentController() instanceof WalkController wc) {
                        Target target = wc.getTarget();
                        if (!grid.isGridOccupied(target.getGridX(), target.getGridY())) {
                            grid.occupyGrid(target.getGridX(), target.getGridY(), this);
                            temp_occupants.add(target);
                        }
                    }
                }
                unit_target = grid.findGridTargets(lt.getGridX(), lt.getGridY(), 1, true)[0];
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
    protected float getZError() {
        return getLandscapeError();
    }

    public @Nullable UnitSupplyContainer getSupplyContainer() {
        return supply_container;
    }

    @Override
    public @NonNull String toString() {
        if (!isDead())
            return "Unit: " + hashCode() + " | getOwner() = " + getOwner() + " | mounted = " + mounted
                    + " | getGridX() = " + getGridX() + " | getGridY() = " + getGridY();
        else
            return super.toString();
    }

    private void findInitialPosition(float x, float y, boolean grid_targets_only) {
        UnitGrid unit_grid = getUnitGrid();
        Target reserved_target = unit_grid.findGridTargets(UnitGrid.toGridCoordinate(x), UnitGrid.toGridCoordinate(y),
                1, grid_targets_only)[0];
        setGridPosition(reserved_target.getGridX(), reserved_target.getGridY());
        setPosition(reserved_target.getPositionX(), reserved_target.getPositionY());

        // Orient initially towards world center
        float center = getOwner().getWorld().getHeightMap().getMetersPerWorld() / 2f;
        float dx = center - reserved_target.getPositionX();
        float dy = center - reserved_target.getPositionY();
        float len = (float) Math.hypot(dx, dy);
        if (len > 0) {
            setDirection(dx / len, dy / len);
        }

        occupy();
        reinsert();
    }

    @Override
    public int getStatusValue() {
        int tower_factor = 1;
        if (mounted)
            tower_factor = 3;
        return getTemplate().getStatusValue() * tower_factor;
    }

    public void increaseRange(float amount) {
        assert !isDead();
        range_bonus += amount;
    }

    @Override
    public AttackScanFilter.@NonNull Priority getAttackPriority() {
        assert !isDead();
        return getTemplate().getAbilities().hasAbilities(Abilities.BUILD)
                ? AttackScanFilter.Priority.PEON
                : AttackScanFilter.Priority.WARRIOR;
    }

    public @Nullable String getName() {
        return name;
    }

    public int getHitPoints() {
        return hit_points;
    }

    public void unmount() {
        assert !isDead();
        clearControllerStack();
        swapController(new IdleController(this, new AttackScanFilter(getOwner(), AttackScanFilter.UNIT_RANGE), true));
        mounted = false;
        mount_offset = 0;
        enable();
        findInitialPosition(getPositionX(), getPositionY(), true);
    }

    public void mount(@NonNull Building building) {
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

    public boolean isMounted() {
        return mounted;
    }

    @Override
    public boolean isEnabled() {
        return !isDead() && !mounted;
    }

    public float getMetersPerSecond() {
        assert !isDead();
        if (getAbilities().hasAbilities(Abilities.HARVEST) && supply_container.getNumSupplies() > 0)
            return TRANSPORT_SPEED_SCALE * getTemplate().getMetersPerSecond();
        else
            return getTemplate().getMetersPerSecond();
    }

    public void aimAtTarget(@NonNull Target target) {
        assert !isDead();
        float dx = target.getPositionX() - getPositionX();
        float dy = target.getPositionY() - getPositionY();
        float dir_len_inv = 1f / (float) Math.hypot(dx, dy);
        dx *= dir_len_inv;
        dy *= dir_len_inv;
        setDirection(dx, dy);
    }

    public void switchToIdleAnimation() {
        assert !isDead();
        switchAnimation(IDLE_SPEED, Animation.IDLING);
    }

    public @NonNull WeaponFactory getWeaponFactory() {
        assert !isDead();
        return getTemplate().getWeaponFactory();
    }

    public float getRange(@NonNull Target target) {
        assert !isDead();
        return getWeaponFactory().getRange() + range_bonus + target.getSize();
    }

    @Override
    public float getSize() {
        assert !isDead();
        return 1.9f;
    }

    @Override
    protected @NonNull BoundingBox @NonNull [] getLocalBounds() {
        return getTemplate().getBounds();
    }

    @Override
    public void doAnimate(float t) {
        anim_time += anim_speed * t;
        if (isDead() || mounted)
            reinsert();
        getOwner().getWorld().updateGlobalChecksum(animation.ordinal());

        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
            getOwner().getRaceInfo().getMagics().forEach(magic -> increaseMagicEnergy(magic, t));
        }
    }

    private float getMagicEnergy(@NonNull MagicType type) {
        return magic_energy.getOrDefault(type, 0f);
    }

    private void setMagicEnergy(@NonNull MagicType type, float value) {
        magic_energy.put(type, value);
    }

    public void increaseMagicEnergy(@NonNull MagicType type, float amount) {
        float nextVal = Math.min(MAX_MAGIC_ENERGY.get(type), getMagicEnergy(type) + amount);
        setMagicEnergy(type, nextVal);
    }

    public void maxMagicEnergy(@NonNull MagicType type) {
        setMagicEnergy(type, MAX_MAGIC_ENERGY.get(type));
    }

    @Override
    public @NonNull PathTracker getTracker() {
        assert !isDead();
        return path_tracker;
    }

    @Override
    public void markBlocking() {
        assert !isDead();
        path_penalty = Math.min(path_penalty + PENALTY_INCREMENT, STATIC - 1); // never gets STATIC
    }

    @Override
    public int getPenalty() {
        assert !isDead();
        return isBlocking() ? Occupant.STATIC : path_penalty;
    }

    @Override
    protected void removeDying() {
        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
            getOwner().setActiveChieftain(null);
        }
        free();
        if (!getAbilities().hasAbilities(Abilities.MAGIC)) {
            int result = getOwner().getUnitCountContainer().increaseSupply(-1);
            assert result == -1;
        }
        super.removeDying();
    }

    public void removeNow() {
        assert !isDead();
        removeDying();
        remove();
    }

    @Override
    public void free() {
        assert !isDead();
        UnitGrid unit_grid = getUnitGrid();
        unit_grid.freeGrid(getGridX(), getGridY(), this);
        path_penalty = INITIAL_PATH_PENALTY;
    }

    @Override
    public void occupy() {
        assert !isDead();
        UnitGrid unit_grid = getUnitGrid();
        unit_grid.occupyGrid(getGridX(), getGridY(), this);

        // stats
        getOwner().unitMoved();
    }

    @Override
    public boolean isMoving() {
        return getCurrentBehaviour() instanceof WalkBehaviour;
    }

    /*	public final void moveNextAnimate() {
    	WalkBehaviour behaviour = (WalkBehaviour)getCurrentBehaviour();
    	behaviour.moveNextAnimate();
    }
     */
    @Override
    public void hit(int damage, float direction_x, float direction_y, @NonNull Player owner) {
        super.hit(damage, direction_x, direction_y, owner);
        if (mounted) {
            mounted_building.hit(damage, direction_x, direction_y, owner);
        } else if (!isDead()) {
            hit_points = Math.clamp(hit_points - damage, 0, getTemplate().getMaxHitPoints());
            if (hit_points == 0) {
                // stats
                owner.unitKilled();
                getOwner().unitLost();

                getClientState(ModelClient.class).ifPresent(client -> {
                    client.addVisualSound(EmojiType.GRAVESTONE,
                            ModelClient.DURATION_UNIT_DEATH, AudioAssets.AUDIO_DISTANCE_DEATH);
                });

                pushController(new DieController(this));
                forceDecide();
                float pitchRange = getTemplate().getDeathPitch();
                var params = new AudioParameters(getTemplate().getDeathSound(), AudioAssets.AUDIO_RANK_DEATH,
                        AudioAssets.AUDIO_DISTANCE_DEATH, AudioAssets.AUDIO_GAIN_DEATH, AudioAssets.AUDIO_RADIUS_DEATH,
                        1f + (pitchRange > 0f ? ThreadLocalRandom.current().nextFloat(-0.5f * pitchRange, 0.5f
                                * pitchRange) : 0f));
                getOwner().getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(), params);
                setDirection(-direction_x, -direction_y);
                removeDying();
            }
        }
    }

    public void stun(float time) {
        pushController(new StunController(this, time));
        forceDecide();
    }

    public boolean canAttack(@NonNull Target target, boolean kill_friendly) {
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
                (action_repair || !building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER) || !building
                        .isBuilt()) &&
                // getOwner() == building.getOwner() && building.isPlaced() && building.isDamaged();
                !getOwner().isEnemy(building.getOwner()) && building.isPlaced() && building.isDamaged();
    }

    private boolean canEnter(@NonNull Target target) {
        return target instanceof Building building &&
                !getAbilities().hasAbilities(Abilities.MAGIC) &&
                building.getUnitContainer().isPresent() &&
                getOwner() == building.getOwner() &&
                building.getUnitContainer().get().canEnter(this);
    }

    @Override
    public float getDefenseChance() {
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
            case DEFAULT -> {
                if (canBuild(target)) {
                    pushController(new PlaceBuildingController(this, (Building) target));
                } else if (canGather(target)) {
                    pushController(new GatherController(this, (Supply) target, ((Supply) target).getSupplyType()));
                } else if (canRepair(target, false)) {
                    pushController(new RepairController(this, (Building) target));
                } else if (canEnter(target)) {
                    pushController(new EnterController(this, (Building) target));
                } else if (canAttack(target, false)) {
                    pushController(new HuntController(this, (Selectable<?>) target));
                } else {
                    walkToTarget(target, aggressive);
                }
            }
            case MOVE -> {
                if (canEnter(target)) {
                    pushController(new EnterController(this, (Building) target));
                } else {
                    walkToTarget(target, false);
                }
            }
            case ATTACK -> {
                if (canAttack(target, true)) {
                    pushController(new HuntController(this, (Selectable<?>) target));
                } else {
                    walkToTarget(target, true);
                }
            }
            case GATHER_REPAIR -> {
                if (canGather(target)) {
                    pushController(new GatherController(this, (Supply) target, ((Supply) target).getSupplyType()));
                } else if (canRepair(target, true)) {
                    pushController(new RepairController(this, (Building) target));
                }
            }
            case DEFEND -> pushController(new DefendController(this, target));
            default -> IO.println("Invalid action: " + action);
        }
    }

    public void printDebugInfo() {
        IO.println("-----------------------------------");
        IO.println("Primary Controller = " + getPrimaryController());
        if (getAbilities().hasAbilities(Abilities.MAGIC)) {
            IO.println("Hit Points = " + hit_points);
            for (MagicType type : getOwner().getRaceInfo().getMagics()) {
                IO.println("Magic Energy " + type.name() + " = " + getMagicEnergy(type));
            }
            IO.println("Controller = " + getPrimaryController());
        }
    }

    public boolean canDoMagic(@NonNull MagicType type) {
        return !isDead() && getOwner().canDoMagic(type) && getMagicEnergy(type) == MAX_MAGIC_ENERGY.get(type);
    }

    public boolean canDoMagic(int slotIndex) {
        return canDoMagic(getOwner().getRaceInfo().getMagicType(slotIndex));
    }

    public void doMagic(@NonNull MagicType type, boolean clear_stack) {
        if (canDoMagic(type)) {
            if (clear_stack)
                clearControllerStack();
            pushController(new MagicController(this, getOwner().getRaceInfo().getMagicFactory(type)));
            magic_energy.clear();
            last_magic_type = type;

            // stats
            getOwner().magicCast();
        }
    }

    public @Nullable MagicType getLastMagicType() {
        return last_magic_type;
    }

    public float getMagicProgress(@NonNull MagicType type) {
        return getMagicEnergy(type) / MAX_MAGIC_ENERGY.get(type);
    }

    public void switchAnimation(float anim_speed, @NonNull Animation animation) {
        assert !isDead();
        this.anim_speed = anim_speed;
        if (this.animation != animation) {
            this.animation = animation;
            this.anim_time = 0f;
        } else if (getTemplate().getAnimationType(animation) == AnimationInfo.AnimationType.PLAIN) {
            this.anim_time = 0f;
        }
    }

    @Override
    public int getAnimation() {
        return animation.ordinal();
    }

    @Override
    public float getAnimationTicks() {
        return anim_time;
    }

    public float getMountOffset() {
        assert !isDead();
        return mount_offset;
    }

    @Override
    public float getOffsetZ() {
        if (mounted)
            return mounted_building.getOffsetZ() + mount_offset;
        else {
            if (isDead()) {
                DieBehaviour die_behaviour = (DieBehaviour) getCurrentBehaviour();
                return die_behaviour.getOffsetZ();
            } else
                return getSlopeOffset(getSize() * 0.2f);
        }
    }

    @Override
    public void debugRender() {
        path_tracker.debugRender();
    }
}
