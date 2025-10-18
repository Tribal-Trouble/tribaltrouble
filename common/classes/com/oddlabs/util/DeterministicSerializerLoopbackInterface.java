package com.oddlabs.util;

public interface DeterministicSerializerLoopbackInterface<T> {
	public void saveSucceeded();
	public void loadSucceeded(T object);
	public void failed(Throwable e);
}
