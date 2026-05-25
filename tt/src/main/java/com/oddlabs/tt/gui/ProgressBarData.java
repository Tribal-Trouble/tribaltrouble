package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

record ProgressBarData(@NonNull Horizontal progressBar,
                       @NonNull ModeIconQuads leftFill,
                       @NonNull ModeIconQuads centerFill,
                       @NonNull ModeIconQuads rightFill,
                       @NonNull Font font) {

}
