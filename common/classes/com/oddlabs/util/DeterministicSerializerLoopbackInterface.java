package com.oddlabs.util;

public interface DeterministicSerializerLoopbackInterface<T> {
	void saveSucceeded();
	void loadSucceeded(T object);
	void failed(Throwable e);
}
