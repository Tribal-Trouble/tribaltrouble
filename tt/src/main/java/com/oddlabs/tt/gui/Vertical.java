package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class Vertical {
    private final @NonNull ModeIconQuads bottom;
    private final @NonNull ModeIconQuads center;
    private final @NonNull ModeIconQuads top;
    private final int bottom_height;
    private final int top_height;
    private final int width;

    public Vertical(@NonNull ModeIconQuads bottom, @NonNull ModeIconQuads center, @NonNull ModeIconQuads top) {
        this.bottom = bottom;
        this.center = center;
        this.top = top;
        bottom_height = bottom.quad(ModeIconQuads.Mode.NORMAL).getHeight();
        top_height = top.quad(ModeIconQuads.Mode.NORMAL).getHeight();
        width = bottom.quad(ModeIconQuads.Mode.NORMAL).getWidth();
    }

    public void render(@NonNull GUIRenderer renderer, float x, float y, int height,
            ModeIconQuads.@NonNull Mode skinMode) {
        int center_height = height - bottom_height - top_height;

        renderer.drawModeIcon(bottom, skinMode, x, y);
        renderer.drawIcon(center.quad(skinMode), x, y + bottom_height, width, center_height);
        renderer.drawModeIcon(top, skinMode, x, y + bottom_height + center_height);
    }

    public int getWidth() {
        return width;
    }

    public int getMinHeight() {
        return bottom.quad(ModeIconQuads.Mode.NORMAL).getHeight() + top.quad(ModeIconQuads.Mode.NORMAL).getHeight();
    }
}
