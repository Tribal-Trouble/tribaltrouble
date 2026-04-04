package com.oddlabs.tt.guievent;

import com.oddlabs.tt.input.InputEvent;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface InputListener extends EventListener {
    void handleInput(@NonNull InputEvent event);
}
