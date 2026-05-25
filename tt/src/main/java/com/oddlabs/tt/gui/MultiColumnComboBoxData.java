package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public record MultiColumnComboBoxData(@NonNull Box box,
                                      @NonNull Horizontal buttonPressed,
                                      @NonNull Horizontal buttonUnpressed,
                                      @NonNull ModeIconQuads descending,
                                      @NonNull ModeIconQuads ascending,
                                      @NonNull Vector4fc color1,
                                      @NonNull Vector4fc color2,
                                      @NonNull Vector4fc colorMarked,
                                      @NonNull Font font,
                                      int captionOffset) {
}
