package com.oddlabs.tt.resource;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.util.GLState;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public final strictfp class StructureBlend extends BlendInfo {
    private final Texture structure_map;

    private final Texture createStructureMap(GLIntImage structure_image) {
        return new Texture(
                new GLIntImage[] {structure_image},
                GL11.GL_RGB,
                GL11.GL_LINEAR,
                GL11.GL_LINEAR,
                GL11.GL_REPEAT,
                GL11.GL_REPEAT);
    }

    public StructureBlend(GLIntImage structure_image, GLByteImage alpha_image) {
        super(alpha_image, Globals.COMPRESSED_A_FORMAT);
        structure_map = createStructureMap(structure_image);
    }

    private final void bindStructure() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, structure_map.getHandle());
    }

    public final void setup() {
        // Unit1: alpha map contributes fragment alpha only; preserve incoming RGB
        GLState.activeTexture(GL13.GL_TEXTURE1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        bindAlpha();
        // Route: RGB = PREVIOUS (from unit0), ALPHA = TEXTURE (alpha map)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL13.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_PREVIOUS);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL13.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL13.GL_TEXTURE);

        // Unit0: structure color
        GLState.activeTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        bindStructure();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public final void reset() {
        GLState.activeTexture(GL13.GL_TEXTURE1);
        // Restore default env mode for safety
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GLState.activeTexture(GL13.GL_TEXTURE0);
    }
}
