package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/** A shader that emulates the classic OpenGL fixed-function pipeline. */
public final class FixedFunctionShader extends ShaderProgram {
	
	public static final class Uniforms {
		public static final String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
		public static final String PROJECTION_MATRIX = "u_projectionMatrix";
		public static final String ENABLE_LIGHTING = "u_enableLighting";
		public static final String ENABLE_TEXTURE = "u_enableTexture";
		public static final String TEXTURE_0 = "u_texture0";
		
		private Uniforms() {}
	}
	
	public static final class Attributes {
		public static final String POSITION = "a_position";
		public static final String NORMAL = "a_normal";
		public static final String COLOR = "a_color";
		public static final String TEX_COORD_0 = "a_texCoord0";
		
		private Attributes() {}
	}

	public enum Attribute implements VertexAttribute {
		POSITION(Attributes.POSITION, 3, GL11.GL_FLOAT),
		NORMAL(Attributes.NORMAL, 3, GL11.GL_FLOAT),
		COLOR(Attributes.COLOR, 4, GL11.GL_FLOAT),
		TEX_COORD_0(Attributes.TEX_COORD_0, 2, GL11.GL_FLOAT);

		private final @NonNull String name;
		private final int componentCount;
		private final int glType;
		private final boolean normalized;

		Attribute(@NonNull String name, int componentCount, int glType) {
			this(name, componentCount, glType, false);
		}

		Attribute(@NonNull String name, int componentCount, int glType, boolean normalized) {
			this.name = name;
			this.componentCount = componentCount;
			this.glType = glType;
			this.normalized = normalized;
		}

		@Override
		public @NonNull String getName() {
			return name;
		}

		@Override
		public int getComponentCount() {
			return componentCount;
		}

		@Override
		public int getGlType() {
			return glType;
		}

		@Override
		public boolean isNormalized() {
			return normalized;
		}
	}

	private static final String VERTEX_SHADER = """
		#version 120
		
		attribute vec3 a_position;
		attribute vec3 a_normal;
		attribute vec4 a_color;
		attribute vec2 a_texCoord0;
		
		uniform mat4 u_modelViewMatrix;
		uniform mat4 u_projectionMatrix;
		uniform bool u_enableLighting;
		
		varying vec4 v_color;
		varying vec2 v_texCoord0;
		
		void main() {
			gl_Position = u_projectionMatrix * u_modelViewMatrix * vec4(a_position, 1.0);
			v_texCoord0 = a_texCoord0;
		
			if (u_enableLighting) {
				vec3 normal = normalize((u_modelViewMatrix * vec4(a_normal, 0.0)).xyz);
				float diffuse = max(dot(normal, vec3(0.0, 0.0, 1.0)), 0.0);
				v_color = a_color * (0.3 + 0.7 * diffuse);
			} else {
				v_color = a_color;
			}
		}
		""";
	
	private static final String FRAGMENT_SHADER = """
		#version 120
		
		uniform sampler2D u_texture0;
		uniform bool u_enableTexture;
		
		varying vec4 v_color;
		varying vec2 v_texCoord0;
		
		void main() {
			vec4 color = v_color;
			if (u_enableTexture) {
				color *= texture2D(u_texture0, v_texCoord0);
			}
			gl_FragColor = color;
		}
		""";
	
	public FixedFunctionShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}
