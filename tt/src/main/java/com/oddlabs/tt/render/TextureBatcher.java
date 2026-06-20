package com.oddlabs.tt.render;

import com.oddlabs.tt.render.state.RenderContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Helper class for tracking texture slots in multi-texture batching renderers.
 */
public final class TextureBatcher {
    private final int maxSlots;
    private final IdentityHashMap<@NonNull Texture, @NonNull Integer> textureToSlot = new IdentityHashMap<>();
    private final @NonNull List<@NonNull Texture> activeTextures;

    public TextureBatcher(int maxSlots) {
        this.maxSlots = maxSlots;
        this.activeTextures = new ArrayList<>(maxSlots);
    }

    /**
     * Gets the current slot for a texture, or assigns a new one if available.
     * Returns -1 if the texture is not currently batched and the batch is full.
     */
    public int getOrAssignSlot(@NonNull Texture texture) {
        Integer slot = textureToSlot.get(texture);
        if (slot != null) {
            return slot;
        }
        if (activeTextures.size() >= maxSlots) {
            return -1; // Batch is full
        }
        slot = activeTextures.size();
        activeTextures.add(texture);
        textureToSlot.put(texture, slot);
        return slot;
    }

    /**
     * Binds all active textures to the provided RenderContext, starting at the specified offset unit.
     */
    public void bindTextures(@NonNull RenderContext context, int startUnit) {
        for (int i = 0; i < activeTextures.size(); i++) {
            context.setTexture(startUnit + i, activeTextures.get(i).getHandle());
        }
    }

    public void clear() {
        textureToSlot.clear();
        activeTextures.clear();
    }
}
