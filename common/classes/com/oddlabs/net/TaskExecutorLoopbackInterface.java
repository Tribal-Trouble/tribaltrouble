package com.oddlabs.net;

public interface TaskExecutorLoopbackInterface<T> {
	public void taskCompleted(T result);
	public void taskFailed(Exception e);
}
