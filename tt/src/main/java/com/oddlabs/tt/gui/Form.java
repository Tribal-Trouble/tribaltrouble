package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.guievent.CloseListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Form extends Group {
	private final Set<@NonNull CloseListener> close_listeners = new CopyOnWriteArraySet<>();

	private final @Nullable String caption;

	private boolean drag = false;

	public Form(@Nullable String caption) {
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
			close_button.addMouseClickListener(( _,  _,  _,  _) -> this.cancel());
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
	protected final void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        var data = Skin.getSkin().getFormData();
        var form = caption != null
                ? data.getForm()
                : data.getSlimForm();
        var skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive() ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;
		form.render(0f, 0f, getWidth(), getHeight(), skinMode);
	}

	@Override
	protected final void mousePressed (@NonNull MouseButton button, int x, int y) {
		if (caption != null && y >= getHeight() - Skin.getSkin().getFormData().getForm().getTopOffset())
			drag = true;
	}

	@Override
	protected final void mouseReleased (@NonNull MouseButton button, int x, int y) {
		drag = false;
	}

	@Override
	public final void mouseDragged (@NonNull MouseButton button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
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
	protected final void mouseClicked (@NonNull MouseButton button, int x, int y, int clicks) {
	}

	@Override
	protected final void keyPressed(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() == Keyboard.KEY_H && event.isControlDown())
			super.keyPressed(event);
	}

	@Override
	protected final void keyReleased(@NonNull KeyboardEvent event) {
	}

	@Override
	protected void keyRepeat(@NonNull KeyboardEvent event) {
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
	protected final void mouseHeld (@NonNull MouseButton button, int x, int y) {
	}

	public final void closedAll() {
		closed();
        close_listeners.forEach(CloseListener::closed);
	}

	protected void closed() {
	}

	public final void addCloseListener(@NonNull CloseListener listener) {
		close_listeners.add(listener);
	}

	public final void removeCloseListener(@NonNull CloseListener listener) {
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

	private static final class DragListener implements MouseMotionListener {
		private final Form owner;
		public DragListener(Form owner) {
			this.owner = owner;
		}

		@Override
		public void mouseDragged(@NonNull MouseButton button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
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
