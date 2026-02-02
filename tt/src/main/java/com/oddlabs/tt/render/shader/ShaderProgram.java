package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.tt.resource.NativeResource;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oddlabs.tt.util.GLUtils.checkGLError;

public abstract class ShaderProgram extends NativeResource<ShaderProgram.Program> implements Shader {
    private static final Logger logger = Logger.getLogger(ShaderProgram.class.getSimpleName());
    /** the currently active shader or null */
    private static final AtomicReference<@Nullable ShaderProgram> inUse = new AtomicReference<>();

    static final class Program extends NativeResource.NativeState {
        final int programId;
        private final int vertexShaderId;
        private final int fragmentShaderId;
        private final int geometryShaderId;
        private final Map<@NonNull String, @NonNull Integer> uniformLocations = new HashMap<>();
        private final Map<@NonNull String, @NonNull Integer> attributeLocations = new HashMap<>();

        Program(int vertexShaderId, int fragmentShaderId, int geometryShaderId) {
            this.vertexShaderId = vertexShaderId;
            this.fragmentShaderId = fragmentShaderId;
            this.geometryShaderId = geometryShaderId;
            this.programId = GL20.glCreateProgram();

            GL20.glAttachShader(programId, vertexShaderId);
            GL20.glAttachShader(programId, fragmentShaderId);
            if (geometryShaderId != 0) {
                GL20.glAttachShader(programId, geometryShaderId);
            }
        }

        /** complete linking after (optionally) setting up shader layouts */
        void link() {
            bindStandardAttributes();
            GL20.glLinkProgram(programId);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalArgumentException("Shader link failed: " + GL20.glGetProgramInfoLog(programId, 1024));
            }
        }

        private void bindStandardAttributes() {
            GL20.glBindAttribLocation(programId, POSITION_LOC, POSITION);
            GL20.glBindAttribLocation(programId, NORMAL_LOC, NORMAL);
            GL20.glBindAttribLocation(programId, TEX_COORD_LOC, TEX_COORD);
            GL20.glBindAttribLocation(programId, COLOR_LOC, COLOR);
        }

        @Override
        public void close() {
            GL20.glDetachShader(programId, vertexShaderId);
            GL20.glDetachShader(programId, fragmentShaderId);
            GL20.glDeleteShader(vertexShaderId);
            GL20.glDeleteShader(fragmentShaderId);
            if (geometryShaderId != 0) {
                GL20.glDetachShader(programId, geometryShaderId);
                GL20.glDeleteShader(geometryShaderId);
            }
            GL20.glDeleteProgram(programId);
        }
    }

    public ShaderProgram(@NonNull String vertexSource, @NonNull String fragmentSource) throws IllegalArgumentException {
        this(vertexSource, fragmentSource, null);
    }
    
    public ShaderProgram(@NonNull String vertexSource, @NonNull String fragmentSource, @Nullable String geometrySource) throws IllegalArgumentException {
        super(new Program(
            compileShader(GL20.GL_VERTEX_SHADER, vertexSource), 
            compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource),
            geometrySource != null ? compileShader(GL32.GL_GEOMETRY_SHADER, geometrySource) : 0
        ));
    }

    protected void bindFragDataLocation(int colorNumber, @NonNull String name) {
        GL30.glBindFragDataLocation(state.programId, colorNumber, name);
    }

    /** complete linking after (optionally) setting up shader layouts */
    protected void link() {
        state.link();
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

    public @NonNull ScopedState use() {
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
        return state.attributeLocations.computeIfAbsent(name, n -> {
            int loc = GL20.glGetAttribLocation(state.programId, n);
            return loc;
        });
    }

    public int getUniformLocation(@NonNull String name) {
        return state.uniformLocations.computeIfAbsent(name, n -> {
            int loc = GL20.glGetUniformLocation(state.programId, n);
            return loc;
        });
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            setUniformMatrix4(name, transpose, matrix.get(stack.mallocFloat(16)));
        }
    }

    public void setUniformMatrix4(@NonNull String name, boolean transpose, @NonNull FloatBuffer matrix) {
        GL20.glUniformMatrix4fv(getUniformLocation(name), transpose, matrix);
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
