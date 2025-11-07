package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public final class ShaderProgram extends NativeResource<ShaderProgram.Program> {
    static final class Program extends NativeResource.NativeState {
        private final int programId;
        private final int vertexShaderId;
        private final int fragmentShaderId;

        Program(int vertexShaderId, int fragmentShaderId) {
            this.vertexShaderId = vertexShaderId;
            this.fragmentShaderId = fragmentShaderId;
            programId = GL20.glCreateProgram();

            GL20.glAttachShader(programId, vertexShaderId);
            GL20.glAttachShader(programId, fragmentShaderId);
            GL20.glLinkProgram(programId);

            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException("Shader link failed: " + GL20.glGetProgramInfoLog(programId, 1024));
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
	
	public ShaderProgram(@NonNull String vertexSource, @NonNull String fragmentSource) {
        super(new Program(compileShader(GL20.GL_VERTEX_SHADER, vertexSource),
                compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource)));
	}
	
	private static int compileShader(int type, @NonNull String source) {
		int shaderId = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderId, source);
		GL20.glCompileShader(shaderId);
		
		if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			String log = GL20.glGetShaderInfoLog(shaderId, 1024);
			GL20.glDeleteShader(shaderId);
			throw new RuntimeException("Shader compilation failed: " + log);
		}
		return shaderId;
	}
	
	public void use() {
		GL20.glUseProgram(state.programId);
	}
	
	public int getAttributeLocation(@NonNull String name) {
		return GL20.glGetAttribLocation(state.programId, name);
	}
	
	public int getUniformLocation(@NonNull String name) {
		return GL20.glGetUniformLocation(state.programId, name);
	}
	
	public void setUniform(@NonNull String name, int value) {
		GL20.glUniform1i(getUniformLocation(name), value);
	}
	
	public void setUniform(@NonNull String name, float value) {
		GL20.glUniform1f(getUniformLocation(name), value);
	}
	
	public void setUniform(@NonNull String name, float x, float y, float z) {
		GL20.glUniform3f(getUniformLocation(name), x, y, z);
	}
	
	public void setUniform(@NonNull String name, float x, float y, float z, float w) {
		GL20.glUniform4f(getUniformLocation(name), x, y, z, w);
	}
	
	public void setUniformMatrix4(@NonNull String name, boolean transpose, @NonNull FloatBuffer matrix) {
		GL20.glUniformMatrix4(getUniformLocation(name), transpose, matrix);
	}

	public static void unbind() {
		GL20.glUseProgram(0);
	}
}
