package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLayout;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

public class TextBox extends TextField implements Scrollable, Clipped {
    private @NonNull TextLayout textLayout;
    private final @NonNull ScrollBar scroll_bar;

    private int offset_y;

    public TextBox(int width, int height, @NonNull Font font, int max_chars) {
        super(font, max_chars);
        setDim(width, height);
        setCanFocus(true);
        offset_y = 0;
        textLayout = new TextLayout(font, getText(), width);

        scroll_bar = new ScrollBar(height, this);

        updateLayout();
        scroll_bar.setPos(width - scroll_bar.getWidth(), 0);
        addChild(scroll_bar);
    }

    private void updateLayout() {
        Box edit_box = Skin.getSkin().getEditBox();
        int wrapWidth = getWidth() - edit_box.getLeftOffset() - edit_box.getRightOffset() - scroll_bar.getWidth();
        textLayout = new TextLayout(getFont(), getText(), wrapWidth);
        scroll_bar.update();
    }

    @Override
    public @NonNull TextBox setText(@NonNull CharSequence text) {
        super.setText(text);
        updateLayout();
        return this;
    }

    @Override
    public final void append(@NonNull CharSequence str) {
        super.append(str);
        updateLayout();
    }

    protected final @NonNull TextLayout getTextLayout() {
        return textLayout;
    }

    protected final void renderBox(@NonNull GUIRenderer renderer, ModeIconQuads.@NonNull Mode skinMode) {
        Box edit_box = Skin.getSkin().getEditBox();
        edit_box.render(renderer, 0f, 0f, getWidth() - scroll_bar.getWidth(), getHeight(), skinMode);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        Box edit_box = Skin.getSkin().getEditBox();
        renderBox(renderer, ModeIconQuads.Mode.NORMAL);
        TextLineRenderer.render(renderer, textLayout, edit_box.getLeftOffset(),
                getHeight() - edit_box.getBottomOffset() - getFont().getHeight() + offset_y, edit_box.getLeftOffset(),
                getWidth() - edit_box.getRightOffset(), Color.WHITE);
    }

    @Override
    protected final void mouseScrolled(int amount) {
        setOffsetY(offset_y + (amount > 0 ? -3 : 3) * getFont().getHeight());
    }

    @Override
    public final void setOffsetY(int new_offset) {
        offset_y = Math.max(new_offset, 0);

        Box edit_box = Skin.getSkin().getEditBox();
        int max_offset_y = Math.max(0,
                textLayout.getTextHeight() - (getHeight() - edit_box.getBottomOffset() - edit_box.getTopOffset()));
        offset_y = Math.min(offset_y, max_offset_y);
        scroll_bar.update();
    }

    @Override
    public final int getOffsetY() {
        return offset_y;
    }

    @Override
    public final int getStepHeight() {
        return getFont().getHeight();
    }

    @Override
    public final void jumpPage(boolean up) {
        Box edit_box = Skin.getSkin().getEditBox();
        int inner_height = getHeight() - edit_box.getBottomOffset() - edit_box.getTopOffset();
        setOffsetY(offset_y + (up ? -inner_height : inner_height));
    }

    @Override
    public final float getScrollBarRatio() {
        int text_height = textLayout.getTextHeight();
        if (text_height <= 0) return 1f;
        Box edit_box = Skin.getSkin().getEditBox();
        int inner_height = getHeight() - edit_box.getBottomOffset() - edit_box.getTopOffset();
        return Math.min(1f, (float) inner_height / text_height);
    }

    @Override
    public final float getScrollBarOffset() {
        int text_height = textLayout.getTextHeight();
        Box edit_box = Skin.getSkin().getEditBox();
        int inner_height = getHeight() - edit_box.getBottomOffset() - edit_box.getTopOffset();
        int max_offset = text_height - inner_height;
        return max_offset <= 0 ? 0 : (float) offset_y / max_offset;
    }

    @Override
    public final void setScrollBarOffset(float offset) {
        int text_height = textLayout.getTextHeight();
        Box edit_box = Skin.getSkin().getEditBox();
        int inner_height = getHeight() - edit_box.getBottomOffset() - edit_box.getTopOffset();
        int max_offset = text_height - inner_height;
        if (max_offset <= 0) return;
        setOffsetY((int) (offset * max_offset));
    }
}
