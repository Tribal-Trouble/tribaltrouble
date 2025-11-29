package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class BorderGroup extends Group {
	private final @Nullable Label label;

	public BorderGroup() {
		label = null;
	}

	public BorderGroup(@NonNull String caption) {
		GroupData data = Skin.getSkin().getGroupData();
		label = new Label(caption, data.getCaptionFont());
	}

	@Override
	public void compileCanvas() {
		GroupData data = Skin.getSkin().getGroupData();
		Box group = data.getGroup();
		if (label != null) {
			super.compileCanvas(group.getLeftOffset(),
								group.getBottomOffset(),
								group.getRightOffset(),
								group.getTopOffset() + data.getCaptionOffset());
			label.setPos(data.getCaptionLeft(), getHeight() - data.getCaptionY());
			addChild(label);
		} else {
			super.compileCanvas(group.getLeftOffset(), group.getBottomOffset(), group.getRightOffset(), group.getTopOffset());
		}
		setCanFocus(true);
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		Skin.getSkin().getGroupData().getGroup().render(renderer, 0f, 0f, getWidth(), getHeight(), ModeIconQuads.Mode.NORMAL);
	}
}
