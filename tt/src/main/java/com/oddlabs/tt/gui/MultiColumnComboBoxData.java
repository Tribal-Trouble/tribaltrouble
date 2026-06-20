package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

public record MultiColumnComboBoxData(@NonNull Box box,
                                      @NonNull Horizontal buttonPressed,
                                      @NonNull Horizontal buttonUnpressed,
                                      @NonNull ModeIconQuads descending,
                                      @NonNull ModeIconQuads ascending,
                                      Color.@NonNull Linear color1,
                                      Color.@NonNull Linear color2,
                                      Color.@NonNull Linear colorMarked,
                                      @NonNull Font font,
                                      int captionOffset) {
}
