package com.oddlabs.tt.gui;


public final class PulldownItem extends ButtonObject {
	private final Label label;
	private final Object attachment;

	public PulldownItem(String label_str) {
		this(label_str, null);
	}
	
	public PulldownItem(String label_str, Object attachment) {
		this.attachment = attachment;
		PulldownData data = Skin.getSkin().getPulldownData();
		label = new Label(label_str, data.getFont(), 0, Label.ALIGN_LEFT);
		addChild(label);
		setDim(0, label.getHeight());
	}

	public Object getAttachment() {
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
	public void setDim(int width, int height) {
		super.setDim(width, height);
		Box item = Skin.getSkin().getPulldownData().getPulldownItem();
		label.setDim(getWidth() - item.getLeftOffset() - item.getRightOffset(), label.getHeight());
		label.setPos(item.getLeftOffset(), (getHeight() - label.getHeight())/2);
	}

        @Override
	protected void renderGeometry() {
		Box item = Skin.getSkin().getPulldownData().getPulldownItem();
		if (isDisabled())
			item.render(0, 0, getWidth(), getHeight(), Skin.NORMAL);
		else if (isActive() || isHovered())
			item.render(0, 0, getWidth(), getHeight(), Skin.ACTIVE);
		else			
			item.render(0, 0, getWidth(), getHeight(), Skin.NORMAL);
	}

	public void setLabelString(CharSequence label_str) {
		label.set(label_str);
	}

	public CharSequence getLabelString() {
		return label;
	}

        @Override
	protected void mouseClicked(int button, int x, int y, int clicks) {
            // Prevent super.mouseClicked from being called to avoid infinite loop.

	}
}
