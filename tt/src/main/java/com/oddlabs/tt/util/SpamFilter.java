package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;

public final class SpamFilter {
    public static @NonNull String scan(String string) {
        // Chat renders single line and Discord relayed messages can contain line breaks
        return string.replaceAll("[\\t\\r\\n\\f]+", " ");
    }

    private SpamFilter() {
    }
}
