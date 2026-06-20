package com.oddlabs.tt.render;

import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.weapon.SonicBlast;
import com.oddlabs.tt.resource.AudioAssets;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Client-side visual accessory for the sonic blast spell.
 * Manages the cast charge-up and release audio effects entirely on the client.
 */
public final class SonicBlastVisualAccessory implements AnimatedAccessory {
    private final @NonNull SonicBlast blast;
    private final @NonNull AudioPlayer lur;
    private final @NonNull AudioPlayer rumble;

    private boolean firstRingSent = false;

    public SonicBlastVisualAccessory(@NonNull SonicBlast blast) {
        this.blast = blast;
        World world = blast.getWorld();
        this.lur = world.getAudio().newAudio(blast.getPositionX(), blast.getPositionY(), blast.getPositionZ(),
                AudioAssets.SONIC_BLAST_LUR[world.getRandom().nextInt(AudioAssets.SONIC_BLAST_LUR.length)]);
        this.rumble = world.getAudio().newAudio(blast.getPositionX(), blast.getPositionY(), blast.getPositionZ(),
                AudioAssets.SONIC_BLAST_RUMBLE);
    }

    @Override
    public void animate(float t) {
        if (!firstRingSent) {
            firstRingSent = true;
            blast.getWorld().getAudio().newAudio(blast.getPositionX(), blast.getPositionY(), blast.getPositionZ(),
                    AudioAssets.SONIC_BLAST);
            lur.stop(10.0f);
            rumble.stop(15.0f);
        }
    }

    @Override
    public boolean isExpired() {
        return blast.isDead();
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        return !blast.isDead();
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    @Override
    public void close() {
        if (lur != null) {
            lur.stop(15.0f);
        }
        if (rumble != null) {
            rumble.stop(15.0f);
        }
    }
}
