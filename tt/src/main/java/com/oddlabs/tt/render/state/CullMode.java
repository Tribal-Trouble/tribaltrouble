package com.oddlabs.tt.render.state;

import org.lwjgl.opengl.GL11;

public enum CullMode implements Mode {
    NONE {
        @Override
        public void apply() {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
    },
    BACK {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
        }
    },
    FRONT {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_FRONT);
        }
    };
}
