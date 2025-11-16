package com.oddlabs.tt.util;

import com.oddlabs.tt.render.shader.DebugShaderRenderer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import com.oddlabs.util.Color;

/**
 * Utilities for rendering debug shapes (boxes, lines, spheres, etc.) using a {@link DebugShaderRenderer}.
 */
public final class DebugRender {
	private static final float[] AXIS_X_COLOR = Color.rgb3f(0xFF0000);
	private static final float[] AXIS_Y_COLOR = Color.rgb3f(0x00FF00);
	private static final float[] AXIS_Z_COLOR = Color.rgb3f(0x0000FF);

	public static final float[][] debug_colors = {
			Color.rgb3f(0x7f1f1f), Color.rgb3f(0x7f1f00), Color.rgb3f(0x7f001f), Color.rgb3f(0x3f7f00),
			Color.rgb3f(0x001f1f), Color.rgb3f(0x001f00), Color.rgb3f(0x00001f), Color.rgb3f(0x000000),
			Color.rgb3f(0x005f5f), Color.rgb3f(0x005f00), Color.rgb3f(0x00005f), Color.rgb3f(0x5f8f8f),
			Color.rgb3f(0x3f5f1f), Color.rgb3f(0x5f5f8f), Color.rgb3f(0x3f2f5f), Color.rgb3f(0x3f3f3f),
			Color.rgb3f(0x5f1f1f), Color.rgb3f(0x5f1f5f), Color.rgb3f(0x5f5f1f), Color.rgb3f(0x5f5f5f)
	};
	private static final float CIRCLE_DELTA = (float)java.lang.Math.PI/2;
	private static final float ANGLE_DELTA = (float)java.lang.Math.PI/20;
	private static final float SUBDIV = 0.4f;

	private static @Nullable DebugShaderRenderer shaderRenderer;
	
	private DebugRender() {
	}
	
	/**
	 * Sets the {@link DebugShaderRenderer} to be used for drawing debug shapes.
	 * @param renderer The renderer to use, or {@code null} to disable debug rendering.
	 */
	public static void setShaderRenderer(@Nullable DebugShaderRenderer renderer) {
		shaderRenderer = renderer;
	}

	/**
	 * Draws a wireframe box.
	 */
	public static void drawBox(float bmin_x, float bmax_x, float bmin_y, float bmax_y, float bmin_z, float bmax_z, float r, float g, float b) {
        DebugShaderRenderer renderer = shaderRenderer;
        if (null == renderer) {
            return;
        }
        renderer.begin(GL11.GL_LINES);
        try {
            renderer.vertex(bmin_x, bmin_y, bmin_z, r, g, b);
            renderer.vertex(bmin_x, bmin_y, bmax_z, r, g, b);
            renderer.vertex(bmin_x, bmin_y, bmin_z, r, g, b);
            renderer.vertex(bmin_x, bmax_y, bmin_z, r, g, b);
            renderer.vertex(bmin_x, bmin_y, bmin_z, r, g, b);
            renderer.vertex(bmax_x, bmin_y, bmin_z, r, g, b);

            renderer.vertex(bmax_x, bmax_y, bmax_z, r, g, b);
            renderer.vertex(bmax_x, bmax_y, bmin_z, r, g, b);
            renderer.vertex(bmax_x, bmax_y, bmax_z, r, g, b);
            renderer.vertex(bmax_x, bmin_y, bmax_z, r, g, b);
            renderer.vertex(bmax_x, bmax_y, bmax_z, r, g, b);
            renderer.vertex(bmin_x, bmax_y, bmax_z, r, g, b);

            renderer.vertex(bmin_x, bmin_y, bmax_z, r, g, b);
            renderer.vertex(bmin_x, bmax_y, bmax_z, r, g, b);
            renderer.vertex(bmin_x, bmin_y, bmax_z, r, g, b);
            renderer.vertex(bmax_x, bmin_y, bmax_z, r, g, b);

            renderer.vertex(bmin_x, bmax_y, bmin_z, r, g, b);
            renderer.vertex(bmin_x, bmax_y, bmax_z, r, g, b);
            renderer.vertex(bmin_x, bmax_y, bmin_z, r, g, b);
            renderer.vertex(bmax_x, bmax_y, bmin_z, r, g, b);

            renderer.vertex(bmax_x, bmin_y, bmin_z, r, g, b);
            renderer.vertex(bmax_x, bmax_y, bmin_z, r, g, b);
            renderer.vertex(bmax_x, bmin_y, bmin_z, r, g, b);
            renderer.vertex(bmax_x, bmin_y, bmax_z, r, g, b);
        } finally {
            renderer.end();
        }
	}

