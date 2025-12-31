package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.KeyListener;
import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class ScrollBar extends GUIObject {
	private final Group focus_group = new Group();
	private final @NonNull ArrowButton less_button;
	private final @NonNull ArrowButton more_button;
	private final ScrollButton scroll_button = new ScrollButton();
	private final @NonNull Scrollable owner;

	public ScrollBar(int height, @NonNull Scrollable owner) {
		this.owner = owner;
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
		scroll_button.setupPos(this);
	}

	@Override
	public void setPos(int x, int y) {
		super.setPos(x, y);
        scroll_button.setupPos(this);
	}

	@Override
	public ScrollBar setDim(int width, int height) {
		super.setDim(width, height);
		scroll_button.setupPos(this);
		return this;
	}

	@Override
	public void setFocus() {
		focus_group.setGroupFocus(LocalInput.isShiftDownCurrently() ? -1 : 1);
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		ScrollBarData data = Skin.getSkin().getScrollBarData();
		Vertical scroll_bar = data.getScrollBar();
		scroll_bar.render(renderer, 0, less_button.getHeight(), getHeight() - less_button.getHeight() - more_button.getHeight(), ModeIconQuads.Mode.NORMAL);
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
	protected void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
		int button_y = getButtonY();
		owner.jumpPage(y > button_y);
		scroll_button.setupPos(this);
	}

	private final class LessListener implements MouseButtonListener {
		@Override
		public void mousePressed(@NonNull MouseButton button, int x, int y) {
			owner.setOffsetY(owner.getOffsetY() + owner.getStepHeight());
			scroll_button.setupPos(ScrollBar.this);
		}

		@Override
		public void mouseReleased(@NonNull MouseButton button, int x, int y) {}
		@Override
		public void mouseHeld(@NonNull MouseButton button, int x, int y) {}
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {}
	}

	private final class MoreListener implements MouseButtonListener {
		@Override
		public void mousePressed(@NonNull MouseButton button, int x, int y) {
			owner.setOffsetY(owner.getOffsetY() - owner.getStepHeight());
			scroll_button.setupPos(ScrollBar.this);
		}

		@Override
		public void mouseReleased(@NonNull MouseButton button, int x, int y) {}
		@Override
		public void mouseHeld(@NonNull MouseButton button, int x, int y) {}
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {}
	}

	private final class DragListener implements MouseMotionListener, MouseButtonListener {
		final ScrollBarData data = Skin.getSkin().getScrollBarData();
		float start_offset;

		@Override
		public void mousePressed(@NonNull MouseButton button, int x, int y) {
			start_offset = owner.getScrollBarOffset();
		}

		@Override
		public void mouseDragged(@NonNull MouseButton button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
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
		public void mouseReleased(@NonNull MouseButton button, int x, int y) {}
		@Override
		public void mouseHeld(@NonNull MouseButton button, int x, int y) {}
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {}
	}

	private final class ButtonKeyListener implements KeyListener {
		@Override
		public void keyRepeat(@NonNull KeyboardEvent event) {
            switch (event.getKeyCode()) {
                case UP -> {
                    owner.setOffsetY(owner.getOffsetY() - owner.getStepHeight());
                    scroll_button.setupPos(ScrollBar.this);
                }
                case DOWN -> {
                    owner.setOffsetY(owner.getOffsetY() + owner.getStepHeight());
                    scroll_button.setupPos(ScrollBar.this);
                }
                default -> {
                }
            }
		}

		@Override
		public void keyPressed(@NonNull KeyboardEvent event) {}
		@Override
		public void keyReleased(@NonNull KeyboardEvent event) {}
	}
}
