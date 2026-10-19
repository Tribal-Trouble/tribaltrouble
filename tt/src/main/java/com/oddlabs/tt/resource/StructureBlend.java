package com.oddlabs.tt.resource;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public final class StructureBlend extends BlendInfo {
    // Uploaded on first request so terrain generation never needs a GL context.
    private final @NonNull GLIntImage structure_image;
    private final @NonNull GLIntImage normal_image;
    private @Nullable Texture structure_map;
    private @Nullable Texture normal_map;

    private @NonNull Texture createStructureMap(GLIntImage structure_image) {
        return new Texture(new GLIntImage[]{structure_image}, GL11.GL_RGB, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    public StructureBlend(GLIntImage structure_image, GLIntImage normal_image, @NonNull GLByteImage alpha_image) {
        super(alpha_image, Globals.COMPRESSED_A_FORMAT);
        this.structure_image = structure_image;
        this.normal_image = normal_image;
    }

    public @NonNull Texture getStructureMap() {
        if (structure_map == null) {
            structure_map = createStructureMap(structure_image);
        }
        return structure_map;
    }

    public @NonNull Texture getNormalMap() {
        if (normal_map == null) {
            normal_map = createStructureMap(normal_image);
        }
        return normal_map;
    }

    private void bindStructure() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, getStructureMap().getHandle());
    }

    @Override
    public void setup() {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        bindAlpha();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        bindStructure();
    }

    @Override
    public void reset() {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

    }
}
