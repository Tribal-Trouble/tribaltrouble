package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

public final class MatrixStack {
	private static final int MATRIX_ELEMENTS = 16;
	
	private final Deque<Matrix4f> stack = new ArrayDeque<>();
	private final FloatBuffer buffer = BufferUtils.createFloatBuffer(MATRIX_ELEMENTS);
	
	public MatrixStack() {
		stack.push(new Matrix4f());
	}
	
	public void push() {
		Matrix4f copy = new Matrix4f();
		copy.set(current());
		stack.push(copy);
	}
	
	public void pop() {
		if (stack.size() <= 1) {
			throw new IllegalStateException("Cannot pop last matrix");
		}
		stack.pop();
	}
	
	public @NonNull Matrix4f current() {
		return stack.peek();
	}
	
	public void loadIdentity() {
		current().identity();
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
	
	public @NonNull FloatBuffer toBuffer() {
		buffer.clear();
		current().get(buffer); // JOML's get() does not advance the buffer's position.
		// Do NOT flip the buffer here. glUniformMatrix4fv reads from the current position.
		return buffer;
	}
}
