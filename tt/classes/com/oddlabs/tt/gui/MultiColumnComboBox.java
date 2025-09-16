package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.RowListener;

import java.util.ArrayList;
import java.util.List;

public final strictfp class MultiColumnComboBox extends GUIObject implements Scrollable {
    private final ColumnInfo[] column_infos;
    private final RadioButtonGroup group = new RadioButtonGroup();
    private final Group focus_group = new Group();
    private final RowCollection rows = new RowCollection(this, 0, true);
    private final List<RowListener> row_listeners = new ArrayList<>();
    private final ScrollBar scroll_bar;
    private final boolean use_buttons;
    private final GUIRoot gui_root;
    private int offset_y = 0;
    private PulldownMenu pulldown_menu = null;
    private Object right_clicked_row_data;
    // Controls optional inversion of the thumb mapping; default false for natural mapping
    private boolean inverted_scrollbar = false;

    public MultiColumnComboBox(GUIRoot gui_root, ColumnInfo[] column_infos, int height) {
        this(gui_root, column_infos, height, true);
    }

    public MultiColumnComboBox(
            GUIRoot gui_root, ColumnInfo[] column_infos, int height, boolean use_buttons) {
        this.column_infos = column_infos;
        this.use_buttons = use_buttons;
        this.gui_root = gui_root;
        Box box = Skin.getSkin().getMultiColumnComboBoxData().getBox();
        int width = 0;
        for (int i = 0; i < column_infos.length; i++) {
            ColumnButton column_button = new ColumnButton(group, rows, column_infos[i], i, true);
            if (use_buttons) {
                column_button.setPos(width, height - column_button.getHeight());
                focus_group.addChild(column_button);
            }
            width += column_button.getWidth();
        }
        scroll_bar = new ScrollBar(height, this);
        scroll_bar.setPos(width, 0);
        focus_group.addChild(scroll_bar);
        setDim(width + scroll_bar.getWidth(), height);
        focus_group.setDim(getWidth(), getHeight());
        focus_group.setPos(0, 0);
        addChild(focus_group);
        if (use_buttons)
            rows.setDim(
                    width - box.getLeftOffset(),
                    height
                            - box.getBottomOffset()
                            - box.getTopOffset()
                            - group.getMarked().getHeight());
        else
            rows.setDim(
                    width - box.getLeftOffset(),
                    height - box.getBottomOffset() - box.getTopOffset());
        rows.setPos(box.getLeftOffset(), box.getBottomOffset());
        addChild(rows);
        setCanFocus(true);
    // Keep original (bottom-up) row layout to preserve spacing and rendering math,
    // but start scrolled so the visual shows the bottom rows and the thumb at the bottom.
    scroll_bar.update();
    scrollToVisualBottom();
    }

    public final int getSize() {
        return rows.getSize();
    }

    public final void clickedRow() {
        for (int i = 0; i < row_listeners.size(); i++) {
            RowListener listener = row_listeners.get(i);
            if (listener != null) listener.rowChosen(rows.getSelected());
        }
    }

    public final void addRowListener(RowListener listener) {
        row_listeners.add(listener);
    }

    public final void doubleClickedRow() {
        for (int i = 0; i < row_listeners.size(); i++) {
            RowListener listener = row_listeners.get(i);
            if (listener != null) listener.rowDoubleClicked(rows.getSelected());
        }
    }

    public final void setPulldownMenu(PulldownMenu pulldown_menu) {
        this.pulldown_menu = pulldown_menu;
    }

    public final void rightClickedRow(int x, int y) {
        if (pulldown_menu != null) {
            int pulldown_x =
                    StrictMath.max(
                            0,
                            StrictMath.min(
                                    LocalInput.getViewWidth() - pulldown_menu.getWidth(), x));
            int pulldown_y =
                    StrictMath.max(
                            0,
                            StrictMath.min(
                                    LocalInput.getViewHeight() - pulldown_menu.getHeight(),
                                    y - pulldown_menu.getHeight()));
            pulldown_menu.setPos(pulldown_x, pulldown_y);
            gui_root.getDelegate().addChild(pulldown_menu);
            pulldown_menu.setFocus();
            right_clicked_row_data = getSelected();
        }
    }

    @Override
    protected final void renderGeometry() {
        MultiColumnComboBoxData data = Skin.getSkin().getMultiColumnComboBoxData();
        Box box = data.getBox();
        if (use_buttons)
            box.render(
                    0,
                    0,
                    getWidth() - scroll_bar.getWidth(),
                    getHeight() - group.getMarked().getHeight(),
                    Skin.NORMAL);
        else box.render(0, 0, getWidth() - scroll_bar.getWidth(), getHeight(), Skin.NORMAL);
    }

    @Override
    public final void setFocus() {
        focus_group.setGroupFocus(LocalInput.isShiftDownCurrently() ? -1 : 1);
    }

    public final void clear() {
        rows.clear();
    }

    public final void addRow(Row row) {
        // Preserve visual top if we were at the top before content changed
        boolean wasAtVisualTop = isAtVisualTop();
        row.setColumnInfos(column_infos);
        rows.addRow(row);
        if (wasAtVisualTop) scrollToVisualTop();
        else scroll_bar.update();
    }

    /**
     * Positions the scrollbar/thumb so that, visually, the first rows appear at the top
     * of the viewport. For top-down layout this is offset 0; for bottom-up it is max offset.
     */
    public final void scrollToVisualTop() {
        int max_offset_y = rows.getContentHeight() - rows.getHeight();
        if (max_offset_y < 0) max_offset_y = 0;
        int target = rows.isTopDownLayout() ? 0 : max_offset_y;
        setOffsetY(target);
    }

    /**
     * Positions the scrollbar/thumb at the bottom visually.
     * For bottom-up layout this is offset 0; for top-down it is the maximum offset.
     */
    public final void scrollToVisualBottom() {
        int max_offset_y = rows.getContentHeight() - rows.getHeight();
        if (max_offset_y < 0) max_offset_y = 0;
        int target = rows.isTopDownLayout() ? max_offset_y : 0;
        setOffsetY(target);
    }

    public final Object getSelected() {
        return rows.getSelected();
    }

    public final Object getRightClickedRowData() {
        return right_clicked_row_data;
    }

    public final void selectRow(Row row) {
        rows.selectRow(row);
    }

    @Override
    protected final void mouseScrolled(int amount) {
        int delta = 3 * Skin.getSkin().getMultiColumnComboBoxData().getFont().getHeight();
    boolean topDown = rows.isTopDownLayout();
    // Invert response so wheel direction matches visual expectation without changing input semantics
    if (amount > 0) setOffsetY(offset_y + (topDown ? +delta : -delta));
    else setOffsetY(offset_y + (topDown ? -delta : +delta));
    }

    public void setTopDownLayout(boolean topDown) {
        rows.setTopDownLayout(topDown);
        // Keep natural mapping so thumb-at-top means visual top
        this.inverted_scrollbar = false;
        scroll_bar.update();
        // Default visible position: visual top
        scrollToVisualTop();
    }

    @Override
    public final void setOffsetY(int new_offset) {
        offset_y = new_offset;

        if (offset_y < 0) offset_y = 0;
        int max_offset_y = rows.getContentHeight() - rows.getHeight();
        if (max_offset_y < 0) max_offset_y = 0;
        if (offset_y > max_offset_y) offset_y = max_offset_y;
        rows.replaceRows();
        scroll_bar.update();
    }

    @Override
    public final int getOffsetY() {
        return offset_y;
    }

    @Override
    public final int getStepHeight() {
        return Skin.getSkin().getMultiColumnComboBoxData().getFont().getHeight();
    }

    @Override
    public final void jumpPage(boolean up) {
        if (up) setOffsetY(offset_y - rows.getHeight());
        else setOffsetY(offset_y + rows.getHeight());
    }

    @Override
    public final float getScrollBarRatio() {
        int total = rows.getContentHeight();
        int visible = rows.getHeight();
        if (total <= 0 || total <= visible) return 1.0f;
        return visible / (float) total;
    }

    @Override
    public final float getScrollBarOffset() {
        int total = rows.getContentHeight();
        int visible = rows.getHeight();
        int maxOffset = total - visible;
        if (maxOffset <= 0) return 0.0f;
        // Map 0 -> visual top, 1 -> visual bottom regardless of layout
        float top = rows.isTopDownLayout() ? 0f : (float) maxOffset;
        float bottom = rows.isTopDownLayout() ? (float) maxOffset : 0f;
        float denom = bottom - top; // may be negative in bottom-up
        if (denom == 0f) return 0.0f;
        float base = (offset_y - top) / denom;
        // Clamp to [0,1]
        if (base < 0f) base = 0f;
        if (base > 1f) base = 1f;
        return inverted_scrollbar ? (1.0f - base) : base;
    }

    @Override
    public final void setScrollBarOffset(float offset) {
        int total = rows.getContentHeight();
        int visible = rows.getHeight();
        int maxOffset = total - visible;
        if (maxOffset <= 0) {
            setOffsetY(0);
            return;
        }
        // Clamp offset to [0,1]
        float o = offset;
        if (o < 0f) o = 0f;
        if (o > 1f) o = 1f;
        float base = inverted_scrollbar ? (1.0f - o) : o;
        float top = rows.isTopDownLayout() ? 0f : (float) maxOffset;
        float bottom = rows.isTopDownLayout() ? (float) maxOffset : 0f;
        float denom = bottom - top; // may be negative in bottom-up
        int target = (int) (top + base * denom);
        setOffsetY(target);
    }

    private int getMaxOffsetY() {
        int max = rows.getContentHeight() - rows.getHeight();
        return Math.max(0, max);
    }

    private boolean isAtVisualTop() {
        if (rows.isTopDownLayout()) {
            return offset_y == 0;
        } else {
            return offset_y == getMaxOffsetY();
        }
    }

    public void setInvertedScrollbar(boolean inverted) {
        this.inverted_scrollbar = inverted;
        scroll_bar.update();
    }
}
