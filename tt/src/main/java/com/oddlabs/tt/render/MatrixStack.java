package com.oddlabs.tt.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/** Maintains a stack of transformation matrix that are applied to the drawing. */
public final class MatrixStack {
	private static final int MATRIX_ELEMENTS = 16;
	
	private final Deque<@NonNull Matrix4f> stack = new ArrayDeque<>();
	private final FloatBuffer buffer = BufferUtils.createFloatBuffer(MATRIX_ELEMENTS);

    public interface TopListener {
        void topChanging(@NonNull Matrix4fc matrix);
    }

    private final @Nullable TopListener topListener;

    public MatrixStack() {
        this(null);
    }

	public MatrixStack(@Nullable TopListener topListener) {
		clear();
        this.topListener = topListener;
	}
	
	public @NonNull Matrix4f push() {
		Matrix4f copy = new Matrix4f(current());
		stack.push(copy);
        if (null != topListener) {
            topListener.topChanging(current());
        }
        return current();
	}
	
	public @NonNull Matrix4f pop() {
        if (null != topListener) {
            topListener.topChanging(current());
        }
		if (stack.size() > 1) {
            stack.pop();
        } else {
            clear();
        }
        return current();
	}
	
	public @NonNull Matrix4f current() {
		return stack.element();
	}
	
	@NonNull Matrix4f clear() {
		stack.clear();
        stack.push(new Matrix4f());

        return current();
	}
	
	public void translate(float x, float y, float z) {
		current().translate(x, y, z);
	}
	
	public void rotate(float angle, float x, float y, float z) {
		current().rotate((float)Math.toRadians(angle), x, y, z);
	}
	
	public void scale(float x, float y, float z) {
		current().scale(x, y, z);
	}
	
	public void multiply(@NonNull Matrix4f matrix) {
		current().mul(matrix);
	}

    /** @implNote The returned FloatBuffer is yours only until it is needed again. Use the {@link #toBuffer(FloatBuffer)}
     * overload if you need the buffer for longer term use. */
	public @NonNull FloatBuffer toBuffer() {
		return toBuffer(buffer);
	}

    public @NonNull FloatBuffer toBuffer(@NonNull FloatBuffer buffer) {
        buffer.clear();
        current().get(buffer);
        // Do NOT flip the buffer here. glUniformMatrix4fv reads from the current position.
        return buffer;
    }
}
