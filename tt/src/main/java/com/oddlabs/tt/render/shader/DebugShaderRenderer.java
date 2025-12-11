package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.MatrixStack;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * Renders debug graphics 
 */
public final class DebugShaderRenderer extends ShaderRenderer {

	/**
	 * Creates a new DebugShaderRenderer.
	 * @param shader The shader program to use for rendering.
	 */
	public DebugShaderRenderer(@NonNull ShaderProgram shader, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
		super(shader, modelViewStack, projectionStack);
	}

	/**
	 * Begins a new drawing sequence.
	 * @param glMode The OpenGL primitive mode (e.g., GL11.GL_LINES, GL11.GL_TRIANGLES).
	 */
	@Override
	public void begin(int glMode) {
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
		super.begin(glMode);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDepthMask(false);
	}

	/**
	 * Adds a vertex to the buffer with specified position and color.
	 */
	public void vertex(float x, float y, float z, float r, float g, float b) {
		super.vertex(x, y, z, 0, 0, 1, r, g, b, 1, 0, 0);
	}

    /**
     * Adds a vertex to the buffer with specified position and color.
     */
    public void vertex(float x, float y, float z, float r, float g, float b, float a) {
        super.vertex(x, y, z, 0, 0, 1, r, g, b, a, 0, 0);
    }

	/**
	 * Adds a vertex to the buffer with specified position and color.
	 * @param color A float array containing the RGB or RGBA color components.
	 */
	public void vertex(float x, float y, float z, float @NonNull[] color) {
		vertex(x, y, z, color[0], color[1], color[2], color.length >= 4 ? color[3] : 1);
	}
	
	/**
	 * Ends the drawing sequence and flushes any remaining vertices.
	 */
	@Override
	public void end() {
		super.end();
		GL11.glPopAttrib();
	}

	public void drawAxes(float center, float z, float @NonNull[] xAxisColor, float @NonNull[] yAxisColor, float @NonNull[] zAxisColor) {
		begin(GL11.GL_LINES);
		try {
			// X axis - red
			vertex(center, center, z, xAxisColor);
			vertex(center + 10, center, z, xAxisColor);

			// Y axis - green
			vertex(center, center, z, yAxisColor);
			vertex(center, center + 10, z, yAxisColor);

			// Z axis - blue
			vertex(center, center, z, zAxisColor);
			vertex(center, center, z + 10, zAxisColor);
		} finally {
			end();
		}
	}
}
