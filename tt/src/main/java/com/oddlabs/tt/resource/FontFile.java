package com.oddlabs.tt.resource;

import com.oddlabs.tt.font.Font;
import com.oddlabs.util.FontInfo;
import org.jspecify.annotations.NonNull;

public final class FontFile extends File<Font> {

    public FontFile(String file_name) {
        super(file_name);
    }

    @Override
    public @NonNull Font get() {
        FontInfo font_info = FontInfo.loadFromFile(getURL());
        return new Font(font_info);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof FontFile) && super.equals(o);
    }
}
