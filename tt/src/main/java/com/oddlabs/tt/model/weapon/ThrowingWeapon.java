package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.WeaponVisualType;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.resource.AudioFile;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base {@link Model} class for all projectile weapons that are thrown through the world.
 */
public abstract sealed class ThrowingWeapon extends Model implements Animated permits RotatingThrowingWeapon,
        DirectedThrowingWeapon {
    /**
     * Multiplier for projectile arc exaggeration.
     */
    private static final float GRAVITY_MULTIPLIER = 3.0f;
    private static final float GRAVITY = -GRAVITY_MULTIPLIER * 9.82f;
    private static final float NO_DETAIL_SIZE = .5f;

    private static final float OFFSET_X = 1.316f;
    private static final float OFFSET_Y = -.347f;
    private static final float OFFSET_Z = 1.382f;

    private final @NonNull AudioPlayer audio_player;
    private final @NonNull AudioFile @NonNull [] hit_sounds;
    private final boolean hit;
    private final @NonNull Unit src;
    /** rendering offset */
    private final float deterministic_z;

    /** the target of the weapon. Mutable because rubber weapons bounce and change targets **/
    private @NonNull Selectable<?> target;
    private float start_x;
    private float start_y;
    private float end_x;
    private float end_y;
    private float dir_x;
    private float dir_y;
    private float time_limit;
    private float time;
    private float z_speed;

    /** absolute height in the world */
    private float current_z;

    public ThrowingWeapon(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target,
            @NonNull AudioFile throw_sound, @NonNull AudioFile @NonNull [] hit_sounds) {
        super(src.getOwner().getWorld());
        this.src = src;
        this.hit = hit;
        this.hit_sounds = hit_sounds;

        float x = src.getPositionX() + OFFSET_X * src.getDirectionX() - OFFSET_Y * src.getDirectionY();
        float y = src.getPositionY() + OFFSET_X * src.getDirectionY() - OFFSET_Y * src.getDirectionX();
        deterministic_z = OFFSET_Z + src.getMountOffset();
        current_z = getWorld().getHeightMap().getNearestHeight(x, y) + deterministic_z;

        setPosition(x, y, current_z - deterministic_z);

        setTarget(target);

        register();

        var params = new AudioParameters(throw_sound, AudioAssets.AUDIO_RANK_WEAPON_ATTACK,
                AudioAssets.AUDIO_DISTANCE_WEAPON_ATTACK, AudioAssets.AUDIO_GAIN_WEAPON_ATTACK,
                AudioAssets.AUDIO_RADIUS_WEAPON_ATTACK,
                ThreadLocalRandom.current().nextFloat(.9f, 1.1f));
        audio_player = getWorld().getAudio().newAudio(getPositionX(), getPositionY(), getPositionZ(), params);
        getWorld().getAnimationManagerGameTime().registerAnimation(this);

        // stats
        src.getOwner().weaponThrown();
    }

    public final @NonNull Unit getSrc() {
        return src;
    }

    public abstract @NonNull WeaponVisualType getWeaponVisualType();

    @Override
    protected @NonNull BoundingBox @Nullable [] getLocalBounds() {
        return null;
    }

    @Override
    public @NonNull String toString() {
        return "ThrowingWeapon: start_x = " + start_x + " | start_y = " + start_y + " | end_x = " + end_x
                + " | end_y = " + end_y + " | target = " + target + "  " + super.toString();
    }

    protected final void setTarget(@NonNull Selectable<?> target) {
        this.target = target;
        updateDirection();
        calcNumUpdatesAndZSpeed();
    }

    private void calcNumUpdatesAndZSpeed() {
        start_x = getPositionX();
        start_y = getPositionY();
        updateTarget();
        float dx = end_x - start_x;
        float dy = end_y - start_y;
        float len = (float) Math.hypot(dx, dy);
        time_limit = (len / getMetersPerSecond()) * getLoftFactor();
        time = 0;
        // current_z is already set to absolute start height
        float dest_z = getWorld().getHeightMap().getNearestHeight(end_x, end_y) + target.getHitOffsetZ();
        float dest_vec_z = dest_z - current_z;
        z_speed = (dest_vec_z) / time_limit - GRAVITY * time_limit / 2f;
    }

    protected abstract float getMetersPerSecond();

    protected abstract float getLoftFactor();

    private void updateTarget() {
        end_x = target.getPositionX();
        end_y = target.getPositionY();
    }

    private void updateDirection() {
        float dx = target.getPositionX() - getPositionX();
        float dy = target.getPositionY() - getPositionY();
        float len = (float) Math.max(Math.hypot(dx, dy), .01);
        float len_inv = 1f / len;
        dir_x = dx * len_inv;
        dir_y = dy * len_inv;
        setDirection(dir_x, dir_y);
    }

    @Override
    public final void updateChecksum(@NonNull StateChecksum checksum) {
        checksum.update(time);
    }

    @Override
    public final float getOffsetZ() {
        return deterministic_z;
    }

    @Override
    public void animate(float t) {
        if (time >= time_limit) {
            hitTarget(hit, getSrc().getOwner(), target);
            return;
        }

        if (hit) {
            updateTarget();
        }
        time += t;
        float progress = time / time_limit;

        float x;
        float y;
        if (progress < 1f) {
            x = start_x + (end_x - start_x) * progress;
            y = start_y + (end_y - start_y) * progress;
        } else {
            x = end_x;
            y = end_y;
        }

        current_z += z_speed * t;
        z_speed += GRAVITY * t;

        setPosition(x, y, current_z - deterministic_z);

        audio_player.setPosition(getPositionX(), getPositionY(), getPositionZ());
    }

    protected void hitTarget(boolean hit, @NonNull Player owner, @NonNull Selectable<?> target) {
        getWorld().getAnimationManagerGameTime().removeAnimation(this);
        audio_player.stop();
        remove();
        if (hit)
            damageTarget(target);
    }

    protected final void damageTarget(@NonNull Selectable<?> target) {
        if (target instanceof Unit unit) {
            float pitchRange = unit.getTemplate().getDeathPitch();
            var params = new AudioParameters(hit_sounds[ThreadLocalRandom.current().nextInt(
                    hit_sounds.length)],
                    AudioAssets.AUDIO_RANK_WEAPON_HIT,
                    AudioAssets.AUDIO_DISTANCE_WEAPON_HIT, AudioAssets.AUDIO_GAIN_WEAPON_HIT,
                    AudioAssets.AUDIO_RADIUS_WEAPON_HIT,
                    1f + (pitchRange > 0f ? ThreadLocalRandom.current().nextFloat(-0.5f * pitchRange, 0.5f * pitchRange)
                            : 0f));
            getWorld().getAudio().newAudio(target.getPositionX(), target.getPositionY(), target.getPositionZ(), params);
        }
        target.hit(getDamage(), dir_x, dir_y, getSrc().getOwner());
    }

    protected abstract int getDamage();

    public final float getZSpeed() {
        return z_speed;
    }

    @Override
    public final float getNoDetailSize() {
        return NO_DETAIL_SIZE;
    }
}
