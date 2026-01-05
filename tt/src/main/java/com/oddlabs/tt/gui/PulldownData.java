package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

record PulldownData(@NonNull Horizontal pulldownTop,
                    @NonNull Horizontal pulldownBottom,
                    @NonNull Box pulldownItem,
                    @NonNull Horizontal pulldownButton,
                    @NonNull ModeIconQuads arrow,
                    int arrowOffsetRight,
                    int textOffsetLeft,
                    @NonNull Font font) {

}