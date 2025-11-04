package com.oddlabs.tt.vbo;

import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

public final class FloatVBO extends VBO {

	public FloatVBO(int usage, int size) {
		super(GL15.GL_ARRAY_BUFFER, usage, size * Float.BYTES);
	}

	public FloatVBO(int usage, @NonNull FloatBuffer initial_data) {
		this(usage, initial_data.remaining());
		put(initial_data);
	}

	public FloatVBO(int usage, float @NonNull [] initial_data) {
		this(usage, initial_data.length);
		put(initial_data);
	}

	public void vertexPointer(int size, int stride, int index) {
        makeCurrent();
        GL11.glVertexPointer(size, GL11.GL_FLOAT, stride, index<<2);
	}

	public void texCoordPointer(int size, int stride, int index) {
        makeCurrent();
        GL11.glTexCoordPointer(size, GL11.GL_FLOAT, stride, index<<2);
	}

	public void normalPointer(int stride, int index) {
        makeCurrent();
        GL11.glNormalPointer(GL11.GL_FLOAT, stride, index<<2);
	}

	public void colorPointer(int size, int stride, int index) {
        makeCurrent();
        GL11.glColorPointer(size, GL11.GL_FLOAT, stride, index<<2);
	}
	
	public void vertexAttribPointer(int location, int size, int stride, long offset) {
		makeCurrent();
		org.lwjgl.opengl.GL20.glVertexAttribPointer(location, size, GL11.GL_FLOAT, false, stride, offset);
	}

	public void put(@NonNull FloatBuffer buffer) {
		putSubData(0, buffer);
	}

	public void put(float @NonNull [] buffer) {
		putSubData(0, Utils.toBuffer(buffer));
	}

	public void putSubData(int index, @NonNull FloatBuffer buffer) {
        makeCurrent();
        GL15.glBufferSubData(getTarget(), (long) index << 2, buffer);
        buffer.position(buffer.limit());
	}

    @Override
	public int capacity() {
		return getSize() / Float.BYTES;
	}
}
