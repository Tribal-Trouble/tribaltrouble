package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class ColumnButton<T> extends RadioButtonGroupElement {
    private final @NonNull RowCollection<T> rows;
    private final int arrow_offset;
    private final int column_index;

    private boolean sorted_descending;
    private boolean pressed = false;

    ColumnButton(@NonNull RadioButtonGroup group, @NonNull RowCollection<T> rows, @NonNull ColumnInfo info,
            int column_index, boolean sorted_descending) {
        super(column_index == 0, group);
        this.rows = rows;
        this.column_index = column_index;
        this.sorted_descending = sorted_descending;
        MultiColumnComboBoxData data = Skin.getSkin().getMultiColumnComboBoxData();
        setDim(info.width(), data.buttonUnpressed().getHeight());

        Font font = data.font();
        Label label = new Label(info.caption(), font);
        label.setPos(data.captionOffset(), (getHeight() - font.getHeight()) / 2 + 1);
        addChild(label);

        IconQuad arrow = Skin.getSkin().getMultiColumnComboBoxData().descending().quad(ModeIconQuads.Mode.NORMAL);
        arrow_offset = info.width() - arrow.getWidth();
        setCanFocus(true);
    }

    @Override
    protected void mouseReleased(@NonNull MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected void mousePressed(@NonNull MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        sorted_descending = !isMarked() || !sorted_descending;
        super.mouseClicked(button, x, y, clicks);
        rows.markChanged(column_index, sorted_descending);
    }

    public int getColumnIndex() {
        return column_index;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled() ? ModeIconQuads.Mode.DISABLED : isHovered()
                && pressed ? ModeIconQuads.Mode.ACTIVE : isActive() ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;

        var data = Skin.getSkin().getMultiColumnComboBoxData();
        Horizontal buttonHorizontal = skinMode == ModeIconQuads.Mode.ACTIVE && isHovered()
                && pressed ? data.buttonPressed() : data.buttonUnpressed();

        buttonHorizontal.render(renderer, 0, 0, getWidth(), skinMode);
        if (isMarked())
            renderMark(renderer, skinMode);
    }

    private void renderMark(@NonNull GUIRenderer renderer, ModeIconQuads.@NonNull Mode skinMode) {
        var data = Skin.getSkin().getMultiColumnComboBoxData();
        ModeIconQuads arrow = sorted_descending ? data.descending() : data.ascending();

        IconQuad arrowQuad = arrow.quad(skinMode);
        renderer.drawIcon(arrowQuad, arrow_offset, (getHeight() - arrowQuad.getHeight()) / 2f);
    }
}
