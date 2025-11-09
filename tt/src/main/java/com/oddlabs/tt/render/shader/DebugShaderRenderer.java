package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.util.GLUtils; // Added import for GLUtils
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

public final class DebugShaderRenderer {
	private static final int FLOATS_PER_VERTEX = 12;
	private static final int INITIAL_CAPACITY = 256;
	
	private final @NonNull ShaderProgram shader;
	private final @NonNull VertexLayout layout;
	private final @NonNull MatrixStack modelViewStack;
	private final @NonNull MatrixStack projectionStack;
	
	private FloatBuffer vertexBuffer;
	private int vboHandle = 0;
	private int vertexCount = 0;
	private int mode = GL11.GL_LINES;
	
	public DebugShaderRenderer(@NonNull ShaderProgram shader, 
	                           @NonNull MatrixStack modelViewStack,
	                           @NonNull MatrixStack projectionStack) {
		this.shader = shader;
		this.layout = VertexLayout.of(
			VertexAttribute.POSITION,
			VertexAttribute.NORMAL,
			VertexAttribute.COLOR,
			VertexAttribute.TEX_COORD_0
		);
		this.modelViewStack = modelViewStack;
		this.projectionStack = projectionStack;
	}
	
	public void begin(int glMode) {
		if (vertexBuffer == null) {
			vertexBuffer = BufferUtils.createFloatBuffer(INITIAL_CAPACITY * FLOATS_PER_VERTEX);
		}
		vertexBuffer.clear();
		vertexCount = 0;
		mode = glMode;
	}
	
	public void color(float r, float g, float b) {
		// Store for next vertex
	}
	
	public void vertex(float x, float y, float z, float r, float g, float b) {
		if (vertexBuffer.remaining() < FLOATS_PER_VERTEX) {
			flush();
			vertexBuffer.clear();
		}
		
		vertexBuffer.put(x).put(y).put(z);
		vertexBuffer.put(0).put(0).put(1);
		vertexBuffer.put(r).put(g).put(b).put(1);
		vertexBuffer.put(0).put(0);
		vertexCount++;
	}
	
	public void end() {
		flush();
	}
	
	private void flush() {
		if (vertexCount == 0) {
			return;
		}
		
		vertexBuffer.flip();
		
		if (vboHandle == 0) {
			vboHandle = GL15.glGenBuffers();
		}
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STREAM_DRAW);
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after glBufferData()"); // Error check
		
		shader.use();
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after shader.use()"); // Error check
		
		shader.setUniformMatrix4(FixedFunctionShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.toBuffer());
		shader.setUniformMatrix4(FixedFunctionShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.toBuffer());
		shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_LIGHTING, 0);
		shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_TEXTURE, 0);
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after setting uniforms()"); // Error check
		
		layout.bind(shader);
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after layout.bind()"); // Error check
		
		GL11.glDrawArrays(mode, 0, vertexCount);
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after glDrawArrays()"); // Error check
		
		layout.unbind(shader);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		ShaderProgram.unbind();
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after ShaderProgram.unbind()"); // Error check
		
		vertexCount = 0;
	}
	
	public void cleanup() {
		if (vboHandle != 0) {
			GL15.glDeleteBuffers(vboHandle);
			vboHandle = 0;
		}
	}
}
