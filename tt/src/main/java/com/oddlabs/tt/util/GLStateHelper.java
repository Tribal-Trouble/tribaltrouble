package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * A utility for safely managing and restoring OpenGL state using a try-with-resources pattern.
 */
public final class GLStateHelper {

    private GLStateHelper() {
        // no instances
    }

    /**
     * An AutoCloseable resource for managing a specific GL capability (e.g., GL_BLEND).
     */
    private static class Capability implements GLState {
        private final int capability;
        private final boolean wasEnabled;

        Capability(int cap, boolean enable) {
            this.capability = cap;
            this.wasEnabled = GL11.glIsEnabled(cap);
            if (enable) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }

        @Override
        public void close() {
            if (wasEnabled) {
                GL11.glEnable(capability);
            } else {
                GL11.glDisable(capability);
            }
        }
    }

    public static @NonNull GLState blend(boolean enable) {
        return new Capability(GL11.GL_BLEND, enable);
    }

    public static @NonNull GLState cullFace(boolean enable) {
        return new Capability(GL11.GL_CULL_FACE, enable);
    }

    public static @NonNull GLState depthTest(boolean enable) {
        return new Capability(GL11.GL_DEPTH_TEST, enable);
    }

    /**
     * An AutoCloseable resource for managing the depth mask.
     */
    public static class DepthMask implements GLState {
        private final boolean wasEnabled;

        public DepthMask(boolean enable) {
            this.wasEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GL11.glDepthMask(enable);
        }

        @Override
        public void close() {
            GL11.glDepthMask(wasEnabled);
        }
    }
}
