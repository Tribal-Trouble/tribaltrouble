package com.oddlabs.tt.resource;

import com.oddlabs.tt.global.Globals;
import org.lwjgl.opengl.GL13;

public final class BlendLighting extends BlendInfo {

    private final float r;
    private final float g;
    private final float b;

    public BlendLighting(GLByteImage alpha_image, float r, float g, float b) {
        super(alpha_image, Globals.COMPRESSED_LUMINANCE_FORMAT);
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void setup() {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        bindAlpha();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    @Override
    public void reset() {
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }
}
