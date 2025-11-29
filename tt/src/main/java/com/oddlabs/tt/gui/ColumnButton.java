package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class ColumnButton<T> extends RadioButtonGroupElement {
	private final @NonNull RowCollection<T> rows;
	private final int arrow_offset;
	private final int column_index;

	private boolean sorted_descending;
	private boolean pressed = false;

	ColumnButton(@NonNull RadioButtonGroup group, @NonNull RowCollection<T> rows, @NonNull ColumnInfo info, int column_index, boolean sorted_descending) {
		super(column_index == 0, group);
		this.rows = rows;
		this.column_index = column_index;
		this.sorted_descending = sorted_descending;
		MultiColumnComboBoxData data = Skin.getSkin().getMultiColumnComboBoxData();
		setDim(info.getWidth(), data.getButtonUnpressed().getHeight());

        Font font = data.getFont();
		Label label = new Label(info.getCaption(), font);
        label.setPos(data.getCaptionOffset(), (getHeight() - font.getHeight())/2 + 1);
		addChild(label);

		IconQuad arrow = Skin.getSkin().getMultiColumnComboBoxData().getDescending().quad(ModeIconQuads.Mode.NORMAL);
		arrow_offset = info.getWidth() - arrow.getWidth();
		setCanFocus(true);
	}

	@Override
	protected void mouseReleased (@NonNull MouseButton button, int x, int y) {
		pressed = false;
	}

	@Override
	protected void mousePressed (@NonNull MouseButton button, int x, int y) {
		pressed = true;
	}

	@Override
	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
        sorted_descending = !isMarked() || !sorted_descending;
		super.mouseClicked(button, x, y, clicks);
		rows.markChanged(column_index, sorted_descending);
	}

	public int getColumnIndex() {
		return column_index;
	}

	@Override
	protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isHovered() && pressed
                    ? ModeIconQuads.Mode.ACTIVE
                    : isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                        : ModeIconQuads.Mode.NORMAL;

        var data = Skin.getSkin().getMultiColumnComboBoxData();
        Horizontal buttonHorizontal = skinMode == ModeIconQuads.Mode.ACTIVE && isHovered() && pressed
                ? data.getButtonPressed()
                : data.getButtonUnpressed();

		buttonHorizontal.render(0, 0, getWidth(), skinMode);
		if (isMarked())
			renderMark(skinMode);
	}

	private void renderMark(ModeIconQuads.@NonNull Mode skinMode) {
        var data = Skin.getSkin().getMultiColumnComboBoxData();
        ModeIconQuads arrow = sorted_descending
                ? data.getDescending()
                : data.getAscending();

        IconQuad arrowQuad = arrow.quad(skinMode);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, arrowQuad.getTexture().getHandle());
		GL11.glBegin(GL11.GL_QUADS);
		arrowQuad.render(arrow_offset, (getHeight() - arrowQuad.getHeight())/2);
		GL11.glEnd();
	}
}
