package com.oddlabs.net;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

public interface ARMIArgumentReader {
    @Nullable
    Object readArgument(Class<?> type, ByteBufferInputStream in) throws IOException, ClassNotFoundException;
}
