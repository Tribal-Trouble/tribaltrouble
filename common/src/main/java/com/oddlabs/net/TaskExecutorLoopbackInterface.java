package com.oddlabs.net;

import org.jspecify.annotations.NonNull;

public interface TaskExecutorLoopbackInterface<T> {
    void taskCompleted(T result);

    void taskFailed(@NonNull Throwable e);
}
