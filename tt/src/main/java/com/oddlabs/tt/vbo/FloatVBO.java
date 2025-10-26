package com.oddlabs.tt.vbo;

import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class FloatVBO extends VBO {
	private @Nullable FloatBuffer saved_buffer = null;
//	private FloatBuffer mapped_buffer = null;

	public FloatVBO(int usage, int size) {
		super(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, usage, size * Float.BYTES);
		ByteBuffer buffer = getSavedBuffer();
		if (buffer != null)
			saved_buffer = buffer.asFloatBuffer();
	}

	public FloatVBO(int usage, @NonNull FloatBuffer initial_data) {
		this(usage, initial_data.remaining());
		put(initial_data);
	}

	public FloatVBO(int usage, float @NonNull [] initial_data) {
		this(usage, initial_data.length);
		put(initial_data);
	}

/*	public final void map(int access) {
		if (!doMap(access))
			saved_buffer = getMappedBuffer().asFloatBuffer();
		mapped_buffer = saved_buffer;
	}

	public final boolean unmap() {
		saved_buffer.clear();
		mapped_buffer = null;
		return doUnmap();
	}

	public final FloatBuffer buffer() {
		return mapped_buffer;
	}
*/
	public void vertexPointer(int size, int stride, int index) {
		if (!use_vbo) {
			saved_buffer.position(index);
			GL11.glVertexPointer(size, stride, saved_buffer);
		} else {
			makeCurrent();
			GL11.glVertexPointer(size, GL11.GL_FLOAT, stride, index<<2);
		}
	}

	public void texCoordPointer(int size, int stride, int index) {
		if (!use_vbo) {
			saved_buffer.position(index);
			GL11.glTexCoordPointer(size, stride, saved_buffer);
		} else {
			makeCurrent();
			GL11.glTexCoordPointer(size, GL11.GL_FLOAT, stride, index<<2);
		}
	}

	public void normalPointer(int stride, int index) {
		if (!use_vbo) {
			saved_buffer.position(index);
			GL11.glNormalPointer(stride, saved_buffer);
		} else {
			makeCurrent();
			GL11.glNormalPointer(GL11.GL_FLOAT, stride, index<<2);
		}
	}

	public void colorPointer(int size, int stride, int index) {
		if (!use_vbo) {
			saved_buffer.position(index);
			GL11.glColorPointer(size, stride, saved_buffer);
		} else {
			makeCurrent();
			GL11.glColorPointer(size, GL11.GL_FLOAT, stride, index<<2);
		}
	}

	public void put(@NonNull FloatBuffer buffer) {
		putSubData(0, buffer);
	}

	public void put(float @NonNull [] buffer) {
		putSubData(0, Utils.toBuffer(buffer));
//		do {
//			map(ARBBufferObject.GL_WRITE_ONLY_ARB);
//			buffer().put(buffer);
//		} while (!unmap());
	}

	public void putSubData(int index, @NonNull FloatBuffer buffer) {
		if (!use_vbo) {
			saved_buffer.position(index);
			saved_buffer.put(buffer);
		} else {
			makeCurrent();
			ARBBufferObject.glBufferSubDataARB(getTarget(), index<<2, buffer);
			buffer.position(buffer.limit());
		}
	}

    @Override
	public int capacity() {
		return getSize() / Float.BYTES;
	}
}
