package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.guievent.CloseListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.lwjgl.input.Keyboard;

public class Form extends Group {
	private final Set<CloseListener> close_listeners = new CopyOnWriteArraySet<>();

	private final String caption;

	private boolean drag = false;

	public Form(String caption) {
		this.caption = caption;
		setFocusCycle(true);
	}

	public Form() {
		this(null);
	}

    @Override
	public final void compileCanvas() {
		int spacing = Skin.getSkin().getFormData().getObjectSpacing();
		Box form;
		if (caption != null) {
			form = Skin.getSkin().getFormData().getForm();
			super.compileCanvas(form.getLeftOffset() + spacing,
								form.getBottomOffset() + spacing,
								form.getRightOffset() + spacing,
								form.getTopOffset() + spacing);

			FormData form_data = Skin.getSkin().getFormData();
			Font font = form_data.getCaptionFont();

			GUIObject label = new Label(caption, font);
			label.setPos(form_data.getCaptionLeft(), getHeight() - form_data.getCaptionY() - font.getHeight()/2);
			addChild(label);
			label.addMouseMotionListener(new DragListener(this));

			GUIObject close_button = new IconButton(Skin.getSkin().getFormData().getFormClose());
			close_button.setPos(getWidth() - close_button.getWidth() - form_data.getCloseRight(),
								getHeight() - close_button.getHeight() - form_data.getCloseTop());
			addChild(close_button);
			close_button.addMouseClickListener((int button, int x, int y, int clicks) -> {
                this.cancel();
            });
		} else {
			form = Skin.getSkin().getFormData().getSlimForm();
			super.compileCanvas(form.getLeftOffset() + spacing,
								form.getBottomOffset() + spacing,
								form.getRightOffset() + spacing,
								form.getTopOffset() + spacing);
		}
	}

	public final void centerPos() {
		setPos((LocalInput.getViewWidth() - getWidth())/2, (LocalInput.getViewHeight() - getHeight())/2);
	}

        @Override
	protected final void renderGeometry() {
		if (isDisabled()) {
			if (caption != null)
				Skin.getSkin().getFormData().getForm().render(0, 0, getWidth(), getHeight(), Skin.DISABLED);
			else
				Skin.getSkin().getFormData().getSlimForm().render(0, 0, getWidth(), getHeight(), Skin.DISABLED);
		} else if (isActive()) {
			if (caption != null)
				Skin.getSkin().getFormData().getForm().render(0, 0, getWidth(), getHeight(), Skin.ACTIVE);
			else
				Skin.getSkin().getFormData().getSlimForm().render(0, 0, getWidth(), getHeight(), Skin.ACTIVE);
		} else {
			if (caption != null)
				Skin.getSkin().getFormData().getForm().render(0, 0, getWidth(), getHeight(), Skin.NORMAL);
			else
				Skin.getSkin().getFormData().getSlimForm().render(0, 0, getWidth(), getHeight(), Skin.NORMAL);
		}

	}

    @Override
	protected final void mousePressed(int button, int x, int y) {
		if (caption != null && y >= getHeight() - Skin.getSkin().getFormData().getForm().getTopOffset())
			drag = true;
	}

    @Override
	protected final void mouseReleased(int button, int x, int y) {
		drag = false;
	}

    @Override
	public final void mouseDragged(int button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
		if (drag)
			setPos(getX() + rel_x, getY() + rel_y);
	}

    @Override
	protected final void mouseScrolled(int amount) {
	}

        @Override
	protected void mouseMoved(int x, int y) {
	}

        @Override
	protected final void mouseExited() {
	}

        @Override
	protected final void mouseEntered() {
	}

        @Override
	protected final void mouseClicked(int button, int x, int y, int clicks) {
	}

        @Override
	protected final void keyPressed(KeyboardEvent event) {
		if (event.getKeyCode() == Keyboard.KEY_H && event.isControlDown())
			super.keyPressed(event);
	}

        @Override
	protected final void keyReleased(KeyboardEvent event) {
	}

        @Override
	protected void keyRepeat(KeyboardEvent event) {
		switch (event.getKeyCode()) {
			case Keyboard.KEY_TAB:
				super.keyRepeat(event);
				break;
			case Keyboard.KEY_ESCAPE:
				cancel();
				break;
			default:
				break;
		}
	}

        @Override
	protected final void mouseHeld(int button, int x, int y) {
	}

	public final void closedAll() {
		closed();
        close_listeners.forEach(listener -> listener.closed());
	}

	protected void closed() {
	}

	public final void addCloseListener(CloseListener listener) {
		close_listeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public final void removeCloseListener(CloseListener listener) {
		close_listeners.remove(listener);
	}

	public final void cancel() {
		doCancel();
		remove();
	}

	protected void doCancel() {
	}

        @Override
	public final void remove() {
		if (getParent() != null)
			closedAll();
		super.remove();
	}

	private final class DragListener implements MouseMotionListener {
		private final Form owner;
		public DragListener(Form owner) {
			this.owner = owner;
		}

                @Override
		public void mouseDragged(int button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
			owner.mouseDragged(button, x, y, rel_x, rel_y, abs_x, abs_y);
		}

                @Override
		public void mouseMoved(int x, int y) {}
                @Override
		public void mouseEntered() {}
                @Override
		public void mouseExited() {}
	}
}
