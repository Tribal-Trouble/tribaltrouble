package com.oddlabs.tt.vbo;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ShortBuffer;

public final class ShortVBO extends VBO {

	public ShortVBO(int usage, int size) {
		super(GL15.GL_ELEMENT_ARRAY_BUFFER, usage, size * Short.BYTES);
	}

	public ShortVBO(int usage, @NonNull ShortBuffer initial_data) {
		this(usage, initial_data.remaining());
		put(initial_data);
	}

	private static void registerTrianglesRendered(int mode, int count) {
		int num_triangles = getNumTriangles(mode, count);
		Renderer.registerTrianglesRendered(num_triangles);
	}

	private static int getNumTriangles(int mode, int count) {
		switch (mode) {
			case GL11.GL_TRIANGLES:
				return count/3;
			case GL11.GL_QUADS:
				return count >> 2;
			case GL11.GL_TRIANGLE_FAN:
			case GL11.GL_TRIANGLE_STRIP:
				return count - 2;
			case GL11.GL_QUAD_STRIP:
				return count - 3;
			case GL11.GL_LINES:
				return count; // Assume a line is two triangles
			case GL11.GL_POINTS:
				return count*3; // assume a line is one triangle;
			case GL11.GL_LINE_STRIP:
				return (count - 1)*2;
			default:
				throw new RuntimeException("Unknown primitive type: 0x" + Integer.toHexString(mode));
		}
	}

	public void put(@NonNull ShortBuffer buffer) {
        makeCurrent();
        GL15.glBufferSubData(getTarget(), 0, buffer);
        buffer.position(buffer.limit());
	}

	public void put(short @NonNull [] buffer) {
		put(Utils.toBuffer(buffer));
	}

	public void drawElements(int mode, int count, int index) {
		registerTrianglesRendered(mode, count);
        makeCurrent();
        GL11.glDrawElements(mode, count, GL11.GL_UNSIGNED_SHORT, index<<1);
	}

        @Override
	public int capacity() {
		return getSize() / Short.BYTES;
	}
}
