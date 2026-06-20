package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.EmojiType;
import com.oddlabs.tt.model.ModelClient;
import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.resource.AudioAssets;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Logic for unit repairing behavior.
 * Manages units moving to damaged buildings and restoring their hit points.
 */
public final class RepairBehaviour implements Behaviour {
    private static final int REPAIRS_PER_SUPPLY = 5;
    private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;
    private final @NonNull Building building;
    private final @NonNull Unit unit;

    private float anim_time;
    private int repairs;
    private boolean sound;

    public RepairBehaviour(@NonNull Unit unit, @NonNull Building building) {
        this.unit = unit;
        this.building = building;
        unit.aimAtTarget(building);
        restartAnimation();
        unit.getSupplyContainer().increaseSupply(-1, SupplyType.WOOD);
        repairs = 0;
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
            unit.getOwner().getWorld().getAudio().newAudio(unit.getPositionX(), unit.getPositionY(), unit
                    .getPositionZ(), AudioAssets.getHarvestSound(SupplyType.WOOD));

            var selectedEmoji = ThreadLocalRandom.current().nextBoolean()
                    ? EmojiType.REPAIR_SAW : EmojiType.REPAIR_HAMMER;
            unit.getClientState(ModelClient.class).ifPresent(client -> {
                client.addVisualSound(selectedEmoji,
                        ModelClient.DURATION_REPAIR, AudioAssets.AUDIO_DISTANCE_HARVEST);
            });
        }

        if (anim_time > SECONDS_PER_ANIMATION_CYCLE) {
            restartAnimation();
            repairs++;
            if (building.isDead() || (building.isBuilt() && !building.isDamaged())) {
                return State.DONE;
            } else
                building.repair(1);
        }

        return repairs == REPAIRS_PER_SUPPLY ? State.DONE : State.INTERRUPTIBLE;
    }

    private void restartAnimation() {
        anim_time = 0;
        sound = false;
        unit.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
    }

    @Override
    public void forceInterrupted() {
    }
}
