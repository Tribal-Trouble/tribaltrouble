package com.oddlabs.tt.resource;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL13;

public final class BlendLighting extends BlendInfo {

    private final Color.@NonNull Linear color;

    public BlendLighting(@NonNull GLByteImage alpha_image, @NonNull Color color) {
        super(alpha_image, Globals.COMPRESSED_LUMINANCE_FORMAT);
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
    }

    public @NonNull Color getColor() {
        return color;
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
