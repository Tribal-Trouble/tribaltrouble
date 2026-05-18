package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public record MapIslandData(@NonNull ModeIconQuads button,
                            int x,
                            int y,
                            @NonNull IconQuad flag,
                            @NonNull IconQuad boat,
                            int pinX,
                            int pinY) {

}
