package com.oddlabs.net;

public interface TaskExecutorLoopbackInterface<T> {
	void taskCompleted(T result);
	void taskFailed(Exception e);
}
