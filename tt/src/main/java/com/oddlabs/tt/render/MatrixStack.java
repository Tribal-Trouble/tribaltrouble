package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;

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
		Matrix4f.load(current(), copy);
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
		current().setIdentity();
	}
	
	public void translate(float x, float y, float z) {
		Matrix4f.translate(new org.lwjgl.util.vector.Vector3f(x, y, z), current(), current());
	}
	
	public void rotate(float angle, float x, float y, float z) {
		Matrix4f.rotate(angle, new org.lwjgl.util.vector.Vector3f(x, y, z), current(), current());
	}
	
	public void scale(float x, float y, float z) {
		Matrix4f.scale(new org.lwjgl.util.vector.Vector3f(x, y, z), current(), current());
	}
	
	public void multiply(@NonNull Matrix4f matrix) {
		Matrix4f.mul(current(), matrix, current());
	}
	
	public @NonNull FloatBuffer toBuffer() {
		buffer.clear();
		current().store(buffer);
		buffer.flip();
		return buffer;
	}
}
