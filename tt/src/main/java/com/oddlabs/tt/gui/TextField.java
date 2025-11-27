package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public abstract class TextField extends GUIObject implements CharSequence {
	private final @NonNull StringBuffer text;
	private final Font font;
	private final int max_chars;

	public TextField(Font font, int max_chars) {
		this("", font, max_chars);
	}

	public TextField(@NonNull CharSequence text, Font font, int max_chars) {
		this.font = font;
		this.text = new StringBuffer(text.toString());
		this.max_chars = max_chars;
	}

	public final Font getFont() {
		return font;
	}

	public final @NonNull String getContents() {
		return text.toString();
	}

	protected final @NonNull StringBuffer getText() {
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

	public final void set(@NonNull CharSequence str) {
		clear();
		append(str.toString());
	}

	public void clear() {
		text.delete(0, text.length());
	}

	public void append(String str) {
		text.append(str);
		appendNotify(str);
	}

	public void append(StringBuffer str) {
		text.append(str);
		appendNotify(str);
	}

	public void append(CharSequence str) {
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

	protected void appendNotify(CharSequence str) {
	}

	@Override
	protected final void keyPressed(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() != Keyboard.KEY_SPACE && event.getKeyCode() != Keyboard.KEY_RETURN)
			super.keyPressed(event);
	}

	@Override
	protected void keyReleased(@NonNull KeyboardEvent event) {
		if (event.getKeyCode() != Keyboard.KEY_SPACE && event.getKeyCode() != Keyboard.KEY_RETURN)
			super.keyReleased(event);
	}
}
