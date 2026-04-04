package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;

public final class SpamFilter {
    public static @NonNull String scan(String string) {
        string = string.replaceAll("\\s+", " ");
        string = string.replaceAll("\\.{3,}", "…");
        string = string.replaceAll("\\?+", "?");
        string = string.replaceAll("\\!+", "!");
        return string;
    }

    private SpamFilter() {
    }
}
