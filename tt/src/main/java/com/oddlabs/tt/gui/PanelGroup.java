package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

public final class PanelGroup extends GUIObject {
	private final Group focus_group = new Group();
	private final @NonNull PanelBox box;
	private final @NonNull Panel @NonNull [] panels;

	private int selected;

	public PanelGroup(@NonNull Panel ... panels) {
		this(0, panels);
	}

	public PanelGroup(int selected,@NonNull Panel @NonNull ... panels) {
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
		int x = Skin.getSkin().getPanelData().leftTabOffset();
		int y = height;
		for (int i = 0; i < panels.length; i++) {
			panels[i].setPos((width - panels[i].getWidth())/2, Skin.getSkin().getPanelData().bottomTabOffset() + (height - panels[i].getHeight())/2);
			panels[i].getTab().setPos(x, y);
			x += panels[i].getTab().getWidth();
			panels[i].getTab().addMouseButtonListener(new TabListener(i));
		}
		box = new PanelBox(width, total_height - panels[0].getTab().getHeight() + Skin.getSkin().getPanelData().bottomTabOffset());

		focus_group.setDim(width, total_height);
		focus_group.setPos(0, 0);
		addChild(focus_group);
		setCanFocus(true);
		selectPanel(selected);
	}

	@Override
	public void setFocus() {
		var localInput = Renderer.getLocalInput();
		focus_group.setGroupFocus(localInput.isShiftDownCurrently() ? -1 : 1);
	}

	public void cyclePanel(int dir) {
		int next = (selected + dir + panels.length) % panels.length;
		selectPanel(next);
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

	private final class PanelBox extends GUIObject {
		public PanelBox(int width, int height) {
			setDim(width, height);
			setPos(0, 0);
		}

		@Override
		protected void renderGeometry(@NonNull GUIRenderer renderer) {
			Box panelBox = Skin.getSkin().getPanelData().box();
			panelBox.render(renderer,  0f, 0f, getWidth(), getHeight(), panels[selected].getTab().getRenderState());
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