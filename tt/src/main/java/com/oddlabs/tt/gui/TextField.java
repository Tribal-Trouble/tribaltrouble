package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.input.Key;
import org.jspecify.annotations.NonNull;

/**
 * A mutable text field that allows text to be appended
 */
public abstract class TextField extends GUIObject implements CharSequence {
	private final @NonNull StringBuilder text;
	private final @NonNull Font font;
	private final int max_chars;

	public TextField(@NonNull Font font, int max_chars) {
		this("", font, max_chars);
	}

	public TextField(@NonNull CharSequence text, @NonNull Font font, int max_chars) {
		this.font = font;
		this.text =  new StringBuilder(max_chars < Integer.MAX_VALUE ? max_chars : text.length());
		this.max_chars = max_chars;
        this.text.append(text);
	}

	public final @NonNull Font getFont() {
		return font;
	}

	public final @NonNull String getContents() {
		return text.toString();
	}

	protected final @NonNull StringBuilder getText() {
		return text;
	}

	@Override
	public final char charAt(int i) {
		return text.charAt(i);
	}

	@Override
	public final int length() {
		return text.length();
	}

	public final int getTextWidth() {
		return font.getWidth(text);
	}

	@Override
	public final @NonNull CharSequence subSequence(int start, int end) {
		return text.subSequence(start, end);
	}

	@Override
	public final @NonNull String toString() {
		return text.toString();
	}

	public @NonNull TextField setText(@NonNull CharSequence text) {
		this.text.setLength(0);
		this.text.append(text);
		return this;
	}

	public final void set(@NonNull CharSequence str) {
		clear();
		append(str.toString());
	}

	public void clear() {
		text.delete(0, text.length());
	}

	public void append(@NonNull CharSequence str) {
		text.append(str);
		appendNotify(str);
	}

	public final void append(long i) {
        append(Long.toString(i));
	}

	protected boolean insert(int index, char key) {
		if (isAllowed(key)) {
			text.insert(index, key);
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean isAllowed(char key) {
		return max_chars == -1 || text.length() < max_chars;
	}
	
	protected void delete(int index) {
		text.deleteCharAt(index);
	}

	protected void appendNotify(@NonNull CharSequence str) {
	}

	@Override
	protected final void keyPressed(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() != Key.SPACE && event.getKeyCode() != Key.RETURN)
			super.keyPressed(event);
	}

	@Override
	protected void keyReleased(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() != Key.SPACE && event.getKeyCode() != Key.RETURN)
			super.keyReleased(event);
	}
}