	/**
	 * Draws a point.
	 */
	public static void drawPoint(float x, float y, float z, float size, float r, float g, float b) {
        DebugShaderRenderer renderer = shaderRenderer;
        if (null == renderer) {
            return;
        }
        GL11.glPointSize(size);
        renderer.begin(GL11.GL_POINTS);
        try {
            renderer.vertex(x, y, z, r, g, b);
        } finally {
            renderer.end();
        }
	}
	
	/**
	 * Draws a line segment.
	 */
	public static void drawLine(float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b) {
        DebugShaderRenderer renderer = shaderRenderer;
        if (null == renderer) {
            return;
        }
        renderer.begin(GL11.GL_LINES);
        try {
            renderer.vertex(x1, y1, z1, r, g, b);
            renderer.vertex(x2, y2, z2, r, g, b);
        } finally {
            renderer.end();
        }
	}

    /**
     * Draws the classic OpenGL axes at ground level at the center of the map
     */
    public static void drawAxes(float center, float z) {
        DebugShaderRenderer renderer = shaderRenderer;
        if (null == renderer) {
            return;
        }
        renderer.begin(GL11.GL_LINES);
        try {
            // X axis - red
            renderer.vertex(center, center, z, AXIS_X_COLOR);
            renderer.vertex(center + 10, center, z, AXIS_X_COLOR);

            // Y axis - green
            renderer.vertex(center, center, z, AXIS_Y_COLOR);
            renderer.vertex(center, center + 10, z, AXIS_Y_COLOR);

            // Z axis - blue
            renderer.vertex(center, center, z, AXIS_Z_COLOR);
            renderer.vertex(center, center, z + 10, AXIS_Z_COLOR);
        } finally {
            renderer.end();
        }
    }

	/**
	 * Draws a wireframe quad.
	 */
	public static void drawQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float z, float r, float g, float b) {
        DebugShaderRenderer renderer = shaderRenderer;
        if (null == renderer) {
            return;
        }
        renderer.begin(GL11.GL_LINE_LOOP);
        try {
            renderer.vertex(x1, y1, z, r, g, b);
            renderer.vertex(x2, y2, z, r, g, b);
            renderer.vertex(x3, y3, z, r, g, b);
            renderer.vertex(x4, y4, z, r, g, b);
        } finally {
            renderer.end();
        }
	}

	/**
	 * Draws a wireframe cylinder composed of multiple circles.
	 */
	public static void drawCylinder(float origin_x, float origin_y, float origin_z, float radius, int num_circles, float r, float g, float b) {
        DebugShaderRenderer renderer = shaderRenderer;
        if (null == renderer) {
            return;
        }
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
        DebugShaderRenderer renderer = shaderRenderer;
        if (null == renderer) {
            return;
        }
        renderer.begin(GL11.GL_LINE_LOOP);
        try {
            for (float phi = 0f; phi < (float) java.lang.Math.PI * 2; phi += ANGLE_DELTA) {
                float x = radius * (float) java.lang.Math.cos(phi);
                float y = radius * (float) java.lang.Math.sin(phi);
                renderer.vertex(x + origin_x, y + origin_y, origin_z, r, g, b);
            }
        } finally {
            renderer.end();
        }
	}

	/**
	 * Draws a wireframe sphere.
	 */
	public static void drawSphere(float origin_x, float origin_y, float origin_z, float radius, float r, float g, float b) {
        DebugShaderRenderer renderer = shaderRenderer;
        if (null == renderer) {
            return;
        }
        for (float phi = 0; phi < (float)java.lang.Math.PI; phi += CIRCLE_DELTA) {
            renderer.begin(GL11.GL_LINE_LOOP);
            try {
                for (float rho = 0f; rho < (float) java.lang.Math.PI * 2; rho += ANGLE_DELTA) {
                    float x = radius * (float) java.lang.Math.cos(rho);
                    float z = radius * (float) java.lang.Math.sin(rho);
                    float y = x * (float) java.lang.Math.sin(phi);
                    x *= (float) java.lang.Math.cos(phi);
                    renderer.vertex(x + origin_x, y + origin_y, z + origin_z, r, g, b);
                }
            } finally {
                renderer.end();
            }
        }
        drawCircle(radius, origin_x, origin_y, origin_z, r, g, b);
	}
}
