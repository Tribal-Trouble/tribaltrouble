package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.Set;

public final class VertexLayout {

	private final Set<VertexAttribute> attributes;
	private final int stride;
	
	private VertexLayout(@NonNull Set<VertexAttribute> attributes) {
		this.attributes = EnumSet.copyOf(attributes);
		this.stride = attributes.stream().mapToInt(VertexAttribute::getSizeBytes).sum();
	}
	
	public static @NonNull VertexLayout of(@NonNull VertexAttribute... attributes) {
		return new VertexLayout(EnumSet.of(attributes[0], attributes));
	}
	
	public int getStride() {
		return stride;
	}
	
	public boolean has(@NonNull VertexAttribute attribute) {
		return attributes.contains(attribute);
	}
	
	public int getOffset(@NonNull VertexAttribute attribute) {
		int offset = 0;
		for (VertexAttribute attr : VertexAttribute.values()) {
			if (attr == attribute) {
				return offset;
			}
			if (attributes.contains(attr)) {
				offset += attr.getSizeBytes();
			}
		}
		throw new IllegalArgumentException("Attribute not in layout: " + attribute);
	}
	
	public void bind(@NonNull ShaderProgram shader) {
		for (VertexAttribute attr : attributes) {
			int location = shader.getAttributeLocation(attr.getName());
			if (location >= 0) {
				attr.enable(location);
				attr.setPointer(location, stride, getOffset(attr) / Float.BYTES);
			}
		}
	}
	
	public void unbind(@NonNull ShaderProgram shader) {
		for (VertexAttribute attr : attributes) {
			int location = shader.getAttributeLocation(attr.getName());
			if (location >= 0) {
				attr.disable(location);
			}
		}
	}
}
