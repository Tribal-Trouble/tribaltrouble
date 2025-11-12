package com.oddlabs.tt.util;

import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

public final class GLStateStack {
	private static GLStateStack current;

	private final Deque<GLState> state_stack = new ArrayDeque<>();

	public static void setCurrent(GLStateStack stack) {
		current = stack;
	}

	public GLStateStack() {
		state_stack.push(new GLState()); // Push initial state
	}

	private @Nullable GLState getCurrentState() {
		return state_stack.peek();
	}

	public static void pushState() {
		current.doPushState();
	}

	private void doPushState() {
		GLState new_state = GLState.createCurrentState();
		state_stack.push(new_state);
	}

	public static void popState() {
		current.doPopState();
	}

	private void doPopState() {
		state_stack.pop(); // Remove the GLState object for the popped state
		// Explicitly restore fixed-function client states from the new top of the stack
		GLState previous_state = getCurrentState();
		if (previous_state != null) {
			int client_flags = 0;
			if (previous_state.vertex_array) client_flags |= GLState.VERTEX_ARRAY;
			if (previous_state.normal_array) client_flags |= GLState.NORMAL_ARRAY;
			if (previous_state.texcoord0_array) client_flags |= GLState.TEXCOORD0_ARRAY;
			if (previous_state.texcoord1_array) client_flags |= GLState.TEXCOORD1_ARRAY;
			if (previous_state.color_array) client_flags |= GLState.COLOR_ARRAY;
			previous_state.switchState(client_flags);
		}
	}

	public static void switchState(int client_flags) {
		current.doSwitchState(client_flags);
	}

	private void doSwitchState(int client_flags) {
		GLState current_state = getCurrentState();
		if (current_state != null) {
			current_state.switchState(client_flags);
		}
	}
}
