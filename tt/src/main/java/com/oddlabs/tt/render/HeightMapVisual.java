package com.oddlabs.tt.render;

import com.oddlabs.tt.landscape.HeightMap;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Client-side visual representation of the height map.
 * Manages the OpenGL Texture state and propagates updates to the GPU.
 */
public final class HeightMapVisual implements HeightMap.ClientState {
    private final @NonNull Texture heightTexture;

    /**
     * Constructs the visual representation of the height map, creating the OpenGL texture.
     *
     * @param heightMap The simulation height map instance
     */
    public HeightMapVisual(@NonNull HeightMap heightMap) {
        this.heightTexture = new Texture(heightMap.getHeightData(),
                heightMap.getGridUnitsPerWorld(), heightMap.getGridUnitsPerWorld(),
                GL30.GL_R32F, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT);
    }

    /**
     * Returns the underlying OpenGL Texture instance.
     *
     * @return Non-null Texture instance
     */
    public @NonNull Texture getHeightTexture() {
        return heightTexture;
    }

    @Override
    public void editHeight(int x, int y, float height) {
        heightTexture.update(x, y, 1, 1, height);
    }
}
