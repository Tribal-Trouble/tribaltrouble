package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

record SliderData(@NonNull Horizontal slider,
                  @NonNull ModeIconQuads button,
                  int leftOffset,
                  int rightOffset) {
}
