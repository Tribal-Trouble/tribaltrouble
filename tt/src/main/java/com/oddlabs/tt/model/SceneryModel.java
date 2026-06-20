package com.oddlabs.tt.model;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A world entity that represents static or ambient environment detail (scenery).
 * Decoupled from rendering-bound keys; stores its bounding box array directly.
 */
public sealed class SceneryModel extends Model implements Occupant, ModelToolTip, Animated permits Plants {
    private final @NonNull BoundsProvider boundsProvider;
    private final float shadow_diameter;
    private final boolean occupy;
    private final @Nullable String name;
    private final int animation;
    private final float seconds_per_animation_cycle;
    private float anim_time = 0;
    private boolean registeredTarget = false;

    public SceneryModel(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull BoundsProvider boundsProvider) {
        this(world, x, y, dir_x, dir_y, boundsProvider, 0f, false, null);
    }

    public SceneryModel(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull BoundingBox @NonNull [] bounds) {
        this(world, x, y, dir_x, dir_y, () -> bounds, 0f, false, null);
    }

    public SceneryModel(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull BoundsProvider boundsProvider, float shadow_diameter, boolean occupy, @Nullable String name) {
        this(world, x, y, dir_x, dir_y, boundsProvider, shadow_diameter, occupy, name, -1, -1, 0);
    }

    public SceneryModel(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull BoundingBox @NonNull [] bounds, float shadow_diameter, boolean occupy, @Nullable String name) {
        this(world, x, y, dir_x, dir_y, () -> bounds, shadow_diameter, occupy, name, -1, -1, 0);
    }

    public SceneryModel(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull BoundingBox @NonNull [] bounds, float shadow_diameter, boolean occupy, @Nullable String name,
            int animation, float seconds_per_animation_cycle, float anim_offset) {
        this(world, x, y, dir_x, dir_y, () -> bounds, shadow_diameter, occupy, name, animation,
                seconds_per_animation_cycle, anim_offset);
    }

    public SceneryModel(@NonNull World world, float x, float y, float dir_x, float dir_y,
            @NonNull BoundsProvider boundsProvider, float shadow_diameter, boolean occupy, @Nullable String name,
            int animation, float seconds_per_animation_cycle, float anim_offset) {
        super(world);
        this.boundsProvider = boundsProvider;
        this.shadow_diameter = shadow_diameter;
        this.occupy = occupy;
        this.name = name;
        this.animation = animation;
        this.seconds_per_animation_cycle = seconds_per_animation_cycle;
        anim_time = anim_offset;
        setPosition(x, y);
        setDirection(dir_x, dir_y);
        doRegister();
        if (occupy) {
            world.getUnitGrid().occupyGrid(getGridX(), getGridY(), this);
        }
    }

    public final @NonNull BoundsProvider getBoundsProvider() {
        return boundsProvider;
    }

    public final @Nullable String getName() {
        return name;
    }

    @Override
    public final float getShadowDiameter() {
        return shadow_diameter;
    }

    protected void doRegister() {
        register();
        reinsert();
        getWorld().getNotificationListener().registerTarget(this);
        registeredTarget = true;
        if (animation > -1)
            getWorld().getAnimationManagerGameTime().registerAnimation(this);
    }

    @Override
    public final void remove() {
        if (occupy) {
            getWorld().getUnitGrid().freeGrid(getGridX(), getGridY(), this);
        }
        super.remove();
        if (registeredTarget) {
            getWorld().getNotificationListener().unregisterTarget(this);
            registeredTarget = false;
        }
        if (animation > -1)
            getWorld().getAnimationManagerGameTime().removeAnimation(this);
    }

    @Override
    public final void animate(float t) {
        anim_time += t / 2.5f;
        if (seconds_per_animation_cycle > -1 && anim_time > seconds_per_animation_cycle)
            anim_time = 0;
        reinsert();
    }

    @Override
    public final int getAnimation() {
        return animation > -1 ? animation : 0;
    }

    @Override
    public final float getAnimationTicks() {
        return animation > -1 ? anim_time : 0;
    }

    @Override
    public final void updateChecksum(@NonNull StateChecksum checksum) {
    }

    @Override
    public int getPenalty() {
        return Occupant.STATIC;
    }

    @Override
    public final int getGridX() {
        return UnitGrid.toGridCoordinate(getPositionX());
    }

    @Override
    public final int getGridY() {
        return UnitGrid.toGridCoordinate(getPositionY());
    }

    @Override
    public final float getSize() {
        throw new UnsupportedOperationException("SceneryModel does not have a size");
    }

    @Override
    public final boolean isDead() {
        return false;
    }

    public final boolean isOccupying() {
        return occupy;
    }

    @Override
    protected @NonNull BoundingBox @NonNull [] getLocalBounds() {
        return boundsProvider.bounds();
    }
}
