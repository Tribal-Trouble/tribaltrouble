package com.oddlabs.converter;

public final class Bone {
	private final String name;
	private final byte index;
	private final Bone[] children;

	public Bone(String name, byte index, Bone[] children) {
		this.name = name;
		this.children = children;
		this.index = index;
	}

	public Bone[] getChildren() {
		return children;
	}

	public byte getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}
}
