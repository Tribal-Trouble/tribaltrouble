package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/** A shader that emulates the classic OpenGL fixed-function pipeline. */
public final class FixedFunctionShader extends ShaderProgram implements FogShader, LitShader {
	
	public interface Uniforms {
		String MODEL_VIEW_MATRIX = "u_modelViewMatrix";
		String PROJECTION_MATRIX = "u_projectionMatrix";
		String ENABLE_LIGHTING = "u_enableLighting";
		String ENABLE_TEXTURE = "u_enableTexture";
		String TEXTURE_0 = "u_texture0";
		String ALPHA_CUTOFF = "u_alphaCutoff";
		String REPLACE_MODE = "u_replaceMode";
		String FOG_PARAMS = "u_fogParams";
	}
	
	public interface Attributes {
		String POSITION = "a_position";
		String NORMAL = "a_normal";
		String COLOR = "a_color";
		String TEX_COORD_0 = "a_texCoord0";
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

	private static final String VERTEX_SHADER =
		"""
		#version 120
		""" +
		LIGHTING_FUNCTION +
		"""
		attribute vec3 a_position;
		attribute vec3 a_normal;
		attribute vec4 a_color;
		attribute vec2 a_texCoord0;
		
		uniform mat4 u_modelViewMatrix;
		uniform mat4 u_projectionMatrix;
		uniform bool u_enableLighting;
		uniform vec3 u_lightDirection;
		uniform vec3 u_globalAmbient;
		
		varying vec4 v_color;
		varying vec2 v_texCoord0;
		varying float v_fogDist;
		
		void main() {
		    vec4 viewPosition = u_modelViewMatrix * vec4(a_position, 1.0);
			gl_Position = u_projectionMatrix * viewPosition;
			v_texCoord0 = a_texCoord0;
			v_fogDist = length(viewPosition.xyz);
		
			if (u_enableLighting) {
				v_color = calculateLighting(a_normal, a_color, u_modelViewMatrix, u_lightDirection, u_globalAmbient);
			} else {
				v_color = a_color;
			}
		}
		""";
	
	private static final String FRAGMENT_SHADER =
		"""
		#version 120
		""" +
		FOG_FUNCTION +
		"""
		uniform sampler2D u_texture0;
		uniform bool u_enableTexture;
		uniform float u_alphaCutoff;
		uniform bool u_replaceMode;
		
		// Fog uniforms
        uniform vec4 u_fogColor;
        uniform int u_fogMode;
        uniform vec3 u_fogParams; // x = density, y = start, z = end
        uniform float u_fogHeightFactor;
        uniform float u_cameraHeight;
		
		varying vec4 v_color;
		varying vec2 v_texCoord0;
		varying float v_fogDist;
		
		void main() {
			vec4 color;
			if (u_enableTexture) {
			    vec4 texColor = texture2D(u_texture0, v_texCoord0);
			    if (u_replaceMode) {
			        color = texColor;
			    } else {
				    color = v_color * texColor;
				}
			} else {
			    color = v_color;
			}
		
			if (color.a < u_alphaCutoff) {
			    discard;
			}
		
			// Apply fog
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
			gl_FragColor = vec4(mix(u_fogColor.rgb, color.rgb, fogFactor), color.a);
		}
		""";
	
	public FixedFunctionShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }
}
