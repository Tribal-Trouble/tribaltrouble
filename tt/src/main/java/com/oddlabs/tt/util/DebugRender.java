package com.oddlabs.tt.util;

import com.oddlabs.tt.render.shader.DebugShaderRenderer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class DebugRender {
	private final static float[][] debug_colors = {{7f, 1f, 1f}, {7f, 1f, 0f}, {7f, 0f, 1f}, {.3f, .7f, 0f},
												   {0f, 1f, 1f}, {0f, 1f, 0f}, {0f, 0f, 1f}, {0f, 0f, 0f},
												   {0f, .5f, .5f}, {0f, .5f, 0f}, {0f, 0f, .5f}, {.5f, .8f, .8f},
												   {.3f, .5f, 1f}, {5f, .5f, .8f}, {.3f, .2f, .5f}, {.3f, .3f, .3f},
												   {.5f, 1f, 1f}, {.5f, 1f, .5f}, {.5f, .5f, 1f}, {.5f, .5f, .5f}};
	private final static float CIRCLE_DELTA = (float)java.lang.Math.PI/2;
	private final static float ANGLE_DELTA = (float)java.lang.Math.PI/20;
	private final static float SUBDIV = 0.4f;

	private static @Nullable DebugShaderRenderer shaderRenderer;
	
	private DebugRender() {
	}
	
	public static void setShaderRenderer(@Nullable DebugShaderRenderer renderer) {
		shaderRenderer = renderer;
	}

	public static void setColor(int i) {
		float[] color = debug_colors[i%debug_colors.length];
		GL11.glColor3f(color[0], color[1], color[2]);
	}

	public static void drawBox(float bmin_x, float bmax_x, float bmin_y, float bmax_y, float bmin_z, float bmax_z, float r, float g, float b) {
		if (shaderRenderer != null) {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			shaderRenderer.begin(GL11.GL_LINES);
			
			shaderRenderer.vertex(bmin_x, bmin_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmin_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmin_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmax_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmin_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmin_y, bmin_z, r, g, b);
			
			shaderRenderer.vertex(bmax_x, bmax_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmax_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmax_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmin_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmax_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmax_y, bmax_z, r, g, b);
			
			shaderRenderer.vertex(bmin_x, bmin_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmax_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmin_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmin_y, bmax_z, r, g, b);
			
			shaderRenderer.vertex(bmin_x, bmax_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmax_y, bmax_z, r, g, b);
			shaderRenderer.vertex(bmin_x, bmax_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmax_y, bmin_z, r, g, b);
			
			shaderRenderer.vertex(bmax_x, bmin_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmax_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmin_y, bmin_z, r, g, b);
			shaderRenderer.vertex(bmax_x, bmin_y, bmax_z, r, g, b);
			
			shaderRenderer.end();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
	}

	public static void drawPoint(float x, float y, float z, float size, float r, float g, float b) {
		if (shaderRenderer != null) {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glPointSize(size);
			shaderRenderer.begin(GL11.GL_POINTS);
			shaderRenderer.vertex(x, y, z, r, g, b);
			shaderRenderer.end();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
	}
	
	public static void drawLine(float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b) {
		if (shaderRenderer != null) {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			shaderRenderer.begin(GL11.GL_LINES);
			shaderRenderer.vertex(x1, y1, z1, r, g, b);
			shaderRenderer.vertex(x2, y2, z2, r, g, b);
			shaderRenderer.end();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
	}

	public static void drawQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float z, float r, float g, float b) {
		if (shaderRenderer != null) {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_LINE);
			GL11.glPolygonMode(GL11.GL_BACK, GL11.GL_LINE);
			GL11.glDisable(GL11.GL_CULL_FACE);
			
			shaderRenderer.begin(GL11.GL_LINE_LOOP);
			shaderRenderer.vertex(x1, y1, z, r, g, b);
			shaderRenderer.vertex(x2, y2, z, r, g, b);
			shaderRenderer.vertex(x3, y3, z, r, g, b);
			shaderRenderer.vertex(x4, y4, z, r, g, b);
			shaderRenderer.end();
			
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_FILL);
			GL11.glPolygonMode(GL11.GL_BACK, GL11.GL_FILL);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
	}

	public static void drawCylinder(float origin_x, float origin_y, float origin_z, float radius, int num_circles, float r, float g, float b) {
		if (shaderRenderer != null) {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			float z = 0f;
			for (int i = 0; i < num_circles; i++) {
				drawCircle(radius, origin_x, origin_y, origin_z + z, r, g, b);
				z += SUBDIV;
			}
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
	}

	private static void drawCircle(float radius, float origin_x, float origin_y, float origin_z, float r, float g, float b) {
		if (shaderRenderer != null) {
			shaderRenderer.begin(GL11.GL_LINE_LOOP);
			for (float phi = 0f; phi < (float)java.lang.Math.PI*2; phi += ANGLE_DELTA) {
				float x = radius*(float)java.lang.Math.cos(phi);
				float y = radius*(float)java.lang.Math.sin(phi);
				shaderRenderer.vertex(x + origin_x, y + origin_y, origin_z, r, g, b);
			}
			shaderRenderer.end();
		}
	}

	public static void drawSphere(float origin_x, float origin_y, float origin_z, float radius, float r, float g, float b) {
		if (shaderRenderer != null) {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			for (float phi = 0; phi < (float)java.lang.Math.PI; phi += CIRCLE_DELTA) {
				shaderRenderer.begin(GL11.GL_LINE_LOOP);
				for (float rho = 0f; rho < (float)java.lang.Math.PI*2; rho += ANGLE_DELTA) {
					float x = radius*(float)java.lang.Math.cos(rho);
					float z = radius*(float)java.lang.Math.sin(rho);
					float y = x*(float)java.lang.Math.sin(phi);
					x *= (float)java.lang.Math.cos(phi);
					shaderRenderer.vertex(x + origin_x, y + origin_y, z + origin_z, r, g, b);
				}
				shaderRenderer.end();
			}
			drawCircle(radius, origin_x, origin_y, origin_z, r, g, b);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
	}
}
