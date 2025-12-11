package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;

import java.io.File;

public class ObjectInfo {
	private final @NonNull File file;

	public ObjectInfo(@NonNull File file) {
		this.file = file;
	}

	public final @NonNull File getFile() {
		return file;
	}
}
