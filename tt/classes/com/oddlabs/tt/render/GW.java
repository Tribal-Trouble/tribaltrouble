package com.oddlabs.tt.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWVidMode;

import org.lwjgl.opengl.GL11;

import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.input.Mouse;

import java.nio.IntBuffer;

public final strictfp class GW {
    public static final int GL_PROJECTION = GL11.GL_PROJECTION;
    public static final int GL_MODELVIEW = GL11.GL_MODELVIEW;

    public static final int GL_ALPHA_TEST = GL11.GL_ALPHA_TEST;

    public static final int GL_COLOR_BUFFER_BIT = GL11.GL_COLOR_BUFFER_BIT;
    public static final int GL_DEPTH_BUFFER_BIT = GL11.GL_DEPTH_BUFFER_BIT;

    public static void glMatrixMode(int mode) {
        GL11.glMatrixMode(mode);
    }

    public static void glPushMatrix() {
        GL11.glPushMatrix();
    }

    public static void glPopMatrix() {
        GL11.glPopMatrix();
    }

    public static void glColor4f(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    public static void ortho(float left, float right, float bottom, float top, float near, float far) {
		GL11.glOrtho(left, right, bottom, top, near, far);
    }

    public static void translate(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    public static void glLoadIdentity() {
        GL11.glLoadIdentity();
    }

    public static void glEnable(int mode) {
        GL11.glEnable(mode);
    }

    public static void glClear(int flags) {
        GL11.glClear(flags);
    }

    public static void glClearColor(float r, float g, float b, float a) {
        GL11.glClearColor(r, g, b, a);
    }

    public static void renderRect(float x0, float x1, float y0, float y1, float z, float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
        GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_LINE);
        GL11.glPolygonMode(GL11.GL_BACK, GL11.GL_LINE);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3f(x0, y0, z);
        GL11.glVertex3f(x1, y0, z);
        GL11.glVertex3f(x1, y1, z);
        GL11.glVertex3f(x0, y1, z);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_FILL);
        GL11.glPolygonMode(GL11.GL_BACK, GL11.GL_FILL);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
