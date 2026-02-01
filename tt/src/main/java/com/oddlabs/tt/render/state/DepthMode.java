package com.oddlabs.tt.render.state;

import org.lwjgl.opengl.GL11;

public enum DepthMode implements Mode {
    NONE {
        @Override
        public void apply() {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    },
    READ_ONLY {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
        }
    },
    READ_WRITE {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
        }
    };
}
