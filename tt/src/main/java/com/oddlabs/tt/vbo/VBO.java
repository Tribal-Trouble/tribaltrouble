package com.oddlabs.tt.vbo;

import com.oddlabs.tt.resource.NativeResource;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.IntBuffer;

public abstract class VBO extends NativeResource {
	private final int handle;
	private final int target;
	private final int size;
	private final static IntBuffer handle_buffer;

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
		handle = createBuffer(target, usage, size);
	}

    @Override
	protected final void doDelete() {
        handle_buffer.put(0, handle);
        GL15.glDeleteBuffers(handle_buffer);
    }

	protected final int getTarget() {
		return target;
	}

	protected final int getSize() {
		return size;
	}

	public abstract int capacity();
}
