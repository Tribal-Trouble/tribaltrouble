package com.oddlabs.tt.guievent;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface TabListener extends EventListener {
    void tabPressed(@NonNull String @NonNull [] words);
}
