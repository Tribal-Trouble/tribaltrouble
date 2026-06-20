package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Unit;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An accessory representing a temporary visual alert for in-game sounds.
 */
public final class VisualSoundAccessory implements AnimatedAccessory {

    private static final float DRIFT_HEIGHT = 0.8f;
    private static final float BASE_Z_OFFSET = 3.5f;
    private static final float FADEOUT_DURATION = 0.25f;

    private final @NonNull SpriteKey emojiSprite;
    private final float duration;
    private final float maxDistance;
    private final @Nullable Vector3fc customOffset;
    private float age;

    public VisualSoundAccessory(@NonNull SpriteKey emojiSprite, float duration, float maxDistance) {
        this(emojiSprite, duration, maxDistance, null);
    }

    public VisualSoundAccessory(@NonNull SpriteKey emojiSprite, float duration, float maxDistance,
            @Nullable Vector3fc customOffset) {
        this.emojiSprite = emojiSprite;
        this.duration = duration;
        this.maxDistance = maxDistance;
        this.customOffset = customOffset;
        this.age = 0f;
    }

    @Override
    public void animate(float t) {
        age += t;
    }

    @Override
    public boolean isVisible(@NonNull Model parent, @NonNull CameraState camera) {
        if (!Renderer.getRenderer().getSettings().sound_emojis) {
            return false;
        }
        if (age >= duration) {
            return false;
        }

        float x = parent.getPositionX();
        float y = parent.getPositionY();
        float z = parent.getPositionZ();

        float mountOffset = 0.0f;
        if (parent instanceof Unit unit) {
            if (unit.isRegistered() && !unit.isDead()) {
                mountOffset = unit.getMountOffset();
            }
        }
        float drift = DRIFT_HEIGHT * (age / duration);
        z += BASE_Z_OFFSET + mountOffset + drift;

        if (customOffset != null) {
            x += customOffset.x();
            y += customOffset.y();
            z += customOffset.z();
        }

        float dx = camera.getCurrentX() - x;
        float dy = camera.getCurrentY() - y;
        float dz = camera.getCurrentZ() - z;
        float distSq = dx * dx + dy * dy + dz * dz;

        return distSq <= maxDistance * maxDistance;
    }

    @Override
    public boolean isExpired() {
        return age >= duration;
    }

    @Override
    public void getRelativeTransform(@NonNull Matrix4f dest, @NonNull Model parent) {
        float mountOffset = 0.0f;
        if (parent instanceof Unit unit) {
            if (unit.isRegistered() && !unit.isDead()) {
                mountOffset = unit.getMountOffset();
            }
        }

        float drift = DRIFT_HEIGHT * (age / duration);
        dest.translate(0f, 0f, BASE_Z_OFFSET + mountOffset + drift);
        if (customOffset != null) {
            dest.translate(customOffset);
        }

        if (duration - age < FADEOUT_DURATION) {
            float t = (duration - age) / FADEOUT_DURATION;
            dest.scale(Math.clamp(t, 0.0f, 1.0f));
        }
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return emojiSprite;
    }

    public float getAlpha() {
        if (duration - age < FADEOUT_DURATION) {
            float t = (duration - age) / FADEOUT_DURATION;
            return Math.clamp(t, 0.0f, 1.0f);
        }
        return 1.0f;
    }
}
