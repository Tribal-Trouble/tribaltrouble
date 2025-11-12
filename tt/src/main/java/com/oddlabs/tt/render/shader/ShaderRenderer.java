package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.util.GLStateStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

/**
 * Renders geometry using a shader program abstracting away the OpenGL VBO and shader setup.
 */
public class ShaderRenderer {
	private static final int FLOATS_PER_VERTEX = 12; // pos(3) + normal(3) + color(4) + uv(2)
	private static final int INITIAL_VERTEX_CAPACITY = 1024;
	
	private final @NonNull ShaderProgram shader;
	private final @NonNull VertexLayout layout;
	private final @NonNull MatrixStack modelViewStack;
	private final @NonNull MatrixStack projectionStack;
	
	private @Nullable FloatBuffer vertexBuffer;
	private int vboHandle = 0;
	private int vertexCount = 0;
	private int mode = GL11.GL_TRIANGLES;
	
	public ShaderRenderer(@NonNull ShaderProgram shader, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
		this.shader = shader;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
		this.layout = VertexLayout.of(
			VertexAttribute.POSITION,
			VertexAttribute.NORMAL,
			VertexAttribute.COLOR,
			VertexAttribute.TEX_COORD_0
		);
	}

    /**
     * Begins a new drawing sequence.
     * @param glMode The OpenGL primitive mode (e.g., GL11.GL_LINES, GL11.GL_TRIANGLES).
     */
	public void begin(int glMode) {
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); // Use GL_ALL_ATTRIB_BITS for comprehensive state saving
		GLStateStack.pushState();
		if (vertexBuffer == null) {
			vertexBuffer = BufferUtils.createFloatBuffer(INITIAL_VERTEX_CAPACITY * FLOATS_PER_VERTEX);
		}
		vertexBuffer.clear();
		vertexCount = 0;
		this.mode = glMode;
	}
	
	public void vertex(float x, float y, float z, 
	                   float nx, float ny, float nz,
	                   float r, float g, float b, float a,
	                   float u, float v) {
		if (vertexBuffer == null) {
			throw new IllegalStateException("Must call begin() first");
		}
		
		if (vertexBuffer.remaining() < FLOATS_PER_VERTEX) {
			flush();
			vertexBuffer.clear();
		}
		
		vertexBuffer.put(x).put(y).put(z);
		vertexBuffer.put(nx).put(ny).put(nz);
		vertexBuffer.put(r).put(g).put(b).put(a);
		vertexBuffer.put(u).put(v);
		vertexCount++;
	}

    /**
     * Ends the drawing sequence and flushes any remaining vertices.
     */
    public void end() {
		flush();
		GLStateStack.popState();
		GL11.glPopAttrib();
	}
	
	private void flush() {
		if (vertexCount == 0 || vertexBuffer == null) {
			return;
		}
		
		vertexBuffer.flip();
		
		if (vboHandle == 0) {
			vboHandle = GL15.glGenBuffers();
		}
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STREAM_DRAW);
		
		shader.use();
        try {
            shader.setUniformMatrix4(FixedFunctionShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.toBuffer());
            shader.setUniformMatrix4(FixedFunctionShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.toBuffer());

            layout.bind(shader);

            GL11.glDrawArrays(mode, 0, vertexCount);

            layout.unbind(shader);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        } finally {
            ShaderProgram.unbind();
        }
		
		vertexCount = 0;
	}
	
	public void cleanup() {
		if (vboHandle != 0) {
			GL15.glDeleteBuffers(vboHandle);
			vboHandle = 0;
		}
	}
}
