package com.oddlabs.tt.vbo;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public final class FloatVBO extends VBO {

    public FloatVBO(int usage, int size) {
        super(GL15.GL_ARRAY_BUFFER, usage, size * Float.BYTES);
    }

    public FloatVBO(int usage, @NonNull FloatBuffer initial_data) {
        this(usage, initial_data.remaining());
        put(initial_data);
    }

    public void vertexAttribPointer(int location, int size, int stride, long offset) {
        bind();
        GL20.glVertexAttribPointer(location, size, GL11.GL_FLOAT, false, stride, offset);
    }

    public void put(@NonNull FloatBuffer buffer) {
        putSubData(0, buffer);
    }

    public void putSubData(int index, @NonNull FloatBuffer buffer) {
        bind();
        GL15.glBufferSubData(getTarget(), (long) index << 2, buffer);
        buffer.position(buffer.limit());
    }

    public void orphan() {
        bind();
        // Reallocate buffer storage to orphan the previous buffer
        GL15.glBufferData(getTarget(), getSize(), GL15.GL_STREAM_DRAW);
    }

    @Override
    public int capacity() {
        return getSize() / Float.BYTES;
    }
}
