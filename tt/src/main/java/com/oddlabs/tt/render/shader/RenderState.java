package com.oddlabs.tt.render.shader;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public final class RenderState {
	private static final int MAX_TEXTURE_UNITS = 8;
	
	private final @Nullable ShaderProgram shader;
	private final Texture @Nullable [] textures;
	private final boolean depthTest;
	private final boolean blend;
	private final boolean cullFace;
	
	private RenderState(@Nullable ShaderProgram shader, Texture @Nullable [] textures, 
	                    boolean depthTest, boolean blend, boolean cullFace) {
		this.shader = shader;
		this.textures = textures;
		this.depthTest = depthTest;
		this.blend = blend;
		this.cullFace = cullFace;
	}
	
	public static @NonNull Builder builder() {
		return new Builder();
	}
	
	public void apply() {
		if (shader != null) {
			shader.use();
		}
		
		if (textures != null) {
			for (int i = 0; i < textures.length && i < MAX_TEXTURE_UNITS; i++) {
				if (textures[i] != null) {
					GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[i].getHandle());
				}
			}
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
		}
		
		if (depthTest) {
			GL11.glEnable(GL11.GL_DEPTH_TEST);
		} else {
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		}
		
		if (blend) {
			GL11.glEnable(GL11.GL_BLEND);
		} else {
			GL11.glDisable(GL11.GL_BLEND);
		}
		
		if (cullFace) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		} else {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}
	}
	
	public boolean isCompatible(@NonNull RenderState other) {
		if (shader != other.shader) return false;
		if (depthTest != other.depthTest) return false;
		if (blend != other.blend) return false;
		if (cullFace != other.cullFace) return false;
		
		if (textures == null && other.textures == null) return true;
		if (textures == null || other.textures == null) return false;
		
		int minLength = Math.min(textures.length, other.textures.length);
		for (int i = 0; i < minLength; i++) {
			if (textures[i] != other.textures[i]) return false;
		}
		return true;
	}
	
	public static final class Builder {
		private @Nullable ShaderProgram shader;
		private Texture @Nullable [] textures;
		private boolean depthTest = true;
		private boolean blend = false;
		private boolean cullFace = true;
		
		public @NonNull Builder shader(@Nullable ShaderProgram shader) {
			this.shader = shader;
			return this;
		}
		
		public @NonNull Builder texture(@NonNull Texture texture) {
			this.textures = new Texture[]{texture};
			return this;
		}
		
		public @NonNull Builder textures(Texture @NonNull ... textures) {
			this.textures = textures;
			return this;
		}
		
		public @NonNull Builder depthTest(boolean enable) {
			this.depthTest = enable;
			return this;
		}
		
		public @NonNull Builder blend(boolean enable) {
			this.blend = enable;
			return this;
		}
		
		public @NonNull Builder cullFace(boolean enable) {
			this.cullFace = enable;
			return this;
		}
		
		public @NonNull RenderState build() {
			return new RenderState(shader, textures, depthTest, blend, cullFace);
		}
	}
}
