package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.FixedFunctionShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.SpriteBatchRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public final class UIRenderer {
	private static @Nullable ShaderProgram shader;
	private static @Nullable SpriteBatchRenderer batchRenderer;
	private static final @NonNull MatrixStack modelViewStack = new MatrixStack();
	private static final @NonNull MatrixStack projectionStack = new MatrixStack();
	
	private UIRenderer() {}
	
	public static void initialize() {
		if (shader != null) {
			return;
		}
		
		try {
			shader = FixedFunctionShader.create();
			batchRenderer = new SpriteBatchRenderer(shader);
		} catch (Exception e) {
			System.err.println("Failed to initialize UI shader: " + e.getMessage());
		}
	}
	
	public static void syncMatrices() {
		if (batchRenderer == null) {
			return;
		}
		
		FloatBuffer mvBuffer = BufferUtils.createFloatBuffer(16);
		FloatBuffer pBuffer = BufferUtils.createFloatBuffer(16);
		
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, mvBuffer);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, pBuffer);
		
		modelViewStack.current().load(mvBuffer);
		projectionStack.current().load(pBuffer);
	}
	
	public static void drawQuad(float x, float y, float width, float height,
	                            float u1, float v1, float u2, float v2,
	                            @NonNull Texture texture) {
		if (batchRenderer == null) {
			return;
		}
		
		batchRenderer.begin(texture);
		batchRenderer.drawQuad(x, y, 0, width, height, 1, 1, 1, 1, u1, v1, u2, v2);
		batchRenderer.end();
	}
	
	public static boolean isAvailable() {
		return batchRenderer != null;
	}
}
