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
		super.begin(glMode);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}

	/**
	 * Adds a vertex to the buffer with specified position and color.
	 */
	public void vertex(float x, float y, float z, float r, float g, float b) {
		super.vertex(x, y, z, 0, 0, 1, r, g, b, 1, 0, 0);
	}
	
	/**
	 * Ends the drawing sequence and flushes any remaining vertices.
	 */
	@Override
	public void end() {
		super.end();
	}
}
