package com.oddlabs.tt.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/** Maintains a stack of transformation matrix that are applied to the drawing. */
public final class MatrixStack {
	private final Deque<@NonNull Matrix4f> stack = new ArrayDeque<>();

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
	
	public @NonNull MatrixStack translate(float x, float y, float z) {
		current().translate(x, y, z);
        return this;
    }
	
	public @NonNull MatrixStack rotate(float angle, float x, float y, float z) {
		current().rotate((float)Math.toRadians(angle), x, y, z);
        return this;
    }
	
	public @NonNull MatrixStack scale(float x, float y, float z) {
		current().scale(x, y, z);
        return this;
    }
	
	public @NonNull MatrixStack multiply(@NonNull Matrix4f matrix) {
		current().mul(matrix);
        return this;
	}
}
