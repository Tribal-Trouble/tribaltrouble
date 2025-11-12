package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public final class SpriteBatchRenderer {
	private static final int FLOATS_PER_VERTEX = 12;
	private static final int VERTICES_PER_SPRITE = 4;
	private static final int INDICES_PER_SPRITE = 6;
	private static final int INITIAL_BATCH_SIZE = 256;
	
	private final @NonNull ShaderProgram shader;
	private final VertexLayout layout = VertexLayout.of(
            VertexAttribute.POSITION,
            VertexAttribute.NORMAL,
            VertexAttribute.COLOR,
            VertexAttribute.TEX_COORD_0
    );
	private final @NonNull MatrixStack modelViewStack;
	private final @NonNull MatrixStack projectionStack;
	
	private @Nullable FloatBuffer vertexBuffer;
	private @Nullable ShortBuffer indexBuffer;
	private int vboHandle = 0;
	private int iboHandle = 0;
	private int spriteCount = 0;
	
	private @Nullable Texture currentTexture;
	private boolean drawing = false;
	
	public SpriteBatchRenderer(@NonNull ShaderProgram shader, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
		this.shader = shader;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
	}

	public void begin(@Nullable Texture texture) {
		if (drawing) {
			throw new IllegalStateException("Already drawing");
		}
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
		GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
		GLStateStack.pushState();
		if (vertexBuffer == null) {
			vertexBuffer = BufferUtils.createFloatBuffer(INITIAL_BATCH_SIZE * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX);
			indexBuffer = BufferUtils.createShortBuffer(INITIAL_BATCH_SIZE * INDICES_PER_SPRITE);
		}
		
		spriteCount = 0;
		currentTexture = texture;
		drawing = true;

		// Explicitly manage states
		if (currentTexture != null) {
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		} else {
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		}
		GL11.glEnable(GL11.GL_DEPTH_TEST); // Sprites usually need depth testing
	}
	
	public void drawQuad(float x, float y, float z, float width, float height,
	                     float r, float g, float b, float a,
	                     float u0, float v0, float u1, float v1) {
		if (!drawing) {
			throw new IllegalStateException("Must call begin() first");
		}
		
		if (vertexBuffer.remaining() < VERTICES_PER_SPRITE * FLOATS_PER_VERTEX) {
			flush();
		}
		
		short baseIndex = (short)(spriteCount * VERTICES_PER_SPRITE);
		
		addVertex(x, y, z, 0, 0, 1, r, g, b, a, u0, v0);
		addVertex(x + width, y, z, 0, 0, 1, r, g, b, a, u1, v0);
		addVertex(x + width, y + height, z, 0, 0, 1, r, g, b, a, u1, v1);
		addVertex(x, y + height, z, 0, 0, 1, r, g, b, a, u0, v1);
		
		indexBuffer.put(baseIndex);
		indexBuffer.put((short)(baseIndex + 1));
		indexBuffer.put((short)(baseIndex + 2));
		indexBuffer.put(baseIndex);
		indexBuffer.put((short)(baseIndex + 2));
		indexBuffer.put((short)(baseIndex + 3));
		
		spriteCount++;
	}
	
	private void addVertex(float x, float y, float z, float nx, float ny, float nz,
	                       float r, float g, float b, float a, float u, float v) {
		vertexBuffer.put(x).put(y).put(z);
		vertexBuffer.put(nx).put(ny).put(nz);
		vertexBuffer.put(r).put(g).put(b).put(a);
		vertexBuffer.put(u).put(v);
	}
	
	public void end() {
		if (!drawing) {
			throw new IllegalStateException("Not drawing");
		}
		flush();
		drawing = false;
		GLStateStack.popState();
		GL11.glPopClientAttrib();
		GL11.glPopAttrib();
	}
	
	private void flush() {
		if (spriteCount == 0) {
			return;
		}
		
		vertexBuffer.flip();
		indexBuffer.flip();
		
		if (vboHandle == 0) {
			vboHandle = GL15.glGenBuffers();
			iboHandle = GL15.glGenBuffers();
		}
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboHandle);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STREAM_DRAW);
		
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, iboHandle);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STREAM_DRAW);
		
		shader.use();
		shader.setUniformMatrix4(FixedFunctionShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.toBuffer());
		shader.setUniformMatrix4(FixedFunctionShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.toBuffer());
		shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_LIGHTING, 0);
		shader.setUniform(FixedFunctionShader.Uniforms.ENABLE_TEXTURE, currentTexture != null ? 1 : 0);
		
		if (currentTexture != null) {
			shader.setUniform(FixedFunctionShader.Uniforms.TEXTURE_0, 0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture.getHandle());
		}
		
		layout.bind(shader);
		
		GL11.glDrawElements(GL11.GL_TRIANGLES, indexBuffer.limit(), GL11.GL_UNSIGNED_SHORT, 0);
		
		layout.unbind(shader);
		
		if (currentTexture != null) {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		}
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		ShaderProgram.unbind();
		
		vertexBuffer.clear();
		indexBuffer.clear();
		spriteCount = 0;
	}
	
	public void cleanup() {
		if (vboHandle != 0) {
			GL15.glDeleteBuffers(vboHandle);
			GL15.glDeleteBuffers(iboHandle);
			vboHandle = 0;
			iboHandle = 0;
		}
	}
}
