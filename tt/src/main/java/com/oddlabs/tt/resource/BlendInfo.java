package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public abstract class BlendInfo {
    // Uploaded on first request so terrain generation never needs a GL context.
    private @Nullable Texture alpha_map;
    private final int format;
    private final @NonNull GLByteImage sourceImage;

    private @NonNull Texture createAlphaMap(@NonNull GLByteImage alpha_image, int format) {
        GLImage[] mipmaps = alpha_image.buildMipMaps(0, 1.0f, true, false);
        return new Texture(mipmaps, format, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    protected BlendInfo(@NonNull GLByteImage alpha_image, int format) {
        this.sourceImage = alpha_image;
        this.format = format;
    }

    public @NonNull Texture getAlphaMap() {
        if (alpha_map == null) {
            alpha_map = createAlphaMap(sourceImage, format);
        }
        return alpha_map;
    }

    public @NonNull GLByteImage getSourceImage() {
        return sourceImage;
    }

    /*	public void delete() {
            alpha_map.delete();
        }
    */
    protected final void bindAlpha() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, getAlphaMap().getHandle());
    }

    public abstract void setup();

    public abstract void reset();
}
