package com.oddlabs.tt.vbo;

import com.oddlabs.tt.resource.NativeResource;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.IntBuffer;
import java.util.Objects;

public abstract class VBO extends NativeResource<VBO.Buffer> {
    static final class Buffer extends NativeResource.NativeState {
        private static final IntBuffer handle_buffer = Objects.requireNonNull(BufferUtils.createIntBuffer(1));

        private final int handle;

        Buffer(int target, int usage, int size) {
            handle = createBuffer(target, usage, size);
        }

        private int createBuffer(int target, int usage, int size) {
            synchronized (handle_buffer) {
                GL15.glGenBuffers(handle_buffer);
                int handle = handle_buffer.get(0);
                assert handle != 0;
                makeCurrent(target, handle);
                GL15.glBufferData(target, size, usage);
                return handle;
            }
        }

        @Override
        public void close() {
            synchronized (handle_buffer) {
                handle_buffer.put(0, handle);
                GL15.glDeleteBuffers(handle_buffer);
            }
        }
    }

	private final int target;
	private final int size;

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

	public final void makeCurrent() {
		makeCurrent(target, state.handle);
	}

	public VBO(int target, int usage, int size) {
        super(new Buffer(target, usage, size));
		this.target = target;
		this.size = size;
	}

	protected final int getTarget() {
		return target;
	}

	protected final int getSize() {
		return size;
	}

	public abstract int capacity();
}
