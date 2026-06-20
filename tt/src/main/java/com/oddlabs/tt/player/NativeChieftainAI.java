package com.oddlabs.tt.player;

import com.oddlabs.tt.model.MagicType;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import org.jspecify.annotations.NonNull;

import java.util.stream.StreamSupport;

/**
 * AI logic controller for Native chieftains, determining when to cast Poison Fog and Lightning Cloud.
 */
public final class NativeChieftainAI extends ChieftainAI {
    private static final int NUM_UNITS_FOR_LIGHTNING = 2;
    private static final int NUM_UNITS_FOR_POISON = 5;

    @Override
    public void decide(@NonNull Unit chieftain) {
        nodeLightningCloud(chieftain);
        nodePoisonFog(chieftain);
    }

    private void nodeLightningCloud(@NonNull Unit chieftain) {
        if (chieftain.getMagicProgress(MagicType.LIGHTNING_CLOUD) < 1)
            return;

        float hit_radius = 30f;
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());
        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius);
        if (num_enemy_units_close >= NUM_UNITS_FOR_LIGHTNING
                || (num_enemy_units < NUM_UNITS_FOR_LIGHTNING && num_enemy_units_close > 1)
                || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1)) {
            chieftain.doMagic(MagicType.LIGHTNING_CLOUD, false);
        }
    }

    private void nodePoisonFog(@NonNull Unit chieftain) {
        if (chieftain.getMagicProgress(MagicType.POISON_FOG) < 1)
            return;

        float hit_radius = chieftain.getOwner().getRaceInfo().getMagicFactory(MagicType.POISON_FOG)
                .getHitRadius();
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());
        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius);
        int num_friendly_units_close = getNumFriendlyUnitsClose(chieftain, hit_radius);
        if (2 * num_friendly_units_close < num_enemy_units_close
                && (num_enemy_units_close >= NUM_UNITS_FOR_POISON
                        || (num_enemy_units < NUM_UNITS_FOR_POISON && num_enemy_units_close > 1)
                        || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1))) {
            chieftain.doMagic(MagicType.POISON_FOG, false);
        }
    }

    private int getNumEnemyUnitsClose(@NonNull Unit chieftain, float hit_radius) {
        var filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain.getPositionY(), hit_radius, chieftain,
                Unit.class);
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        long num_enemy_units_close = StreamSupport.stream(filter.getResult().spliterator(), false)
                .filter(Selectable::isAlive)
                .filter(unit -> {
                    float dx = unit.getPositionX() - chieftain.getPositionX();
                    float dy = unit.getPositionY() - chieftain.getPositionY();
                    float squared_dist = dx * dx + dy * dy;
                    return chieftain.getOwner().isEnemy(unit.getOwner()) && squared_dist < hit_radius * hit_radius;
                }).count();
        return (int) num_enemy_units_close;
    }

    private int getNumFriendlyUnitsClose(@NonNull Unit chieftain, float hit_radius) {
        var filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain.getPositionY(), hit_radius, chieftain,
                Selectable.genericClass());
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        long num_friendly_units_close = StreamSupport.stream(filter.getResult().spliterator(), false)
                .filter(Selectable::isAlive)
                .filter(s -> {
                    float dx = s.getPositionX() - chieftain.getPositionX();
                    float dy = s.getPositionY() - chieftain.getPositionY();
                    float squared_dist = dx * dx + dy * dy;
                    return !chieftain.getOwner().isEnemy(s.getOwner()) && squared_dist < hit_radius * hit_radius;
                }).count();
        return (int) num_friendly_units_close;
    }
}
