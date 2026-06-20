package com.oddlabs.tt.render;

import com.oddlabs.tt.model.EmojiType;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.ModelClient;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the client-side visual state (accessories) for a simulation model.
 */
public final class VisualModel implements ModelClient {
    private final @NonNull Model model;
    private final @NonNull List<@NonNull Accessory> accessories = new ArrayList<>();

    public VisualModel(@NonNull Model model) {
        this.model = model;
    }

    public @NonNull List<@NonNull Accessory> getAccessories() {
        return accessories;
    }

    @Override
    public void update(float t) {
        boolean hasExpired = false;
        for (Accessory acc : accessories) {
            if (acc instanceof AnimatedAccessory animated) {
                animated.animate(t);
            }
            if (acc.isExpired()) {
                hasExpired = true;
            }
        }
        if (hasExpired) {
            for (Accessory acc : accessories) {
                if (acc.isExpired()) {
                    acc.close();
                }
            }
            accessories.removeIf(Accessory::isExpired);
        }
    }

    @Override
    public void close() {
        for (Accessory acc : accessories) {
            acc.close();
        }
        accessories.clear();
    }

    @Override
    public void addVisualSound(@NonNull EmojiType emoji, float duration, float audioDistance) {
        VisualRegistry.getInstance().getEmojiSprite(emoji)
                .map(sprite -> new VisualSoundAccessory(sprite, duration, audioDistance))
                .ifPresent(accessories::add);
    }

    @Override
    public void addLightningStrike(float targetX, float targetY, float targetZ) {
        for (Accessory acc : accessories) {
            if (acc instanceof LightningCloudVisualAccessory cloudAcc) {
                cloudAcc.triggerStrike(targetX, targetY, targetZ);
            }
        }
    }
}
