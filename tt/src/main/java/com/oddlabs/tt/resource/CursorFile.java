package com.oddlabs.tt.resource;

import com.oddlabs.tt.gui.Cursor;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class CursorFile extends File<Cursor> {

    private final int xHot, yHot;

    /**
     * Create a new cursor instance from an image
     *
     * @param source resource path of cursor image
     * @param xHot   x location from top left of cursor hot spot
     * @param yHot   y location from top left of cursor hot spot
     */
    public CursorFile(@NonNull String source, int xHot, int yHot) {
        super(source);
        this.xHot = xHot;
        this.yHot = yHot;
    }

    @Override
    public @NonNull Cursor get() {
        try {
            return new Cursor(GLIntImage.loadImage(getURL()), xHot, yHot);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
