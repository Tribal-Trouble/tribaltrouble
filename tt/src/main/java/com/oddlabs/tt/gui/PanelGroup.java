package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseButtonListener;
import org.jspecify.annotations.NonNull;

public final class PanelGroup extends GUIObject {
	private final @NonNull Group focus_group;
	private final @NonNull PanelBox box;
	private final Panel @NonNull [] panels;

	private int selected;

	public PanelGroup(Panel @NonNull [] panels, int selected) {
		assert selected < panels.length && panels.length > 0: "Invalid index selected.";
		this.panels = panels;

		int tab_height = panels[0].getTab().getHeight();
		int width = 0;
		int height = 0;
            for (Panel panel : panels) {
                if (width < panel.getWidth()) {
                    width = panel.getWidth();
                }
                if (height < panel.getHeight()) {
                    height = panel.getHeight();
                }
            }
		int total_height = height + tab_height;
		setDim(width, total_height);
		int x = Skin.getSkin().getPanelData().getLeftTabOffset();
		int y = height;
		for (int i = 0; i < panels.length; i++) {
			panels[i].setPos((width - panels[i].getWidth())/2, Skin.getSkin().getPanelData().getBottomTabOffset() + (height - panels[i].getHeight())/2);
			panels[i].getTab().setPos(x, y);
			x += panels[i].getTab().getWidth();
			panels[i].getTab().addMouseButtonListener(new TabListener(i));
		}
		box = new PanelBox(width, total_height - panels[0].getTab().getHeight() + Skin.getSkin().getPanelData().getBottomTabOffset());

		focus_group = new Group();
		focus_group.setDim(width, total_height);
		focus_group.setPos(0, 0);
		addChild(focus_group);
		setCanFocus(true);
		selectPanel(selected);
	}

	private void selectPanel(int index) {
		focus_group.clearChildren();
		for (int i = 0; i < panels.length; i++) {
			if (i != index) {
				focus_group.addChild(panels[i].getTab());
				panels[i].getTab().select(false);
			}
		}
		focus_group.addChild(box);
		focus_group.addChild(panels[index].getTab());
		panels[index].getTab().select(true);
		focus_group.addChild(panels[index]);
		selected = index;
		panels[index].setFocus();
	}

	@Override
	public void setFocus() {
		focus_group.setGroupFocus(LocalInput.isShiftDownCurrently() ? -1 : 1);
	}

	@Override
	protected void renderGeometry() {}

	private final class PanelBox extends GUIObject {
		public PanelBox(int width, int height) {
			setDim(width, height);
			setPos(0, 0);
		}

		@Override
		protected void renderGeometry() {
			Skin.getSkin().getPanelData().getBox().render(0, 0, getWidth(), getHeight(), panels[selected].getTab().getRenderState());
		}
	}

	private final class TabListener implements MouseButtonListener {
		private final int index;

		public TabListener(int index) {
			this.index = index;
		}

		@Override
		public void mousePressed(@NonNull MouseButton button, int x, int y) {
			selectPanel(index);
		}

		@Override
		public void mouseReleased(@NonNull MouseButton button, int x, int y) {}
		@Override
		public void mouseHeld(@NonNull MouseButton button, int x, int y) {}
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {}
	}
}
