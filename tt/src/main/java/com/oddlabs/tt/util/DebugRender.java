package com.oddlabs.tt.util;

import com.oddlabs.tt.render.shader.DebugShaderRenderer;
import com.oddlabs.util.Color;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

/**
 * Utilities for rendering debug shapes (boxes, lines, spheres, etc.) using a {@link DebugShaderRenderer}.
 */
public final class DebugRender {
    private static final Vector4fc AXIS_X_COLOR = Color.argb4v(0xFF_FF_00_00);
    private static final Vector4fc AXIS_Y_COLOR = Color.argb4v(0xFF_00_FF_00);
    private static final Vector4fc AXIS_Z_COLOR = Color.argb4v(0xFF_00_00_FF);

    public static final Vector4fc[] debug_colors = {
            Color.argb4v(0xFF_7f_1f_1f), Color.argb4v(0xFF_7f_1f_00), Color.argb4v(0xFF_7f_00_1f), Color.argb4v(0xFF_3f_7f_00),
            Color.argb4v(0xFF_00_1f_1f), Color.argb4v(0xFF_00_1f_00), Color.argb4v(0xFF_00_00_1f), Color.argb4v(0xFF_00_00_00),
            Color.argb4v(0xFF_00_5f_5f), Color.argb4v(0xFF_00_5f_00), Color.argb4v(0xFF_00_00_5f), Color.argb4v(0xFF_5f_8f_8f),
            Color.argb4v(0xFF_3f_5f_1f), Color.argb4v(0xFF_5f_5f_8f), Color.argb4v(0xFF_3f_2f_5f), Color.argb4v(0xFF_3f_3f_3f),
            Color.argb4v(0xFF_5f_1f_1f), Color.argb4v(0xFF_5f_1f_5f), Color.argb4v(0xFF_5f_5f_1f), Color.argb4v(0xFF_5f_5f_5f)
    };
    private static final float CIRCLE_DELTA = (float) java.lang.Math.PI / 2;
    private static final float ANGLE_DELTA = (float) java.lang.Math.PI / 20;
    private static final float SUBDIV = 0.4f;

    private static @Nullable DebugShaderRenderer shaderRenderer;
    private static final FloatList linesBuffer = new FloatList(1024);
    private static final FloatList pointsBuffer = new FloatList(128);
    private static float currentPointSize = 1.0f;

    private DebugRender() {
    }

    /**
     * Sets the {@link DebugShaderRenderer} to be used for drawing debug shapes.
     *
     * @param renderer The renderer to use, or {@code null} to disable debug rendering.
     */
    public static void setShaderRenderer(@Nullable DebugShaderRenderer renderer) {
        shaderRenderer = renderer;
    }

    /**
     * Flushes the batched debug primitives to the renderer.
     * Should be called at the end of the debug rendering phase.
     */
    public static void flush() {
        if (shaderRenderer == null) {
            linesBuffer.clear();
            pointsBuffer.clear();
            return;
        }

        if (!linesBuffer.isEmpty()) {
            shaderRenderer.begin(GL11.GL_LINES);
            try {
                for (int i = 0; i < linesBuffer.size(); i += 6) {
                    shaderRenderer.vertex(
                            linesBuffer.get(i), linesBuffer.get(i + 1), linesBuffer.get(i + 2),
                            linesBuffer.get(i + 3), linesBuffer.get(i + 4), linesBuffer.get(i + 5)
                    );
                }
            } finally {
                shaderRenderer.end();
            }
            linesBuffer.clear();
        }

        if (!pointsBuffer.isEmpty()) {
            shaderRenderer.setPointSize(currentPointSize);
            shaderRenderer.begin(GL11.GL_POINTS);
            try {
                for (int i = 0; i < pointsBuffer.size(); i += 6) {
                    shaderRenderer.vertex(
                            pointsBuffer.get(i), pointsBuffer.get(i + 1), pointsBuffer.get(i + 2),
                            pointsBuffer.get(i + 3), pointsBuffer.get(i + 4), pointsBuffer.get(i + 5)
                    );
                }
            } finally {
                shaderRenderer.end();
            }
            pointsBuffer.clear();
        }
    }

    /**
     * Draws a wireframe box.
     */
    public static void drawBox(float bmin_x, float bmax_x, float bmin_y, float bmax_y, float bmin_z, float bmax_z, float r, float g, float b) {
        if (null == shaderRenderer) return;

        // Bottom face
        drawLine(bmin_x, bmin_y, bmin_z, bmax_x, bmin_y, bmin_z, r, g, b);
        drawLine(bmax_x, bmin_y, bmin_z, bmax_x, bmax_y, bmin_z, r, g, b);
        drawLine(bmax_x, bmax_y, bmin_z, bmin_x, bmax_y, bmin_z, r, g, b);
        drawLine(bmin_x, bmax_y, bmin_z, bmin_x, bmin_y, bmin_z, r, g, b);

        // Top face
        drawLine(bmin_x, bmin_y, bmax_z, bmax_x, bmin_y, bmax_z, r, g, b);
        drawLine(bmax_x, bmin_y, bmax_z, bmax_x, bmax_y, bmax_z, r, g, b);
        drawLine(bmax_x, bmax_y, bmax_z, bmin_x, bmax_y, bmax_z, r, g, b);
        drawLine(bmin_x, bmax_y, bmax_z, bmin_x, bmin_y, bmax_z, r, g, b);

        // Vertical connectors
        drawLine(bmin_x, bmin_y, bmin_z, bmin_x, bmin_y, bmax_z, r, g, b);
        drawLine(bmax_x, bmin_y, bmin_z, bmax_x, bmin_y, bmax_z, r, g, b);
        drawLine(bmax_x, bmax_y, bmin_z, bmax_x, bmax_y, bmax_z, r, g, b);
        drawLine(bmin_x, bmax_y, bmin_z, bmin_x, bmax_y, bmax_z, r, g, b);
    }

