package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.EmojiType;
import com.oddlabs.tt.model.ModelClient;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.resource.AudioAssets;
import org.jspecify.annotations.NonNull;

/**
 * Logic for unit harvesting behavior.
 * Manages finding harvestable resources, moving to them, and returning resources to a collection point.
 */
public final class HarvestBehaviour implements Behaviour {
    private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;
    private final @NonNull Supply supply;
    private final @NonNull Unit unit;
    private float anim_time;
    private boolean sound;

    public HarvestBehaviour(@NonNull Unit unit, @NonNull Supply supply) {
        this.unit = unit;
        this.supply = supply;
        unit.aimAtTarget(supply);
        restartAnimation();
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public @NonNull State animate(float t) {
        anim_time += t;
        if (anim_time > unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ANIMATION_CYCLE) && !sound) {
            sound = true;
            var params = AudioAssets.getHarvestSound(supply.getSupplyType());
            unit.getOwner().getWorld().getAudio().newAudio(unit.getPositionX(), unit.getPositionY(), unit
                    .getPositionZ(), params);

            unit.getClientState(ModelClient.class).ifPresent(client -> {
                client.addVisualSound(EmojiType.fromSupply(supply.getSupplyType()),
                        ModelClient.DURATION_HARVEST, AudioAssets.AUDIO_DISTANCE_HARVEST);
            });

            if (supply.hit()) {
                unit.getSupplyContainer().increaseSupply(1, supply.getSupplyType());
                unit.getOwner().harvested(supply.getSupplyType());
            }
        }

        if (anim_time > SECONDS_PER_ANIMATION_CYCLE) {
            restartAnimation();
            if (unit.getSupplyContainer().isSupplyFull() || supply.isEmpty())
                return State.DONE;
        }

        return State.INTERRUPTIBLE;
    }

    private void restartAnimation() {
        unit.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
        anim_time = 0;
        sound = false;
    }

    @Override
    public void forceInterrupted() {
    }
}
