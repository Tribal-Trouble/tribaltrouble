package com.oddlabs.tt.render;

import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.util.GLUtils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Represents a 2D Array Texture (GL_TEXTURE_2D_ARRAY).
 */
public final class TextureArray extends Texture {
    private final int depth;

    public TextureArray(int width, int height, int depth, int internal_format, int min_filter, int mag_filter,
            int wrap) {
        super(GL30.GL_TEXTURE_2D_ARRAY, width, height, min_filter, mag_filter, wrap, wrap, 1000);
        this.depth = depth;

        int format = switch (internal_format) {
            case GL11.GL_RGB, GL11.GL_RGB8 -> GL11.GL_RGB;
            case GL11.GL_RED, GL30.GL_R8 -> GL11.GL_RED;
            default -> GL11.GL_RGBA;
        };

        // Pre-allocate storage for all mipmap levels to ensure we can build it level-by-level
        int numLevels = 1 + (int) Math.floor(Math.log(Math.max(width, height)) / Math.log(2));
        int w = width;
        int h = height;
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, getHandle());
        for (int level = 0; level < numLevels; level++) {
            GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, level, internal_format, w, h, depth, 0, format,
                    GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
        }
        GLUtils.checkAndThrow("TextureArray storage allocation");
    }

    /**
     * Builds the texture array from a map of slot indices to image mipmaps.
     * All layers for a given level are uploaded at once to avoid alignment issues with compressed formats.
     */
    public void build(@NonNull Map<@NonNull Integer, @NonNull GLImage[]> sources, int internal_format) {
        int numLevels = 1 + (int) Math.floor(Math.log(Math.max(getWidth(), getHeight())) / Math.log(2));

        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, getHandle());

        int format = GL11.GL_RGBA;
        int type = GL11.GL_UNSIGNED_BYTE;

        for (int level = 0; level < numLevels; level++) {
            int levelWidth = Math.max(1, getWidth() >> level);
            int levelHeight = Math.max(1, getHeight() >> level);

            // Stitch all layers for this level into one big buffer
            ByteBuffer levelBuffer = BufferUtils.createByteBuffer(levelWidth * levelHeight * depth * Integer.BYTES);

            for (int layer = 0; layer < depth; layer++) {
                GLImage[] slotMipmaps = sources.get(layer);
                assert slotMipmaps != null : "Missing source for layer " + layer;
                GLImage mip = slotMipmaps[level];
                assert mip.getWidth() == levelWidth && mip.getHeight() == levelHeight
                        : "Mipmap dimension mismatch at level " + level;

                ByteBuffer pixels = mip.getPixels();
                pixels.rewind();
                levelBuffer.put(pixels);
            }
            levelBuffer.flip();

            GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, level, internal_format, levelWidth, levelHeight, depth, 0,
                    format, type, levelBuffer);
        }
        GLUtils.checkAndThrow("TextureArray build");
    }

    @Override
    public int getDepth() {
        return depth;
    }
}
