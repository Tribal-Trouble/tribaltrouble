package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public enum VertexAttribute {
	POSITION(FixedFunctionShader.Attributes.POSITION, 3, GL11.GL_FLOAT),
	NORMAL(FixedFunctionShader.Attributes.NORMAL, 3, GL11.GL_FLOAT),
	COLOR(FixedFunctionShader.Attributes.COLOR, 4, GL11.GL_FLOAT),
	TEX_COORD_0(FixedFunctionShader.Attributes.TEX_COORD_0, 2, GL11.GL_FLOAT);

	private final @NonNull String name;
	private final int componentCount;
	private final int glType;
	
	VertexAttribute(@NonNull String name, int componentCount, int glType) {
		this.name = name;
		this.componentCount = componentCount;
		this.glType = glType;
	}
	
	public @NonNull String getName() {
		return name;
	}
	
	public int getComponentCount() {
		return componentCount;
	}
	
	public int getGlType() {
		return glType;
	}
	
	public int getSizeBytes() {
		return componentCount * Float.BYTES;
	}
	
	public void enable(int location) {
		GL20.glEnableVertexAttribArray(location);
	}
	
	public void disable(int location) {
		GL20.glDisableVertexAttribArray(location);
	}
	
	public void setPointer(int location, int stride, int offset) {
		GL20.glVertexAttribPointer(location, componentCount, glType, false, stride, offset * (long) Float.BYTES);
	}
}
