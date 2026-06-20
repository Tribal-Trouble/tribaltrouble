package com.oddlabs.tt.render.state;

import org.lwjgl.opengl.GL11;

public enum CullMode implements Mode {
    NONE {
        @Override
        public void apply(RenderContext context) {
            context.setCullFace(false);
        }
    },
    BACK {
        @Override
        public void apply(RenderContext context) {
            context.setCullFace(true);
            context.setCullFaceMode(GL11.GL_BACK);
        }
    },
    FRONT {
        @Override
        public void apply(RenderContext context) {
            context.setCullFace(true);
            context.setCullFaceMode(GL11.GL_FRONT);
        }
    };
}
