package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;

import java.nio.FloatBuffer;

public interface Shader {

    boolean inUse();

    int getAttributeLocation(@NonNull String name);

    int getUniformLocation(@NonNull String name);

    void setUniform(@NonNull String name, int value);

    void setUniform(@NonNull String name, float value);

    void setUniform(@NonNull String name, boolean value);

    void setUniform(@NonNull String name, float x, float y);

    void setUniform(@NonNull String name, float x, float y, float z);

    void setUniform(@NonNull String name, float x, float y, float z, float w);

    void setUniform(@NonNull String name, float @NonNull [] value);

    void setUniformMatrix4(@NonNull String name, boolean transpose, @NonNull FloatBuffer matrix);
}

