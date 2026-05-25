package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class Horizontal {
    private final @NonNull ModeIconQuads left;
    private final @NonNull ModeIconQuads center;
    private final @NonNull ModeIconQuads right;
    private final int height;
    private final int left_width;
    private final int right_width;

    public Horizontal(@NonNull ModeIconQuads left, @NonNull ModeIconQuads center, @NonNull ModeIconQuads right) {
        this.left = left;
        this.center = center;
        this.right = right;
        height = left.quad(ModeIconQuads.Mode.NORMAL).getHeight();
        left_width = left.quad(ModeIconQuads.Mode.NORMAL).getWidth();
        right_width = right.quad(ModeIconQuads.Mode.NORMAL).getWidth();
    }

    public void render(@NonNull GUIRenderer renderer, float x, float y, int width,
            ModeIconQuads.@NonNull Mode skinMode) {
        int center_width = width - left_width - right_width;

        renderer.drawModeIcon(left, skinMode, x, y);
        renderer.drawIcon(center.quad(skinMode), x + left_width, y, center_width, height);
        renderer.drawModeIcon(right, skinMode, x + left_width + center_width, y);
    }

    public int getHeight() {
        return height;
    }
}
