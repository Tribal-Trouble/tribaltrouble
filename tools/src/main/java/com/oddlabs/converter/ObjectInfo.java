package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Path;

public class ObjectInfo {
	private final @NonNull Path file;

	public ObjectInfo(@NonNull Path file) {
		this.file = file;
	}

	public final @NonNull Path getFile() {
		return file;
	}
}
