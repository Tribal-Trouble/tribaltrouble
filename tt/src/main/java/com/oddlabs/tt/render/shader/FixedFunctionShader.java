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
		String POINT_SIZE = "u_pointSize";
	}
	
	public interface Attributes {
		String POSITION = "in_Position";
		String NORMAL = "in_Normal";
		String COLOR = "in_Color";
		String TEX_COORD_0 = "in_TexCoord0";
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
		#version 410 core
		""" +
		LIGHTING_FUNCTION +
		"""
		layout(location = 0) in vec3 in_Position;
		layout(location = 1) in vec3 in_Normal;
		layout(location = 2) in vec4 in_Color;
		layout(location = 3) in vec2 in_TexCoord0;
		
		uniform mat4 u_modelViewMatrix;
		uniform mat4 u_projectionMatrix;
		uniform bool u_enableLighting;
		uniform vec3 u_lightDirection;
		uniform vec3 u_globalAmbient;
		uniform float u_pointSize;
		
		out vec4 v_color;
		out vec2 v_texCoord0;
		out float v_fogDist;
		
		void main() {
		    vec4 viewPosition = u_modelViewMatrix * vec4(in_Position, 1.0);
			gl_Position = u_projectionMatrix * viewPosition;
			v_texCoord0 = in_TexCoord0;
			v_fogDist = length(viewPosition.xyz);
			
			if (u_pointSize > 0.0) {
			    gl_PointSize = u_pointSize;
			}
		
			if (u_enableLighting) {
				v_color = calculateLighting(in_Normal, in_Color, u_modelViewMatrix, u_lightDirection, u_globalAmbient);
			} else {
				v_color = in_Color;
			}
		}
		""";
	
	private static final String FRAGMENT_SHADER =
		"""
		#version 410 core
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
		
		in vec4 v_color;
		in vec2 v_texCoord0;
		in float v_fogDist;
		
		layout(location = 0) out vec4 out_FragColor;
		
		void main() {
			vec4 color;
			if (u_enableTexture) {
			    vec4 texColor = texture(u_texture0, v_texCoord0);
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
		
            float fogFactor = calculateFogFactor(u_fogMode, u_fogParams, u_fogHeightFactor, u_cameraHeight, v_fogDist, gl_FragCoord.xy);
			out_FragColor = vec4(mix(u_fogColor.rgb, color.rgb, fogFactor), color.a);
		}
		""";
	
	public FixedFunctionShader() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        // bindFragDataLocation(0, "out_FragColor");
        link();
    }
}