    /**
     * Draws a point.
     */
    public static void drawPoint(float x, float y, float z, float size, float r, float g, float b) {
        if (null == shaderRenderer) return;
        currentPointSize = size;

        pointsBuffer.add(x);
        pointsBuffer.add(y);
        pointsBuffer.add(z);
        pointsBuffer.add(r);
        pointsBuffer.add(g);
        pointsBuffer.add(b);
    }

    /**
     * Draws a line segment.
     */
    public static void drawLine(float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b) {
        if (null == shaderRenderer) return;
        linesBuffer.add(x1);
        linesBuffer.add(y1);
        linesBuffer.add(z1);
        linesBuffer.add(r);
        linesBuffer.add(g);
        linesBuffer.add(b);

        linesBuffer.add(x2);
        linesBuffer.add(y2);
        linesBuffer.add(z2);
        linesBuffer.add(r);
        linesBuffer.add(g);
        linesBuffer.add(b);
    }

    /**
     * Draws the classic OpenGL axes at ground level at the center of the map
     */
    public static void drawAxes(float center, float z) {
        if (null == shaderRenderer) return;
        // X axis - red
        drawLine(center, center, z, center + 10, center, z, AXIS_X_COLOR.x(), AXIS_X_COLOR.y(), AXIS_X_COLOR.z());
        // Y axis - green
        drawLine(center, center, z, center, center + 10, z, AXIS_Y_COLOR.x(), AXIS_Y_COLOR.y(), AXIS_Y_COLOR.z());
        // Z axis - blue
        drawLine(center, center, z, center, center, z + 10, AXIS_Z_COLOR.x(), AXIS_Z_COLOR.y(), AXIS_Z_COLOR.z());
    }

    /**
     * Draws a wireframe quad.
     */
    public static void drawQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float z, float r, float g, float b) {
        if (null == shaderRenderer) return;
        drawLine(x1, y1, z, x2, y2, z, r, g, b);
        drawLine(x2, y2, z, x3, y3, z, r, g, b);
        drawLine(x3, y3, z, x4, y4, z, r, g, b);
        drawLine(x4, y4, z, x1, y1, z, r, g, b);
    }

    /**
     * Draws a wireframe cylinder composed of multiple circles.
     */
    public static void drawCylinder(float origin_x, float origin_y, float origin_z, float radius, int num_circles, float r, float g, float b) {
        if (null == shaderRenderer) return;
        float z = 0f;
        for (int i = 0; i < num_circles; i++) {
            drawCircle(radius, origin_x, origin_y, origin_z + z, r, g, b);
            z += SUBDIV;
        }
    }

    /**
     * Draws a wireframe circle.
     */
    private static void drawCircle(float radius, float origin_x, float origin_y, float origin_z, float r, float g, float b) {
        if (null == shaderRenderer) return;
        float prevX = radius * (float) java.lang.Math.cos(0) + origin_x;
        float prevY = radius * (float) java.lang.Math.sin(0) + origin_y;
        float startX = prevX;
        float startY = prevY;

        for (float phi = ANGLE_DELTA; phi < (float) java.lang.Math.PI * 2; phi += ANGLE_DELTA) {
            float x = radius * (float) java.lang.Math.cos(phi) + origin_x;
            float y = radius * (float) java.lang.Math.sin(phi) + origin_y;
            drawLine(prevX, prevY, origin_z, x, y, origin_z, r, g, b);
            prevX = x;
            prevY = y;
        }
        drawLine(prevX, prevY, origin_z, startX, startY, origin_z, r, g, b);
    }

    /**
     * Draws a wireframe sphere.
     */
    public static void drawSphere(float origin_x, float origin_y, float origin_z, float radius, float r, float g, float b) {
        if (null == shaderRenderer) return;
        for (float phi = 0; phi < (float) java.lang.Math.PI; phi += CIRCLE_DELTA) {
            float prevX = 0, prevY = 0, prevZ = 0;
            boolean first = true;
            float startX = 0, startY = 0, startZ = 0;

            for (float rho = 0f; rho < (float) java.lang.Math.PI * 2; rho += ANGLE_DELTA) {
                float x_local = radius * (float) java.lang.Math.cos(rho);
                float z_local = radius * (float) java.lang.Math.sin(rho);
                float y = x_local * (float) java.lang.Math.sin(phi);
                float x = x_local * (float) java.lang.Math.cos(phi);

                float worldX = x + origin_x;
                float worldY = y + origin_y;
                float worldZ = z_local + origin_z;

                if (!first) {
                    drawLine(prevX, prevY, prevZ, worldX, worldY, worldZ, r, g, b);
                } else {
                    startX = worldX;
                    startY = worldY;
                    startZ = worldZ;
                    first = false;
                }
                prevX = worldX;
                prevY = worldY;
                prevZ = worldZ;
            }
            if (!first) {
                drawLine(prevX, prevY, prevZ, startX, startY, startZ, r, g, b);
            }
        }
        drawCircle(radius, origin_x, origin_y, origin_z, r, g, b);
    }

    private static class FloatList {
        private float[] elements;
        private int size;

        public FloatList(int initialCapacity) {
            elements = new float[initialCapacity];
        }

        public void add(float value) {
            if (size == elements.length) {
                elements = Arrays.copyOf(elements, size * 2);
            }
            elements[size++] = value;
        }

        public float get(int index) {
            return elements[index];
        }

        public int size() {
            return size;
        }

        public void clear() {
            size = 0;
        }

        public boolean isEmpty() {
            return size == 0;
        }
    }
}
