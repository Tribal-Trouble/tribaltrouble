package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.SonicBlastEffect;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

public final class SonicBlast implements Magic {
    private final float hit_radius;
    private final float hit_chance_closest;
    private final float hit_chance_farthest;
    private final int damage_closest;
    private final int damage_farthest;
    private final float seconds;
    private final @NonNull Player owner;
    private final float start_x;
    private final float start_y;
    private final float start_z;
    private final @NonNull AbstractAudioPlayer lur;
    private final @NonNull AbstractAudioPlayer rumble;

    private float time = 0f;
    private final @NonNull Iterable<? extends Selectable<?>> blast_targets;
    private final @NonNull SonicBlastEffect sonicBlastEffect;

    private boolean first_ring_sent = false;

    public SonicBlast(float offset_x, float offset_y, float offset_z, float hit_radius, float hit_chance_closest,
            float hit_chance_farthest, int damage_closest, int damage_farthest, float seconds, @NonNull Unit src) {
        this.hit_radius = hit_radius;
        this.hit_chance_closest = hit_chance_closest;
        this.hit_chance_farthest = hit_chance_farthest;
        this.damage_closest = damage_closest;
        this.damage_farthest = damage_farthest;
        this.seconds = seconds;
        owner = src.getOwner();

        start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
        start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
        start_z = src.getPositionZ() + offset_z;

        var filter = new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), hit_radius, src,
                Selectable.genericClass());
        UnitGrid unit_grid = owner.getWorld().getUnitGrid();
        unit_grid.scan(filter, UnitGrid.toGridCoordinate(src.getPositionX()), UnitGrid.toGridCoordinate(
                src.getPositionY()));
        blast_targets = filter.getResult();

        sonicBlastEffect = new SonicBlastEffect(owner.getWorld(), new Vector3f(start_x, start_y, start_z), hit_radius,
                seconds);

        lur = owner.getWorld().getAudio().newAudio(new AudioParameters<>(
                owner.getWorld().getRacesResources().getBlastLurSound(owner.getWorld().getRandom()), start_x, start_y,
                start_z,
                AudioPlayer.AUDIO_RANK_MAGIC,
                AudioPlayer.AUDIO_DISTANCE_MAGIC,
                AudioPlayer.AUDIO_GAIN_BLAST_LUR,
                AudioPlayer.AUDIO_RADIUS_BLAST_LUR,
                1f));
        rumble = owner.getWorld().getAudio().newAudio(new AudioParameters<>(
                owner.getWorld().getRacesResources().getBlastRumbleSound(), start_x, start_y, start_z,
                AudioPlayer.AUDIO_RANK_MAGIC,
                AudioPlayer.AUDIO_DISTANCE_MAGIC,
                AudioPlayer.AUDIO_GAIN_BLAST_RUMBLE,
                AudioPlayer.AUDIO_RADIUS_BLAST_RUMBLE,
                1f));
    }

    @Override
    public void animate(float t) {
        time = Math.min(time + t, seconds);
        if (time >= seconds) {
            owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
        }

        if (!first_ring_sent) {
            first_ring_sent = true;

            owner.getWorld().getAudio().newAudio(new AudioParameters<>(
                    owner.getWorld().getRacesResources().getBlastBlastSound(), start_x, start_y, start_z,
                    AudioPlayer.AUDIO_RANK_MAGIC,
                    AudioPlayer.AUDIO_DISTANCE_MAGIC,
                    AudioPlayer.AUDIO_GAIN_BLAST_BLAST,
                    AudioPlayer.AUDIO_RADIUS_BLAST_BLAST,
                    1f));
            lur.stop(.3f, Settings.getSettings().sound_gain);
            rumble.stop(.2f, Settings.getSettings().sound_gain);
        }

        sonicBlastEffect.update(t);

        float current_radius = hit_radius * time / seconds;
        float squared_radius = current_radius * current_radius;

        var targets = blast_targets.iterator();
        while (targets.hasNext()) {
            var s = targets.next();
            float dx = s.getPositionX() - start_x;
            float dy = s.getPositionY() - start_y;
            float squared_dist = dx * dx + dy * dy;
            if (squared_dist < squared_radius) {
                if (!s.isDead()) {
                    float hit_chance = calculateValueFromCurrentRadius(current_radius, hit_chance_closest,
                            hit_chance_farthest);
                    if (owner.getWorld().getRandom().nextFloat() < hit_chance * (1 - s.getDefenseChance())) {
                        int damage = (int) calculateValueFromCurrentRadius(current_radius, damage_closest,
                                damage_farthest);
                        float inv_dist = 1f / ((float) Math.sqrt(squared_dist));
                        s.hit(damage, dx * inv_dist, dy * inv_dist, owner);
                    }
                }
                targets.remove();
            }
        }
    }

    private float calculateValueFromCurrentRadius(float current_radius, float max, float min) {
        float base_factor = 4f / 7f;
        float error = (float) Math.pow(base_factor, hit_radius);
        float factor = (float) Math.pow(base_factor, current_radius);
        float result = (max - min + error) * factor + min - error;
        return result;
    }

    @Override
    public void interrupt() {
        lur.stop(.2f, Settings.getSettings().sound_gain);
        rumble.stop(.2f, Settings.getSettings().sound_gain);
        sonicBlastEffect.abort();
    }
}
