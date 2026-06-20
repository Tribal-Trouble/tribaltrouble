package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.LandscapeBoundsProvider;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A rock boulder supply that erupts from the ground.
 */
public final class RockSupply extends SupplyModel {
    private static final float SPAWN_OFFSET_Z = -2f;
    private static final int INITIAL_SUPPLIES = 10;

    private static final Color.Linear COLOR_ERUPTION = new Color.Standard(0xFF_FF_CC_00).linear();
    private static final Color.Linear COLOR_COOLING = new Color.Standard(0xFF_FF_BE_94).linear();
    private static final Color.Linear COLOR_DECAL_COOLED = new Color.Standard(0.3f, 0.3f).linear();

    private Color.@Nullable Linear spawnColorTint = null;
    private Color.@Nullable Linear crackDecalColor = null;
    private float crackDecalOpacity = 0.0f;
    private float crackDecalDiameter = 0.0f;
    private float crackDecalPattern = 0.0f;

    public RockSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        var rotation = ThreadLocalRandom.current().nextFloat((float) -Math.PI, (float) Math.PI);
        var fragmentIndex = ThreadLocalRandom.current().nextInt(LandscapeBoundsProvider.SUPPLY_FRAGMENT_COUNT);
        super(world, 2f, grid_x, grid_y, x, y, SPAWN_OFFSET_Z, rotation, INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getRockBounds(fragmentIndex));
    }

    @Override
    public @NonNull SupplyType getSupplyType() {
        return SupplyType.ROCK;
    }

    @Override
    public @NonNull Supply respawn() {
        return new RockSupply(getWorld(), getGridX(), getGridY(), getPositionX(), getPositionY(), false);
    }


    @Override
    public float getSpawnTime() {
        return 6.0f;
    }

    @Override
    public Color.@Nullable Linear getSpawnColorTint() {
        return spawnColorTint;
    }

    @Override
    public Color.@Nullable Linear getCrackDecalColor() {
        return crackDecalColor;
    }

    @Override
    public float getCrackDecalOpacity() {
        return crackDecalOpacity;
    }

    @Override
    public float getCrackDecalDiameter() {
        return crackDecalDiameter;
    }

    @Override
    public float getCrackDecalPattern() {
        return crackDecalPattern;
    }

    @Override
    public float getOffsetZ() {
        float slope = getSlopeOffset();
        if (isSpawning()) {
            float progress = getSpawnProgress();
            if (progress < 0.3f) return SPAWN_OFFSET_Z + slope;
            if (progress < 0.7f) {
                float riseProgress = (progress - 0.3f) / 0.4f;
                return (1.0f - riseProgress) * SPAWN_OFFSET_Z + slope;
            }
        }
        return slope;
    }

    @Override
    public float getShadowOpacity() {
        if (isSpawning()) {
            float progress = getSpawnProgress();
            if (progress < 0.7f) {
                return 0.0f;
            } else {
                float coolProgress = (progress - 0.7f) / 0.3f;
                return super.getShadowOpacity() * coolProgress;
            }
        }
        return super.getShadowOpacity();
    }

    @Override
    public void animateSpawn(float t, float progress) {
        super.animateSpawn(t, progress);
        if (progress < 0.3f) {
            // Phase 1: Rumble/Rise - Start Whispy White
            float progressRatio = progress / 0.3f;
            crackDecalOpacity = progressRatio;
            crackDecalDiameter = getSize() * 2.0f;
            crackDecalPattern = 10.0f + 0.5f * progressRatio;
            crackDecalColor = Color.Linear.WHITE;
            spawnColorTint = null;
        } else if (progress < 0.7f) {
            // Phase 2: Eruption - Mixture of Black and Sooty
            crackDecalOpacity = 1.0f;
            crackDecalDiameter = getSize() * 2.0f;
            crackDecalPattern = 10.5f;
            crackDecalColor = Color.Linear.WHITE;
            spawnColorTint = COLOR_ERUPTION;
        } else {
            // Phase 3: Dissipating - Back to Whispy White
            setShowShadow(true);

            float coolProgress = (progress - 0.7f) / 0.3f;
            setShowShadow(true);
            crackDecalOpacity = 1.0f - coolProgress;
            crackDecalPattern = 10.5f;
            // Fade from white (1.0) to grey (0.3)
            crackDecalColor = Color.Linear.WHITE.lerp(COLOR_DECAL_COOLED, coolProgress);

            if (coolProgress < 0.5f) {
                float factor = coolProgress / 0.5f;
                spawnColorTint = COLOR_ERUPTION.lerp(COLOR_COOLING, factor);
            } else {
                float factor = (coolProgress - 0.5f) / 0.5f;
                spawnColorTint = COLOR_COOLING.lerp(Color.Linear.WHITE, factor);
            }
        }
        reinsert();
    }

    @Override
    public void spawnComplete() {
        super.spawnComplete();
        spawnColorTint = null;
        crackDecalColor = null;
        crackDecalOpacity = 0.0f;
        crackDecalDiameter = 0.0f;
        crackDecalPattern = 0.0f;
        reinsert();
    }

    @Override
    public void remove() {
        super.remove();
        getClientState(ModelClient.class).ifPresent(ModelClient::close);
    }
}
