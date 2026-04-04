package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Row<T, C extends GUIObject & Comparable<C>> extends GUIObject implements Comparable<Row<T, C>> {
    private final @NonNull C @NonNull [] columns;
    private final @Nullable T content_object;
    private int sort_index;
    private final Vector4f color = new Vector4f(com.oddlabs.util.Color.TRANSPARENT);
    private boolean marked = false;

    public Row(@NonNull C @NonNull [] columns, @Nullable T content_object) {
        this.columns = columns;
        this.content_object = content_object;
        setDim(0, columns[0].getHeight());
        setCanFocus(true);
    }

    public @NonNull C getColumn(int index) {
        return columns[index];
    }

    public void setColumnInfos(@NonNull ColumnInfo @NonNull [] column_infos) {
        int x = 0;
        for (int i = 0; i < column_infos.length; i++) {
            C gui_object = getColumn(i);
            gui_object.setPos(x, 0);
            addChild(gui_object);
            x += column_infos[i].width();

            // if left most column, correct for the radio button starting without left_offset
            if (i == 0)
                x -= Skin.getSkin().getMultiColumnComboBoxData().box().getLeftOffset();
            // if right most column, correct for the radio button extending over right_offset
            if (i == column_infos.length - 1)
                x -= Skin.getSkin().getMultiColumnComboBoxData().box().getRightOffset();
        }
        setDim(x, getHeight());
    }

    public void setSortIndex(int sort_index) {
        this.sort_index = sort_index;
    }

    @Override
    public int compareTo(@NonNull Row<T, C> o) {
        return getColumn(sort_index).compareTo(o.getColumn(sort_index));
    }

    public void setColor(@NonNull Vector4fc color) {
        this.color.set(color);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        var c = marked ? Skin.getSkin().getMultiColumnComboBoxData().colorMarked() : color;
        if (c.w() >= .2f) {
            renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), c);
        }
    }

    public @Nullable T getContentObject() {
        return content_object;
    }

    public void mark(boolean marked) {
        this.marked = marked;
    }
}
