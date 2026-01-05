package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

 public record FormData(@NonNull Box form,
						@NonNull Box slimForm,
						@NonNull ModeIconQuads formClose,
						int objectSpacing,
                        int sectionSpacing,
						int captionLeft,
						int captionY,
						int closeRight,
						int closeTop,
                        @NonNull Font captionFont) {
}
