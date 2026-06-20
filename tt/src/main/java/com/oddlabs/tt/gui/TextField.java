package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import org.jspecify.annotations.NonNull;

/**
 * A mutable text field that allows text to be appended
 */
public abstract class TextField extends GUIObject implements CharSequence {
    private final @NonNull StringBuilder text;
    private final @NonNull Font font;
    /**
     * This is the maximum number of codepoints that can be stored in the text field.
     * Note that this may differ from the number of bytes, characters, and graphemes.
     */
    private final int max_codepoints;

    public TextField(@NonNull Font font, int max_codepoints) {
        this("", font, max_codepoints);
    }

    public TextField(@NonNull CharSequence text, @NonNull Font font, int max_codepoints) {
        this.font = font;
        this.text = new StringBuilder(max_codepoints < Integer.MAX_VALUE ? max_codepoints : text.length());
        this.max_codepoints = max_codepoints;
        this.text.append(text);
    }

    @Override
    public final @NonNull String toString() {
        return text.toString();
    }

    @Override
    public final char charAt(int i) {
        return text.charAt(i);
    }

    @Override
    public final int length() {
        return text.length();
    }

    @Override
    public final @NonNull CharSequence subSequence(int start, int end) {
        return text.subSequence(start, end);
    }

    public final int codePointAt(int index) {
        return text.codePointAt(index);
    }

    public final int getTextWidth() {
        return font.getWidth(text);
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

    public @NonNull TextField setText(@NonNull CharSequence text) {
        this.text.setLength(0);
        this.text.append(text);
        return this;
    }

    public final void set(@NonNull CharSequence str) {
        clear();
        append(str);
    }

    public void clear() {
        text.delete(0, text.length());
    }

    public boolean append(@NonNull CharSequence text) {
        if (this.text.codePointCount(0, this.text.length()) + text.codePoints().count() > max_codepoints) {
            return false;
        }
        this.text.append(text);
        appendNotify(text);

        return true;
    }

    public final void append(long i) {
        append(Long.toString(i));
    }

    protected boolean insert(int index, int codepoint) {
        if (isAllowed(codepoint)) {
            text.insert(index, Character.toChars(codepoint));
            return true;
        } else {
            return false;
        }
    }

    protected boolean isAllowed(int codepoint) {
        return max_codepoints == Integer.MAX_VALUE || text.codePointCount(0, length()) < max_codepoints;
    }

    protected void delete(int index) {
        text.delete(index, index + Character.charCount(text.codePointAt(index)));
    }

    protected void appendNotify(@NonNull CharSequence str) {
    }

    @Override
    protected void handleInput(@NonNull InputEvent event) {
        event.consumeAction(GameAction.UI_ACTIVATE);
        super.handleInput(event);
    }
}
