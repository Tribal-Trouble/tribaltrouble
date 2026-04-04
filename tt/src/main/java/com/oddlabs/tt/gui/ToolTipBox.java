package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ToolTipBox extends TextField {
    static final float MAX_DELAY_SECONDS = 1.5f;

    private @NonNull IconQuad @Nullable [] icons;

    public ToolTipBox() {
        super(Skin.getSkin().getEditFont(), 1000);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        throw new UnsupportedOperationException("ToolTipBox.renderGeometry should not be called directly. Use render(GUIRenderer, ...)");
    }

    public void append(@NonNull IconQuad @Nullable ... icons) {
        this.icons = icons;
    }

    @Override
    public void clear() {
        super.clear();
        icons = null;
    }

    public void render(@NonNull GUIRenderer renderer, int center_x, int top_y, int width, int height) {
        if (getText().isEmpty())
            return;
        ToolTipBoxInfo box = Skin.getSkin().getToolTipInfo();
        int text_width = getFont().getWidth(getText());
        int box_width = text_width + box.leftOffset() + box.rightOffset();
        int box_height = box.box().getHeight();
        if (icons != null) {
            int i;
            for (i = 0; i < icons.length; i++) {
                box_width += icons[i].getWidth() / 3;
            }
            box_width += icons[i - 1].getWidth() * 2 / 3;
        }

        float x = Math.clamp(center_x - box_width / 2f, 0, width - box_width);
        float y = Math.clamp(top_y - box_height, 0, height - box_height);

        box.box().render(renderer, x, y, box_width, ModeIconQuads.Mode.NORMAL);

        TextLineRenderer.render(renderer, getFont(), getText(), x + box.leftOffset(), y + box.bottomOffset(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Color.WHITE);
        if (icons != null) {
            float render_x = box_width - box.rightOffset() - icons[icons.length - 1].getWidth();
            for (IconQuad icon : icons) {
                renderer.drawIcon(icon, x + render_x, y + (box_height - icon.getHeight()) / 2f);
                render_x -= icon.getWidth() / 3f;
            }
        }
    }
}
