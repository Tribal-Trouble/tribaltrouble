package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class PulldownButton extends GUIObject {
	private final @NonNull PulldownMenu<Void> menu;
	private final @NonNull Label label;
	private final GUIRoot gui_root;
	private boolean menu_active;

	public PulldownButton(GUIRoot gui_root, @NonNull PulldownMenu<Void> menu, int width) {
		this.menu = menu;
		this.gui_root = gui_root;
		setCanFocus(true);
		menu.addItemChosenListener(new ItemListener());
		label = new Label("", Skin.getSkin().getEditFont(), 0, Origin.AT_START);
		addChild(label);
		setDim(width, Skin.getSkin().getPulldownData().pulldownButton().getHeight());
	}

	public PulldownButton(GUIRoot gui_root, @NonNull PulldownMenu<Void> menu, int item_index, int width) {
		this(gui_root, menu, width);
		menu.chooseItem(item_index);
	}

	@Override
	public @NonNull PulldownButton setDim(int width, int height) {
		super.setDim(width, height);
		PulldownData data = Skin.getSkin().getPulldownData();
		label.setDim(getWidth() - data.textOffsetLeft() - data.arrowOffsetRight() - data.arrow().quad(ModeIconQuads.Mode.NORMAL).getWidth(), label.getHeight());
		label.setPos(data.textOffsetLeft(), (getHeight() - label.getHeight())/2);
		if (menu.getWidth() < width)
			menu.setDim(width, menu.getHeight());
		return this;
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		PulldownData data = Skin.getSkin().getPulldownData();
		Horizontal pulldownButton = data.pulldownButton();

        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive()
                    ? ModeIconQuads.Mode.ACTIVE
                    : ModeIconQuads.Mode.NORMAL;

        pulldownButton.render(renderer, 0, 0, getWidth(), skinMode);

		IconQuad arrowQuad = data.arrow().quad(skinMode);
		renderer.drawIcon(arrowQuad, getWidth() - data.arrowOffsetRight() - arrowQuad.getWidth(), 0);
	}

	@Override
	protected void mousePressed (@NonNull MouseButton button, int x, int y) {
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
	protected void mouseReleased (@NonNull MouseButton button, int x, int y) {
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

	public @NonNull PulldownMenu<Void> getMenu() {
		return menu;
	}

	@Override
	protected void doRemove() {
		super.doRemove();
		if (!menu.isActive())
			menu.remove();
	}

	private final class ItemListener implements ItemChosenListener<Void> {
		@Override
		public void itemChosen(@NonNull PulldownMenu<Void> menu, int item_index) {
			PulldownItem<Void> item = menu.getItem(item_index);
			label.set(item.getLabelString());
			if (menu.isActive())
				deactivateMenu();
		}
	}
}
