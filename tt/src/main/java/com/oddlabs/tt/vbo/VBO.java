package com.oddlabs.tt.vbo;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.Nullable;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public abstract class VBO extends NativeResource {
	private final int handle;
	private final int target;
	private final int size;
	private final static IntBuffer handle_buffer;

	private final @Nullable ByteBuffer saved_buffer;

	static {
		handle_buffer = BufferUtils.createIntBuffer(1);
	}

	private int createBuffer(int target, int usage, int size) {
		GL15.glGenBuffers(handle_buffer);
		int handle = handle_buffer.get(0);
		assert handle != 0;
		makeCurrent(target, handle);
		GL15.glBufferData(target, size, usage);
		return handle;
	}

	private static void makeCurrent(int target, int handle) {
		GL15.glBindBuffer(target, handle);
	}

	public static void releaseAll() {
		makeCurrent(GL15.GL_ARRAY_BUFFER, 0);
		releaseIndexVBO();
	}

	public static void releaseIndexVBO() {
	    makeCurrent(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	protected final void makeCurrent() {
		makeCurrent(target, handle);
	}

	public VBO(int target, int usage, int size) {
		this.target = target;
		this.size = size;
		//		mapped_buffer = null;
		handle = createBuffer(target, usage, size);
		saved_buffer = null;
	}

    @Override
	protected final void doDelete() {
        handle_buffer.put(0, handle);
        GL15.glDeleteBuffers(handle_buffer);
    }

	protected final int getTarget() {
		return target;
	}

/*	protected final boolean doMap(int access) {
		assert mapped_buffer == null;
			makeCurrent();
			mapped_buffer = ARBBufferObject.glMapBufferARB(target, access, size, saved_buffer);
			assert mapped_buffer != null;
			mapped_buffer.order(ByteOrder.nativeOrder());
			boolean result = mapped_buffer == saved_buffer;
			saved_buffer = mapped_buffer;
			return result;
	}

	protected final boolean doUnmap() {
		assert mapped_buffer != null;
		mapped_buffer.clear();
		mapped_buffer = null;
        makeCurrent();
        return ARBBufferObject.glUnmapBufferARB(target);
	}
*/
/*
	protected final ByteBuffer getMappedBuffer() {
		return mapped_buffer;
	}
*/
	protected final int getSize() {
		return size;
	}

	public abstract int capacity();
}
