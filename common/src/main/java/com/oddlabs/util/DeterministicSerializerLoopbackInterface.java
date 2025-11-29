package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

public interface DeterministicSerializerLoopbackInterface<T> {
	void saveSucceeded();
	void loadSucceeded(T object);
	void failed(@NonNull Throwable e);
}
