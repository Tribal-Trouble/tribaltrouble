package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.util.GLUtils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

/**
 * Renders debug geometry using a shader program abstracting away the OpenGL VBO and shader setup.
 */
public final class DebugShaderRenderer {
	private static final int FLOATS_PER_VERTEX = 12;
	private static final int INITIAL_CAPACITY = 256;
	
	private final @NonNull ShaderProgram shader;
	private final @NonNull VertexLayout layout = VertexLayout.of(
            VertexAttribute.POSITION,
            VertexAttribute.NORMAL,
            VertexAttribute.COLOR,
            VertexAttribute.TEX_COORD_0
    );
	private final @NonNull MatrixStack modelViewStack;
	private final @NonNull MatrixStack projectionStack;
	
	private final @NonNull FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(INITIAL_CAPACITY * FLOATS_PER_VERTEX);

	private int vboHandle = 0;
	private int vertexCount = 0;
	private int mode = GL11.GL_LINES;

    /**
     * Creates a new DebugShaderRenderer.
     * @param modelViewStack The model-view matrix stack.
     * @param projectionStack The projection matrix stack.
     */
    public DebugShaderRenderer(@NonNull MatrixStack modelViewStack,
                               @NonNull MatrixStack projectionStack) {
        this(FixedFunctionShader.create(), modelViewStack, projectionStack);
    }

	/**
	 * Creates a new DebugShaderRenderer.
	 * @param shader The shader program to use for rendering.
	 * @param modelViewStack The model-view matrix stack.
	 * @param projectionStack The projection matrix stack.
	 */
	public DebugShaderRenderer(@NonNull ShaderProgram shader, 
	                           @NonNull MatrixStack modelViewStack,
	                           @NonNull MatrixStack projectionStack) {
		this.shader = shader;
		this.modelViewStack = modelViewStack;
		this.projectionStack = projectionStack;
	}
	
	/**
	 * Begins a new drawing sequence.
	 * @param glMode The OpenGL primitive mode (e.g., GL11.GL_LINES, GL11.GL_TRIANGLES).
	 */
	public void begin(int glMode) {
        assert vertexCount == 0 : "Buffer already had " + vertexCount + " vertexes";
		vertexBuffer.clear();
		vertexCount = 0;
		mode = glMode;
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}

	/**
	 * Adds a vertex to the buffer with specified position and color.
	 */
	public void vertex(float x, float y, float z, float r, float g, float b) {
		if (vertexBuffer.remaining() < FLOATS_PER_VERTEX) {
			flush();
			vertexBuffer.clear();
		}
		
		vertexBuffer.put(x).put(y).put(z);
		vertexBuffer.put(0).put(0).put(1); // Normal (dummy)
		vertexBuffer.put(r).put(g).put(b).put(1); // Color (RGBA)
		vertexBuffer.put(0).put(0); // TexCoord (dummy)
		vertexCount++;
	}
	
	/**
	 * Ends the drawing sequence and flushes any remaining vertices.
	 */
	public void end() {
		flush();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}
	
	/**
	 * Flushes the buffered vertices to OpenGL for rendering.
	 */
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
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after glBufferData()");

		shader.use();
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after shader.use()");
		
		shader.setUniformMatrix4(FixedFunctionShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.toBuffer());
		shader.setUniformMatrix4(FixedFunctionShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.toBuffer());
		shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_LIGHTING, 0);
		shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_TEXTURE, 0);
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after setting uniforms()");
		
		layout.bind(shader);
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after layout.bind()");
		
		GL11.glDrawArrays(mode, 0, vertexCount);
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after glDrawArrays()");
		
		layout.unbind(shader);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		ShaderProgram.unbind();
		GLUtils.checkGLError("DebugShaderRenderer.flush() - after ShaderProgram.unbind()");
		
		vertexCount = 0;
	}
	
	/**
	 * Cleans up OpenGL resources used by this renderer.
	 */
	public void cleanup() {
		if (vboHandle != 0) {
			GL15.glDeleteBuffers(vboHandle);
			vboHandle = 0;
		}
	}
}
