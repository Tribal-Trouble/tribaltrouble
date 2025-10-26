package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.KeyListener;
import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public final class ScrollBar extends GUIObject {
	private final @NonNull Group focus_group;
	private final @NonNull ArrowButton less_button;
	private final @NonNull ArrowButton more_button;
	private final @NonNull ScrollButton scroll_button;
	private final Scrollable owner;

	public ScrollBar(int height, Scrollable owner) {
		this.owner = owner;
		focus_group = new Group();
		less_button = new ArrowButton(Skin.getSkin().getScrollBarData().getScrollDownButtonPressed(),
									  Skin.getSkin().getScrollBarData().getScrollDownButtonUnpressed(),
									  Skin.getSkin().getScrollBarData().getScrollDownArrow());
		more_button = new ArrowButton(Skin.getSkin().getScrollBarData().getScrollUpButtonPressed(),
									  Skin.getSkin().getScrollBarData().getScrollUpButtonUnpressed(),
									  Skin.getSkin().getScrollBarData().getScrollUpArrow());
		less_button.setPos(0, 0);
		more_button.setPos(0, height - more_button.getHeight());
		focus_group.addChild(more_button);

		less_button.addMouseButtonListener(new LessListener());
		more_button.addMouseButtonListener(new MoreListener());

		setDim(less_button.getWidth(), height);
		setCanFocus(true);

		scroll_button = new ScrollButton();
		focus_group.addChild(scroll_button);
		focus_group.addChild(less_button); // add here to secure proper tabbing order
		DragListener drag_listener = new DragListener();
		scroll_button.addMouseMotionListener(drag_listener);
		scroll_button.addMouseButtonListener(drag_listener);
		scroll_button.addKeyListener(new ButtonKeyListener());

		focus_group.setDim(getWidth(), getHeight());
		focus_group.setPos(0, 0);
		addChild(focus_group);
	}

	public void update() {
		if (scroll_button != null)
			scroll_button.setupPos(this);
	}

        @Override
	public void setPos(int x, int y) {
		super.setPos(x, y);
		if (scroll_button != null)
			scroll_button.setupPos(this);
	}

        @Override
	public void setDim(int width, int height) {
		super.setDim(width, height);
		if (scroll_button != null)
			scroll_button.setupPos(this);
	}

        @Override
	public void setFocus() {
		focus_group.setGroupFocus(LocalInput.isShiftDownCurrently() ? -1 : 1);
	}

        @Override
	protected void renderGeometry() {
		ScrollBarData data = Skin.getSkin().getScrollBarData();
		Vertical scroll_bar = data.getScrollBar();
		scroll_bar.render(0, less_button.getHeight(), getHeight() - less_button.getHeight() - more_button.getHeight(), Skin.NORMAL);
	}

	public int getButtonX() {
		return Skin.getSkin().getScrollBarData().getLeftOffset();
	}

	public int getButtonY() {
		ScrollBarData data = Skin.getSkin().getScrollBarData();
		int max_height = getHeight() - less_button.getHeight() - more_button.getHeight() - data.getBottomOffset() - data.getTopOffset();
		int size = getButtonHeight();
		int offset = max_height - size - (int)((max_height - size) * owner.getScrollBarOffset());
		return less_button.getHeight() + data.getBottomOffset() + offset;
	}

	public int getButtonHeight() {
		ScrollBarData data = Skin.getSkin().getScrollBarData();
		int max_height = getHeight() - less_button.getHeight() - more_button.getHeight() - data.getBottomOffset() - data.getTopOffset();
		float ratio = owner.getScrollBarRatio();
		int size = (int)(ratio*max_height);
		if (size < data.getScrollButton().getMinHeight())
			size = data.getScrollButton().getMinHeight();
		return size;
	}

        @Override
	protected void mouseClicked(int button, int x, int y, int clicks) {
		int button_y = getButtonY();
		owner.jumpPage(y > button_y);
		scroll_button.setupPos(this);
	}

	private final class LessListener implements MouseButtonListener {
                @Override
		public void mousePressed(int button, int x, int y) {
			owner.setOffsetY(owner.getOffsetY() + owner.getStepHeight());
			scroll_button.setupPos(ScrollBar.this);
		}

                @Override
		public void mouseReleased(int button, int x, int y) {}
                @Override
		public void mouseHeld(int button, int x, int y) {}
                @Override
		public void mouseClicked(int button, int x, int y, int clicks) {}
	}

	private final class MoreListener implements MouseButtonListener {
                @Override
		public void mousePressed(int button, int x, int y) {
			owner.setOffsetY(owner.getOffsetY() - owner.getStepHeight());
			scroll_button.setupPos(ScrollBar.this);
		}

                @Override
		public void mouseReleased(int button, int x, int y) {}
                @Override
		public void mouseHeld(int button, int x, int y) {}
                @Override
		public void mouseClicked(int button, int x, int y, int clicks) {}
	}

	private final class DragListener implements MouseMotionListener, MouseButtonListener {
		final ScrollBarData data = Skin.getSkin().getScrollBarData();
		float start_offset;

                @Override
		public void mousePressed(int button, int x, int y) {
			start_offset = owner.getScrollBarOffset();
		}

                @Override
		public void mouseDragged(int button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
			int max_height = getHeight() - less_button.getHeight() - more_button.getHeight() - data.getBottomOffset() - data.getTopOffset();
			float ratio = owner.getScrollBarRatio();
			int size = (int)(ratio*max_height);
			int scroll_button_space = max_height - size;
			owner.setScrollBarOffset(start_offset - abs_y/(float)scroll_button_space);
			scroll_button.setupPos(ScrollBar.this);
		}

                @Override
		public void mouseMoved(int x, int y) {}
                @Override
		public void mouseEntered() {}
                @Override
		public void mouseExited() {}
                @Override
		public void mouseReleased(int button, int x, int y) {}
                @Override
		public void mouseHeld(int button, int x, int y) {}
                @Override
		public void mouseClicked(int button, int x, int y, int clicks) {}
	}

	private final class ButtonKeyListener implements KeyListener {
                @Override
		public void keyRepeat(@NonNull KeyboardEvent event) {
			switch (event.getKeyCode()) {
				case Keyboard.KEY_UP:
					owner.setOffsetY(owner.getOffsetY() - owner.getStepHeight());
					scroll_button.setupPos(ScrollBar.this);
					break;
				case Keyboard.KEY_DOWN:
					owner.setOffsetY(owner.getOffsetY() + owner.getStepHeight());
					scroll_button.setupPos(ScrollBar.this);
					break;
				default:
					break;
			}
		}

                @Override
		public void keyPressed(KeyboardEvent event) {}
                @Override
		public void keyReleased(KeyboardEvent event) {}
	}
}
