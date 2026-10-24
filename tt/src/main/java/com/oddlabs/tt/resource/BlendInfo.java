package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

public abstract class BlendInfo {
    // Uploaded by the texture bake so terrain generation never needs a GL context.
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

    /** Uploads the blend textures. Needs a GL context. */
    public void createTextures() {
        alpha_map = createAlphaMap(sourceImage, format);
    }

    public @NonNull Texture getAlphaMap() {
        return Objects.requireNonNull(alpha_map, "Blend textures were not created by the texture bake");
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
