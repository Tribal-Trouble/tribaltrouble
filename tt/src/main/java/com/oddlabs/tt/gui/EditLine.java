package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Index;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class EditLine extends TextField implements Clipped {
	private final Set<@NonNull EnterListener> enter_listeners = new CopyOnWriteArraySet<>();
	private final @NonNull Origin alignment;
	private final @Nullable String allowed_chars;
	protected final int max_text_width;

	private int offset_x;
	private int index;

	public EditLine(int width, int max_chars) {
		this(width, max_chars, Origin.AT_START);
	}

	public EditLine(int width, int max_chars, @NonNull Origin alignment) {
		this(width, max_chars, null, alignment);
	}

	public EditLine(int width, int max_chars, @Nullable String allowed_chars, @NonNull Origin alignment) {
		super(Skin.getSkin().getEditFont(), max_chars);
		this.allowed_chars = allowed_chars;
		this.alignment = alignment;
		Box edit_box = Skin.getSkin().getEditBox();
		setDim(width, getFont().getHeight() + edit_box.getBottomOffset() + edit_box.getTopOffset());
		setCanFocus(true);
		this.max_text_width = width - edit_box.getLeftOffset() - edit_box.getRightOffset();
		clear();
	}

	@Override
	protected final @NonNull CursorType getCursorType() {
		return isDisabled() ? CursorType.NORMAL : CursorType.TEXT;
	}

    protected @NonNull CharSequence getDisplayText() {
        return getText();
    }

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		Box edit_box = Skin.getSkin().getEditBox();
        var mode = isDisabled() ? ModeIconQuads.Mode.DISABLED : (isActive() ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL);
        edit_box.render(renderer, 0f, 0f, getWidth(), getHeight(), mode);
		int render_index = isActive() ? index : -1;
		renderText(renderer, edit_box, offset_x, render_index);
	}

	protected int getRenderedWidth(@NonNull CharSequence text) {
		int x_border = getFont().getXBorder();
		int half_border = x_border / 2;
        return text.isEmpty() ? half_border : getFont().getWidth(text) - (x_border - half_border);
	}

	protected void renderText(@NonNull GUIRenderer renderer, @NonNull Box box, int offset_x, int render_index) {
        var displayText = getDisplayText();
		TextLineRenderer.render(renderer, getFont(), displayText, box.getLeftOffset() + offset_x, box.getBottomOffset(), box.getLeftOffset() + 1, getWidth() - box.getRightOffset() - 1, Color.WHITE);
		if (render_index != -1) {
			int cursorX = getRenderedWidth(displayText.subSequence(0, render_index));
			Index.renderIndex(renderer, box.getLeftOffset() + offset_x + cursorX, box.getBottomOffset(), getFont(), Color.WHITE);
		}
	}

	@Override
	protected boolean insert(int index, char key) {
		boolean result = super.insert(index, key);
		if (result) {
			this.index++;
		}
		return result;
	}

		@Override
		protected void handleInput(@NonNull InputEvent event) {
			if (event.getPhase() == InputPhase.RELEASED) {
				if (event.consumeAction(GameAction.UI_ACTIVATE)) {
					enterPressedAll();
					return;
				}
			}
	
			if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
				boolean consumed = true;
	
				if (event.consumeAction(GameAction.UI_NAV_LEFT)) {
					if (index > 0) index--;
				} else if (event.consumeAction(GameAction.UI_NAV_RIGHT)) {
					if (index < getText().length()) index++;
				} else if (event.consumeAction(GameAction.UI_NAV_HOME)) {
					index = 0;
				} else if (event.consumeAction(GameAction.UI_NAV_END)) {
					index = getText().length();
				} else if (event.consumeAction(GameAction.UI_BACKSPACE)) {
					if (index > 0) delete(--index);
				            } else if (event.consumeAction(GameAction.UI_DELETE)) {
				                if (index < getText().length()) delete(index);
				            } else if (!event.isControlDown() && !event.isMetaDown() && !event.isAltDown()) {
				                char c = event.getCharacter();
				                if (c != 0 && !Character.isISOControl(c)) {
				                    consumed = insert(index, c);
				                } else {
				                    consumed = false;
				                }
				            } else {
				                consumed = false;
				            }
				
				            if (consumed) {					correctOffsetX();
					event.consume();
					return;
				}
	
				// Consume printable keys in PRESSED phase to prevent bubbling (e.g. 'D' triggering debug bounds)
				// even if the character hasn't been typed yet (will come in REPEAT phase).
				if (event.getPhase() == InputPhase.PRESSED && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
					char c = event.getCharacter();
					if (c != 0 && !Character.isISOControl(c)) {
						event.consume();
						return;
					}
				}
			}
	
			super.handleInput(event);
		}
	@Override
	public final boolean isAllowed(char ch) {
		return super.isAllowed(ch) && getFont().getQuad(ch) != null && (allowed_chars == null || allowed_chars.indexOf(ch) != -1);
	}
	
	private void correctOffsetX() {
        var displayText = getDisplayText();
        int cursorX = getRenderedWidth(displayText.subSequence(0, index));
		int textWidth = getRenderedWidth(displayText);

		if (alignment == Origin.AT_END) {
			offset_x = max_text_width - textWidth;
		} else { // AT_START or AT_MIDDLE
			// First, ensure the cursor is visible
			if (cursorX + offset_x >= max_text_width) {
				offset_x = max_text_width - cursorX - Index.INDEX_WIDTH;
			}
			if (cursorX + offset_x < 0) {
				offset_x = -cursorX;
			}

			// Then, if there's empty space on the right, pull the text back
			if (textWidth + offset_x < max_text_width) {
				offset_x = Math.min(0, max_text_width - textWidth);
			}

			// Finally, ensure we haven't scrolled too far left
			if (offset_x > 0) {
				offset_x = 0;
			}
		}
	}

	public final int getIndex() {
		return index;
	}

	public final void setIndex(int index) {
		this.index = index;
	}

	@Override
	public final void clear() {
		super.clear();
		index = 0;
		correctOffsetX();
	}

	@Override
	protected final void appendNotify(@NonNull CharSequence str) {
		correctOffsetX();
	}

	@Override
	protected void focusNotify(boolean focus) {
		if (focus)
			index = getText().length();
	}

	@Override
	protected final void mouseEntered() {
	}

	@Override
	protected final void mouseExited() {
	}

	@Override
	protected final void mousePressed (@NonNull MouseButton button, int x, int y) {
		if (button == MouseButton.LEFT) {
			Box edit_box = Skin.getSkin().getEditBox();
			float relativeX = x - (getRootX() + edit_box.getLeftOffset() + offset_x);

			int bestIndex = 0;
			float bestDx = Float.MAX_VALUE;

            var displayText = getDisplayText();
			for (int i = 0; i <= displayText.length(); i++) {
				int charX = getRenderedWidth(displayText.subSequence(0, i));
				float dx = Math.abs(relativeX - charX);
				if (dx < bestDx) {
					bestDx = dx;
					bestIndex = i;
				}
			}
			index = bestIndex;
			correctOffsetX();
		}
	}

	public final void enterPressedAll() {
		CharSequence text = getText();
		enterPressed(text);
        for (EnterListener listener : enter_listeners) {
            listener.enterPressed(text);
        }
	}

	protected void enterPressed(@NonNull CharSequence text) {
	}

	public final void addEnterListener(@NonNull EnterListener listener) {
		enter_listeners.add(listener);
	}
}
