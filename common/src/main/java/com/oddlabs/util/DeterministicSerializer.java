package com.oddlabs.util;

import com.oddlabs.event.Deterministic;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DeterministicSerializer {
    private DeterministicSerializer() {
    }

    public static <T> void save(@NonNull Deterministic deterministic, final Object object, final @NonNull Path file,
            final @NonNull DeterministicSerializerLoopbackInterface<T> callback_loopback) {
        IOException exception;
        try {
            ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(file));
            os.writeObject(object);
            exception = null;
        } catch (IOException e) {
            exception = e;
        }
        if (deterministic.log(exception != null))
            callback_loopback.failed(deterministic.log(exception));
        else
            callback_loopback.saveSucceeded();
    }

    public static <T> void load(@NonNull Deterministic deterministic, final @NonNull Path file,
            final @NonNull DeterministicSerializerLoopbackInterface<T> callback_loopback) {
        T object;
        Throwable throwable;
        try {
            ObjectInputStream is = new ObjectInputStream(Files.newInputStream(file));
            //noinspection unchecked
            object = (T) is.readObject();
            throwable = null;
        } catch (Throwable all) {
            throwable = all;
            object = null;
        }
        if (deterministic.log(throwable != null)) {
            callback_loopback.failed(deterministic.log(throwable));
        } else {
            callback_loopback.loadSucceeded(deterministic.log(object));
        }
    }
}
