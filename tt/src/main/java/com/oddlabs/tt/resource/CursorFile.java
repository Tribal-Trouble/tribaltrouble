package com.oddlabs.tt.resource;

import com.oddlabs.tt.gui.Cursor;
import com.oddlabs.util.Image;
import org.jspecify.annotations.NonNull;

public final class CursorFile extends File<Cursor> {

    private final int xHot, yHot;

    public CursorFile(@NonNull String source, int xHot, int yHot) {
        super(source);
        this.xHot = xHot;
        this.yHot = yHot;
    }

    @Override
    public @NonNull Cursor get() {
        return new Cursor(Image.read(getURL()), xHot, yHot);
    }
}
