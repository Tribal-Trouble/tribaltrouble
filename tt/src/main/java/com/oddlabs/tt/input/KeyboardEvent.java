package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;

public record KeyboardEvent(@NonNull Key keyCode, char keyChar, boolean shiftDown, boolean controlDown, boolean altDown, boolean metaDown, int clicks) {
}
