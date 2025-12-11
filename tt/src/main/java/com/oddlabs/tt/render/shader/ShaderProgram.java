package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.resource.NativeResource;
import com.oddlabs.tt.util.GLState;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oddlabs.tt.util.GLUtils.checkGLError;

public abstract class ShaderProgram extends NativeResource<ShaderProgram.Program> implements Shader {
    private static final Logger logger = Logger.getLogger(ShaderProgram.class.getSimpleName());
    private static final AtomicReference<ShaderProgram> inUse = new AtomicReference<>();
    private static final FloatBuffer matrixBuffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(16)).clear();

    static final class Program extends NativeResource.NativeState {
        private final int programId;
        private final int vertexShaderId;
        private final int fragmentShaderId;
        private final Map<@NonNull String, @NonNull Integer> uniformLocations = new HashMap<>();
        private final Map<@NonNull String, @NonNull Integer> attributeLocations = new HashMap<>();

        Program(int vertexShaderId, int fragmentShaderId) {
            this.vertexShaderId = vertexShaderId;
            this.fragmentShaderId = fragmentShaderId;
            programId = GL20.glCreateProgram();

            GL20.glAttachShader(programId, vertexShaderId);
            GL20.glAttachShader(programId, fragmentShaderId);
            GL20.glLinkProgram(programId);

            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalArgumentException("Shader link failed: " + GL20.glGetProgramInfoLog(programId, 1024));
            }
        }

        @Override
        public void close() {
            GL20.glDetachShader(programId, vertexShaderId);
            GL20.glDetachShader(programId, fragmentShaderId);
            GL20.glDeleteShader(vertexShaderId);
            GL20.glDeleteShader(fragmentShaderId);
            GL20.glDeleteProgram(programId);
        }
    }

    public ShaderProgram(int vertexProgramId, int fragmentProgramId) {
        super(new Program(vertexProgramId, fragmentProgramId));
    }

    public ShaderProgram(@NonNull String vertexSource, @NonNull String fragmentSource) throws IllegalArgumentException {
        this(compileShader(GL20.GL_VERTEX_SHADER, vertexSource), compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource));
    }

    private static int compileShader(int type, @NonNull String source) throws IllegalArgumentException {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shaderId, 1024);
            GL20.glDeleteShader(shaderId);
            throw new IllegalArgumentException("Shader compilation failed: " + log);
        }
        return shaderId;
    }

    public @NonNull GLState use() {
        var active = inUse.compareAndExchange(null, this);
        if (null == active) {
            GL20.glUseProgram(state.programId);
            checkGLError("glUseProgram:" + state.programId);
            return this::disuse;
        }
        var ise = new IllegalStateException("Shader already in use; active=" + active);
        logger.log(Level.SEVERE, ise.getMessage(), ise);
        throw ise; // Throw the exception
    }

    public static @Nullable ShaderProgram activeShader() {
        return inUse.get();
    }

    @Override
    public boolean inUse() {
        return this == inUse.get();
    }

    public int getAttributeLocation(@NonNull String name) {
        return state.attributeLocations.computeIfAbsent(name, n -> GL20.glGetAttribLocation(state.programId, n));
    }

    public int getUniformLocation(@NonNull String name) {
        return state.uniformLocations.computeIfAbsent(name, n -> GL20.glGetUniformLocation(state.programId, n));
    }

    public void setUniform(@NonNull String name, int value) {
        GL20.glUniform1i(getUniformLocation(name), value);
    }

    public void setUniform(@NonNull String name, float value) {
        GL20.glUniform1f(getUniformLocation(name), value);
    }

    public void setUniform(@NonNull String name, boolean value) {
        GL20.glUniform1i(getUniformLocation(name), value ? 1 : 0);
    }

    public void setUniform(@NonNull String name, float x, float y) {
        GL20.glUniform2f(getUniformLocation(name), x, y);
    }

    public void setUniform(@NonNull String name, float x, float y, float z) {
        GL20.glUniform3f(getUniformLocation(name), x, y, z);
    }

    public void setUniform(@NonNull String name, float x, float y, float z, float w) {
        GL20.glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    public void setUniform(@NonNull String name, float @NonNull [] value) {
        GL20.glUniform4f(getUniformLocation(name), value[0], value[1], value[2], value[3]);
    }

    public void setUniformMatrix4(@NonNull String name, boolean transpose, @NonNull Matrix4fc matrix) {
        synchronized (ShaderProgram.matrixBuffer) {
            setUniformMatrix4(name, transpose, matrix.get(ShaderProgram.matrixBuffer));
        }
    }

    public void setUniformMatrix4(@NonNull String name, boolean transpose, @NonNull FloatBuffer matrix) {
        GL20.glUniformMatrix4(getUniformLocation(name), transpose, matrix);
    }

    private void disuse() {
        var active = inUse.compareAndExchange(this, null);
        if (this == active) {
            GL20.glUseProgram(0);
            checkGLError("glUseProgram(0)");
        } else {
            var ise = new IllegalStateException("Shader not in use; active=" + active);
            logger.log(Level.SEVERE, ise.getMessage(), ise);
            throw ise; // Throw the exception
        }
    }
}
