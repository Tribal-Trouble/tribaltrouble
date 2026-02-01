package com.oddlabs.tt.render.state;

import org.lwjgl.opengl.GL11;

public enum BlendMode implements Mode {
    NONE {
        @Override
        public void apply() {
            GL11.glDisable(GL11.GL_BLEND);
        }
    },
    ALPHA {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    },
    ADDITIVE {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        }
    },
    PREMULTIPLIED {
        @Override
        public void apply() {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    },
    CUSTOM {
        @Override
        public void apply() {
            // Managed manually via setBlendFunc
        }
    };
}
