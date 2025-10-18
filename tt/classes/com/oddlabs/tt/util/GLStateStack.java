package com.oddlabs.tt.util;

import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;

public final class GLStateStack {
	private static GLStateStack current;

	private final ArrayDeque<GLState> state_stack = new ArrayDeque<>();

	public static void setCurrent(GLStateStack stack) {
		current = stack;
	}

	public GLStateStack() {
		state_stack.addLast(new GLState());
	}

	private GLState getCurrentState() {
		return state_stack.peekLast();
	}

	public static void pushState() {
		current.doPushState();
	}

	private void doPushState() {
		try {
			state_stack.addLast((GLState)getCurrentState().clone());
			GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS);
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public static void popState() {
		current.doPopState();
	}

	private void doPopState() {
		GL11.glPopClientAttrib();
		state_stack.removeLast();
	}

	public static void switchState(int client_flags) {
		current.doSwitchState(client_flags);
	}

	private void doSwitchState(int client_flags) {
		GLState current_state = getCurrentState();
		current_state.switchState(client_flags);
	}
}
