package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class WeaponFactory {
    private static final float TERRAIN_MAX_BONUS = .25f;
    private static final float TERRAIN_BONUS_PER_HEIGHT = TERRAIN_MAX_BONUS / 20f;

    private final float hit_chance;
    private final float range;
    private final float release_ratio;

    protected WeaponFactory(float hit_chance, float range, float release_ratio) {
        this.hit_chance = hit_chance;
        this.range = range;
        this.release_ratio = release_ratio;
    }

    public final float getSecondsPerRelease(float anim_per_second) {
        return release_ratio / anim_per_second;
    }

    public final float getRange() {
        return range;
    }

    private static float computeTerrainBonus(@NonNull HeightMap heightmap, @NonNull Target src, @NonNull Target dst) {
        float src_z = heightmap.getNearestHeight(src.getPositionX(), src.getPositionY());
        float dst_z = heightmap.getNearestHeight(dst.getPositionX(), dst.getPositionY());
        float bonus = (src_z - dst_z) * TERRAIN_BONUS_PER_HEIGHT;
        bonus = Math.clamp(bonus, -TERRAIN_MAX_BONUS, TERRAIN_MAX_BONUS);
        return bonus;
    }

    public final void attack(@NonNull Unit src, @NonNull Selectable<?> target, float factor) {
        /* GAMEPLAY: Terrain bonus, according to who is positioned highest */
        float terrain_bonus = computeTerrainBonus(src.getOwner().getWorld().getHeightMap(), src, target);
        float difficulty_bonus = src.getOwner().getHitBonus();
        boolean hit = target.getOwner().getWorld().getRandom().nextFloat() < factor * (difficulty_bonus + terrain_bonus + hit_chance) * (1 - target.getDefenseChance());
        doAttack(hit, src, target);
    }

    public final void attack(@NonNull Unit src, @NonNull Selectable<?> target) {
        attack(src, target, 1f);
    }

    protected abstract void doAttack(boolean hit, @NonNull Unit src, @NonNull Selectable<?> target);

    public abstract @Nullable Class<? extends ThrowingWeapon> getType();
}
