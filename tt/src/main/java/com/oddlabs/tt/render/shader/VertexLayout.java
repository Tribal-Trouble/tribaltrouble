package com.oddlabs.tt.render.shader;

import org.jspecify.annotations.NonNull;

import java.util.List;

public final class VertexLayout<A extends Enum<A> & VertexAttribute> {

	private final @NonNull List<@NonNull A> attributes;
	private final int stride;

    @SafeVarargs
    public VertexLayout(@NonNull A @NonNull ... attributes) {
        this.attributes = List.of(attributes);
        this.stride = this.attributes.stream()
                .mapToInt(VertexAttribute::getSizeBytes)
                .sum();
    }

	public int getStride() {
		return stride;
	}
	
	public boolean has(@NonNull A attribute) {
		return attributes.contains(attribute);
	}
	
	private int getOffset(@NonNull A attribute) {
		int offset = 0;
		for (A attr : attributes) {
			if (attr == attribute) {
				return offset;
			}
			offset += attr.getSizeBytes();
		}
		throw new IllegalArgumentException("Attribute not present in layout: " + attribute);
	}
	
	public void bind(@NonNull ShaderProgram shader) {
		for (A attr : attributes) {
			int location = shader.getAttributeLocation(attr.getName());
			if (location >= 0) {
				attr.enable(location);
				attr.setPointer(location, stride, getOffset(attr));
			}
		}
	}
	
	public void unbind(@NonNull ShaderProgram shader) {
		for (A attr : attributes) {
			int location = shader.getAttributeLocation(attr.getName());
			if (location >= 0) {
				attr.disable(location);
			}
		}
	}
}
