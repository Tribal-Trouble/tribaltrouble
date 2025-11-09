package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;

public final class PulldownButton extends GUIObject {
	private final @NonNull PulldownMenu menu;
	private final @NonNull Label label;
	private final GUIRoot gui_root;
	private boolean menu_active;

	public PulldownButton(GUIRoot gui_root, @NonNull PulldownMenu menu, int width) {
		this.menu = menu;
		this.gui_root = gui_root;
		setCanFocus(true);
		menu.addItemChosenListener(new ItemListener());
		label = new Label("", Skin.getSkin().getEditFont(), 0, Label.ALIGN_LEFT);
		addChild(label);
		setDim(width, Skin.getSkin().getPulldownData().getPulldownButton().getHeight());
	}

	public PulldownButton(GUIRoot gui_root, @NonNull PulldownMenu menu, int item_index, int width) {
		this(gui_root, menu, width);
		menu.chooseItem(item_index);
	}

	@Override
	public void setDim(int width, int height) {
		super.setDim(width, height);
		PulldownData data = Skin.getSkin().getPulldownData();
		label.setDim(getWidth() - data.getTextOffsetLeft() - data.getArrowOffsetRight() - data.getArrow()[0].getWidth(), label.getHeight());
		label.setPos(data.getTextOffsetLeft(), (getHeight() - label.getHeight())/2);
		if (menu.getWidth() < width)
			menu.setDim(width, menu.getHeight());
	}

	@Override
	protected void renderGeometry() {
		PulldownData data = Skin.getSkin().getPulldownData();
		Quad[] arrow = data.getArrow();

		if (isDisabled()) {
			data.getPulldownButton().render(0, 0, getWidth(), Skin.DISABLED);
			arrow[Skin.DISABLED].render(getWidth() - data.getArrowOffsetRight() - arrow[Skin.DISABLED].getWidth(), 0);
		} else if (isActive()) {
			data.getPulldownButton().render(0, 0, getWidth(), Skin.ACTIVE);
			arrow[Skin.ACTIVE].render(getWidth() - data.getArrowOffsetRight() - arrow[Skin.ACTIVE].getWidth(), 0);
		} else {
			data.getPulldownButton().render(0, 0, getWidth(), Skin.NORMAL);
			arrow[Skin.NORMAL].render(getWidth() - data.getArrowOffsetRight() - arrow[Skin.NORMAL].getWidth(), 0);
		}
	}

	@Override
	protected void mousePressed(int button, int x, int y) {
		if (menu_active) {
			deactivateMenu();
		} else {
			activateMenu();
		}
	}

	@Override
	protected void mouseEntered() {
		menu_active = menu.isActive();
	}

	@Override
	protected void mouseReleased(int button, int x, int y) {
		if (!menu.isActive())
			menu.getItem(menu.getChosenItemIndex()).setFocus();
		menu.clickItem(button, x, y, 1);
	}

	private void activateMenu() {
		menu_active = true;
		menu.setPos((int)(getRootX() + getWidth() - menu.getWidth()), (int)(getRootY() - menu.getHeight()));
		gui_root.getDelegate().addChild(menu);
	}

	private void deactivateMenu() {
		menu_active = false;
		setFocus();
		menu.remove();
	}

	public @NonNull PulldownMenu getMenu() {
		return menu;
	}

	@Override
	protected void doRemove() {
		super.doRemove();
		if (!menu.isActive())
			menu.remove();
	}

	private final class ItemListener implements ItemChosenListener {
		@Override
		public void itemChosen(@NonNull PulldownMenu menu, int item_index) {
			PulldownItem item = menu.getItem(item_index);
			label.set(item.getLabelString());
			if (menu.isActive())
				deactivateMenu();
		}
	}
}
