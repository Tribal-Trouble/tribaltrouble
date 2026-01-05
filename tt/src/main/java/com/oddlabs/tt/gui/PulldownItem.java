package com.oddlabs.tt.gui;


import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PulldownItem<T> extends ButtonObject {
	private final @NonNull Label label;
	private final @Nullable T attachment;

	public PulldownItem(@NonNull String label_str) {
		this(label_str, null);
	}
	
	public PulldownItem(@NonNull String label_str, @Nullable T attachment) {
		super(Skin.getSkin().getPulldownData().font());
		this.attachment = attachment;
		PulldownData data = Skin.getSkin().getPulldownData();
		label = new Label(label_str, data.font(), 0, Origin.AT_START);
		addChild(label);
		setDim(0, label.getHeight());
	}

	public @Nullable T getAttachment() {
		return attachment;
	}
	
	public int getTextHeight() {
		return label.getHeight();
	}

	public int getTextWidth() {
//		return label.getWidth();
		return label.getTextWidth();
	}

	@Override
	public @NonNull PulldownItem<T> setDim(int width, int height) {
		super.setDim(width, height);
		Box item = Skin.getSkin().getPulldownData().pulldownItem();
		label.setDim(getWidth() - item.getLeftOffset() - item.getRightOffset(), label.getHeight());
		label.setPos(item.getLeftOffset(), (getHeight() - label.getHeight())/2);
		return this;
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		Box item = Skin.getSkin().getPulldownData().pulldownItem();
		ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.NORMAL
                : isActive() || isHovered()
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;
        item.render(renderer, 0f, 0f, getWidth(), getHeight(), skinMode);
	}

	public void setLabelString(@NonNull CharSequence label_str) {
		label.set(label_str);
	}

	public @NonNull CharSequence getLabelString() {
		return label;
	}

	@Override
	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
            // Prevent super.mouseClicked from being called to avoid infinite loop.

	}
}
