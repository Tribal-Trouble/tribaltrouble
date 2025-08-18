package com.oddlabs.tt.vbo;

import com.oddlabs.tt.global.*;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.*;

import org.lwjgl.opengl.*;

import java.nio.*;

public final strictfp class IntVBO extends VBO {
    private IntBuffer saved_buffer = null;

    public IntVBO(int usage, int size) {
        super(GL15.GL_ELEMENT_ARRAY_BUFFER, usage, size << 2);
        ByteBuffer buffer = ByteBuffer.allocate(size << 2);
        saved_buffer = buffer.asIntBuffer();
    }

    public IntVBO(int usage, IntBuffer initial_data) {
        this(usage, initial_data.remaining());
        put(initial_data);
    }

    private static void registerTrianglesRendered(int mode, int count) {
        int num_triangles = getNumTriangles(mode, count);
        Renderer.registerTrianglesRendered(num_triangles);
    }

    private static int getNumTriangles(int mode, int count) {
        switch (mode) {
            case GL11.GL_TRIANGLES:
                return count / 3;
            case GL11.GL_QUADS:
                return count >> 2;
            case GL11.GL_TRIANGLE_FAN:
            case GL11.GL_TRIANGLE_STRIP:
                return count - 2;
            case GL11.GL_QUAD_STRIP:
                return count - 3;
            case GL11.GL_LINES:
                return count; // Assume a line is two triangles
            case GL11.GL_POINTS:
                return count * 3; // assume a line is one triangle;
            case GL11.GL_LINE_STRIP:
                return (count - 1) * 2;
            default:
                throw new RuntimeException(
                        "Unknown primitive type: 0x" + Integer.toHexString(mode));
        }
    }

    public final void drawRangeElements(int mode, int start, int end, int count, int index) {
        /*
        registerTrianglesRendered(mode, count);
        if (!use_vbo) {
        	saved_buffer.position(index);
        	saved_buffer.limit(index + count);
        	if (GL.getCapabilities().OpenGL12)
        		GL12.glDrawRangeElements(mode, start, end, saved_buffer);
        	else
        		GL11.glDrawElements(mode, saved_buffer);
        	saved_buffer.clear();
        } else {
        	makeCurrent();
        	if (Settings.getSettings().use_vbo_draw_range_elements && GL.getCapabilities().OpenGL12)
        		GL12.glDrawRangeElements(mode, start, end, count, GL11.GL_INT, index<<2);
        	else
        		GL11.glDrawElements(mode, count, GL11.GL_INT, index<<2);
        }
               */
    }

    public final void put(IntBuffer buffer) {
        if (!use_vbo) {
            saved_buffer.put(buffer);
        } else {
            makeCurrent();
            GL15.glBufferSubData(getTarget(), 0, buffer);
            buffer.position(buffer.limit());
        }
    }

    public final void put(int[] buffer) {
        put(Utils.toBuffer(buffer));
    }

    public final void drawElements(int mode, int count, int index) {
        /*
        registerTrianglesRendered(mode, count);
        if (!use_vbo) {
        	saved_buffer.position(index);
        	saved_buffer.limit(index + count);
        	GL11.glDrawElements(mode, saved_buffer);
        	saved_buffer.clear();
        } else {
        	makeCurrent();
        	GL11.glDrawElements(mode, count, GL11.GL_INT, index<<2);
        }
              */
    }

    public final int capacity() {
        return getSize() >> 2;
    }
}
