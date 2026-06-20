package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.LandscapeBoundsProvider;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * An iron boulder supply that falls from the sky as a meteor.
 */
public final class IronSupply extends SupplyModel {
    private static final int INITIAL_SUPPLIES = 10;
    private static final float SPAWN_OFFSET_Z = 200.0f;
    private static final float FALL_DURATION_RATIO = 0.12f;
    private static final float TRAIL_OFFSET_Z = 10.0f;

    private static final Color.Linear COLOR_FALLING = new Color.Linear(2.0f, 0.5f, 0.05f, 1.0f);
    private static final Color.Linear COLOR_LANDING = new Color.Linear(2.0f, 1.0f, 0.2f, 1.0f); // Overdriven
    private static final Color.Linear COLOR_HOT = new Color.Linear(2.0f, 0.2f, 0.1f, 1.0f); // Overdriven
    private static final Color.Linear COLOR_WHITE_HOT = new Color.Linear(2.0f, 2.0f, 2.0f, 1.0f); // Overdriven
    private static final Color.Linear COLOR_COOLING = new Color.Standard(0xFF_6B_6B_7A).linear();
    private static final Color.Linear COLOR_DECAL_COOLED = Color.Linear.BLACK;
    private static final float SMOKE_PARTICLES_PER_SECOND = 30.0f; // Lower density

    private Color.@Nullable Linear spawnColorTint = COLOR_FALLING;
    private Color.@Nullable Linear crackDecalColor = null;
    private float crackDecalOpacity = 0.0f;
    private float crackDecalDiameter = 0.0f;
    private float crackDecalPattern = 0.0f;
    private final int fragmentIndex;
    private boolean landed = false;
    private boolean cooling = false;
    private boolean useRockTexture = true;

    public IronSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase) {
        var fragmentIndex = ThreadLocalRandom.current().nextInt(LandscapeBoundsProvider.SUPPLY_FRAGMENT_COUNT);
        this(world, grid_x, grid_y, x, y, increase, fragmentIndex);
    }

    private IronSupply(@NonNull World world, int grid_x, int grid_y, float x, float y, boolean increase,
            int fragmentIndex) {
        super(world, 2f, grid_x, grid_y, x, y, SPAWN_OFFSET_Z, ThreadLocalRandom.current().nextFloat((float) -Math.PI,
                (float) Math.PI), INITIAL_SUPPLIES, increase,
                world.getLandscapeResources().getIronBounds(fragmentIndex));
        this.fragmentIndex = fragmentIndex;
    }

    @Override
    public @NonNull BoundsProvider getBoundsProvider() {
        // Use rock texture while hot to show tinting
        return useRockTexture ? getWorld().getLandscapeResources().getRockBounds(fragmentIndex)
                : super.getBoundsProvider();
    }

    @Override
    public @NonNull SupplyType getSupplyType() {
        return SupplyType.IRON;
    }

    @Override
    public @NonNull Supply respawn() {
        return new IronSupply(getWorld(), getGridX(), getGridY(), getPositionX(), getPositionY(), false);
    }

    @Override
    public float getSpawnTime() {
        return 4.5f;
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
            float fallProgress = Math.min(1.0f, getSpawnProgress() / FALL_DURATION_RATIO);
            return (1.0f - fallProgress) * SPAWN_OFFSET_Z + slope;
        }
        return slope;
    }

    @Override
    public void animateSpawn(float t, float progress) {
        super.animateSpawn(t, progress);
        if (progress < FALL_DURATION_RATIO) {
            // falling
            spawnColorTint = COLOR_FALLING;
        } else {
            if (!landed) {
                landed = true;
                setShowShadow(true);
            }

            // Cracks pulse logic (fading out smoothly)
            float crackDuration = 0.6f;
            if (progress >= FALL_DURATION_RATIO && progress < FALL_DURATION_RATIO + crackDuration) {
                float crackProgress = (progress - FALL_DURATION_RATIO) / crackDuration;
                crackDecalOpacity = 1.0f - crackProgress;
                crackDecalDiameter = getSize() * 2.5f; // Matched RockSupply
                crackDecalPattern = 10.5f;
                // Fade color from White to Cooled (Black)
                crackDecalColor = Color.Linear.WHITE.lerp(COLOR_DECAL_COOLED, crackProgress);
            } else {
                crackDecalOpacity = 0.0f;
                crackDecalColor = null;
            }

            float coolProgress = Math.min(1.0f, (progress - FALL_DURATION_RATIO) / (0.85f - FALL_DURATION_RATIO));

            if (!cooling) {
                cooling = true;
            }

            if (coolProgress < 0.3f) {
                float factor = coolProgress / 0.3f;
                spawnColorTint = COLOR_FALLING.lerp(COLOR_LANDING, factor);
            } else if (coolProgress < 0.6f) {
                float factor = (coolProgress - 0.3f) / 0.3f;
                spawnColorTint = COLOR_LANDING.lerp(COLOR_HOT, factor);
            } else if (coolProgress < 0.8f) {
                float factor = (coolProgress - 0.6f) / 0.2f;
                spawnColorTint = COLOR_HOT.lerp(COLOR_COOLING, factor);
            } else if (coolProgress < 0.9f) {
                float factor = (coolProgress - 0.8f) / 0.1f;
                spawnColorTint = COLOR_COOLING.lerp(COLOR_COOLING.mul(0.35f), factor);
            } else if (coolProgress < 1.0f) {
                useRockTexture = false; // Transition to iron texture as we hit cool grey
                float factor = (coolProgress - 0.9f) / 0.1f;
                Color.Linear ironStartTint = COLOR_COOLING.mul(0.9f);
                spawnColorTint = ironStartTint.lerp(Color.Linear.WHITE, factor);
            } else {
                spawnColorTint = null;
            }
        }
    }


    @Override
    public void spawnComplete() {
        super.spawnComplete();

        spawnColorTint = null;
        crackDecalColor = null;
        crackDecalOpacity = 0.0f;
        crackDecalDiameter = 0.0f;
        crackDecalPattern = 0.0f;
        landed = false;
        useRockTexture = false;
        cooling = false;
        reinsert();
    }

    @Override
    public void remove() {
        super.remove();
        getClientState(ModelClient.class).ifPresent(ModelClient::close);
    }
}
