package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.ModelClient;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.tt.model.Target;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Logic controller for the Lightning Cloud magic effect.
 */
public final class LightningCloud extends Model implements Magic {
    private static final int NUM_STRIKES = 6;
    private static final float SECONDS_BETWEEN_STRIKES = .125f;

    private final @NonNull Player owner;
    private final float seconds_per_hit;
    private final float meters_per_second;
    private final float hit_chance;
    private final int damage;
    private final float height;
    private final float seconds_to_init;
    private final float offset_z;

    private float seconds_to_live;
    private @Nullable Selectable<?> target = null;
    private @Nullable Selectable<?> prev_target = null;
    private float hit_timer = 0f;
    private int strike_counter = 0;

    public LightningCloud(@NonNull World world, float offset_x, float offset_y, float offset_z, float seconds_to_live,
            float seconds_per_hit, float seconds_to_init, float meters_per_second, float hit_chance, int damage,
            float height, @NonNull Unit src) {
        super(world);
        float start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        float start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        setPosition(start_x, start_y, world.getHeightMap().getNearestHeight(start_x, start_y) + height);
        register();
        world.getAnimationManagerGameTime().registerAnimation(this);

        this.seconds_to_live = seconds_to_live;
        this.seconds_to_init = seconds_to_init;
        this.offset_z = offset_z;
        this.seconds_per_hit = seconds_per_hit;
        this.meters_per_second = meters_per_second;
        this.hit_chance = hit_chance;
        this.damage = damage;
        this.height = height;
        owner = src.getOwner();
    }

    public float getSecondsToLive() {
        return seconds_to_live;
    }

    public float getSecondsToInit() {
        return seconds_to_init;
    }

    public float getCloudHeight() {
        return height;
    }

    public float getCloudOffsetZ() {
        return offset_z;
    }

    @Override
    public void remove() {
        super.remove();
        owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
        getClientState(ModelClient.class).ifPresent(ModelClient::close);
    }

    @Override
    protected void onReinsert() {
        float x = getPositionX();
        float y = getPositionY();
        float z = getPositionZ();
        setBounds(x, x, y, y, z, z);
    }

    @Override
    protected @NonNull BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }

    @Override
    public void animate(float t) {
        seconds_to_live -= t;
        if (seconds_to_live <= 0f) {
            owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
            remove();
        }

        hit_timer += t;

        if (hit_timer > seconds_per_hit) {
            if (target == null) {
                target = owner.findNearestEnemy(UnitGrid.toGridCoordinate(getPositionX()), UnitGrid.toGridCoordinate(
                        getPositionY()), prev_target).orElse(null);
                if (target == null) {
                    target = owner.findNearestEnemy(UnitGrid.toGridCoordinate(getPositionX()), UnitGrid
                            .toGridCoordinate(getPositionY()), null).orElse(null);
                    if (target == null) {
                        animateClientState(t);
                        return;
                    }
                }
            }

            float dx = target.getPositionX() - getPositionX();
            float dy = target.getPositionY() - getPositionY();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= dist;
            dy /= dist;
            if (dist < meters_per_second * t) {
                if (!target.isDead() && owner.getWorld().getRandom().nextFloat() < hit_chance * (1 - target
                        .getDefenseChance())) {
                    target.hit(damage, dx, dy, owner);
                }
                strike(target);
                strike(target);
                prev_target = target;
                target = null;
                hit_timer = 0f;
                strike_counter = 0;
            } else {
                float x = getPositionX() + dx * (meters_per_second * t);
                float y = getPositionY() + dy * (meters_per_second * t);
                float z = owner.getWorld().getHeightMap().getNearestHeight(x, y) + height;
                setPosition(x, y, z);
            }
        } else if (prev_target != null && strike_counter < NUM_STRIKES - 1 && hit_timer > (strike_counter + 1)
                * SECONDS_BETWEEN_STRIKES) {
                    strike(prev_target);
                    strike(prev_target);
                    strike_counter++;
                }
        animateClientState(t);
    }

    private void strike(@NonNull Target target) {
        float x = target.getPositionX();
        float y = target.getPositionY();
        float z = owner.getWorld().getHeightMap().getNearestHeight(x, y);

        getClientState(ModelClient.class).ifPresent(client -> client.addLightningStrike(x, y, z));
    }

    @Override
    public void interrupt() {
        remove();
    }
}
