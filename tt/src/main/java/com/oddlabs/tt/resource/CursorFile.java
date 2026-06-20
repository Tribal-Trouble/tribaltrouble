package com.oddlabs.tt.resource;

import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.gui.Cursor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A resource file for loading and scaling hardware cursors.
 */
public final class CursorFile extends File<Cursor> {

    private final int xHot, yHot;
    private final float scale;

    /**
     * Create a new cursor instance from an image
     *
     * @param source resource path of cursor image
     * @param xHot x location from top left of cursor hot spot
     * @param yHot y location from top left of cursor hot spot
     */
    public CursorFile(@NonNull String source, int xHot, int yHot) {
        this(source, xHot, yHot, 1.0f);
    }

    /**
     * Create a new cursor instance from an image with a specific scale.
     *
     * @param source resource path of cursor image
     * @param xHot x location from top left of cursor hot spot
     * @param yHot y location from top left of cursor hot spot
     * @param scale scale factor to apply to the cursor image
     */
    public CursorFile(@NonNull String source, int xHot, int yHot, float scale) {
        super(source);
        this.xHot = xHot;
        this.yHot = yHot;
        this.scale = scale;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof CursorFile other && super.equals(o) &&
                xHot == other.xHot && yHot == other.yHot
                && Float.compare(scale, other.scale) == 0;
    }

    @Override
    public @NonNull Cursor get() {
        try {
            GLIntImage image = GLIntImage.loadImage(getURL());
            if (scale != 1.0f) {
                Layer layer = image.toLayer();
                int newWidth = Math.max(1, Math.round(image.getWidth() * scale));
                int newHeight = Math.max(1, Math.round(image.getHeight() * scale));
                layer.scale(newWidth, newHeight);
                image = new GLIntImage(layer);
            }
            return new Cursor(image, Math.round(xHot * scale), Math.round(yHot * scale));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
