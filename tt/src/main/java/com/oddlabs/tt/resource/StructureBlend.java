package com.oddlabs.tt.resource;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.Objects;

public final class StructureBlend extends BlendInfo {
    private static final String NOT_CREATED = "Structure textures were not created by the texture bake";

    // Uploaded by the texture bake so terrain generation never needs a GL context.
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

    @Override
    public void createTextures() {
        super.createTextures();
        structure_map = createStructureMap(structure_image);
        normal_map = createStructureMap(normal_image);
    }

    public @NonNull Texture getStructureMap() {
        return Objects.requireNonNull(structure_map, NOT_CREATED);
    }

    public @NonNull Texture getNormalMap() {
        return Objects.requireNonNull(normal_map, NOT_CREATED);
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
