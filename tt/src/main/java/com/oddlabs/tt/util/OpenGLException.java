package com.oddlabs.tt.util;

import org.jspecify.annotations.Nullable;

/**
 * Thrown to indicate a failure in an OpenGL operation.
 */
public final class OpenGLException extends RuntimeException {
    public OpenGLException(@Nullable String message) {
        super(message);
    }

    public OpenGLException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
