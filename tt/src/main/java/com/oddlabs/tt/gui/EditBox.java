package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Index;
import com.oddlabs.tt.font.TextLayout;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.util.Color;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

/**
 * A box of several lines of text to edit, wrapped to its width and scrolled to keep the insertion point in view.
 *
 * <p>Enter starts a new line. The arrows, Home and End move the insertion point, with Ctrl by words or to either end
 * of the text, and with Shift select as they go, as does dragging the mouse. Ctrl+A selects everything, and Ctrl+C,
 * Ctrl+X and Ctrl+V copy, cut and paste.
 */
public final class EditBox extends TextBox {
    private static final Vector4fc SELECTION_COLOR = new Vector4f(.3f, .5f, 1f, .4f);

    // The insertion point, as an index into the text.
    private int index;
    // Where the selection began, the other end being the insertion point; -1 when nothing is selected.
    private int anchor = -1;
    // How far along its line the insertion point was when Up or Down began moving it, kept so a short line passed
    // over on the way does not pull it to the left; -1 when not moving up or down.
    private float preferred_x = -1f;

    public EditBox(int width, int height, int max_chars) {
        super(width, height, Skin.getSkin().getEditFont(), max_chars);
    }

    /** Replaces the text, with the insertion point at its end and its start in view. */
    @Override
    public @NonNull EditBox setText(@NonNull CharSequence text) {
        super.setText(text);
        index = length();
        anchor = -1;
        preferred_x = -1f;
        setOffsetY(0);
        return this;
    }

    @Override
    public void clear() {
        super.clear();
        index = 0;
        anchor = -1;
        preferred_x = -1f;
    }

    // ---- Drawing ----

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        Box edit_box = Skin.getSkin().getEditBox();
        renderBox(renderer,
                isDisabled() ? ModeIconQuads.Mode.DISABLED : isActive() ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL);
        var color = isDisabled() ? Label.DISABLED_COLOR : Color.WHITE;
        TextLayout layout = getTextLayout();
        int left = edit_box.getLeftOffset();
        int right = getWidth() - edit_box.getRightOffset();
        if (hasSelection())
            renderSelection(renderer, layout, left, right);
        TextLineRenderer.render(renderer, layout, left, lineY(0), left, right, color);
        if (isActive()) {
            int x = left + layout.getCursorAdvance(index) + getFont().getXBorder() / 2;
            Index.renderIndex(renderer, x, lineY(layout.getCursorLine(index)), getFont(), color);
        }
    }

    /** Where a line is drawn in the box, its first line along the top and scrolled by the offset. */
    private int lineY(int line) {
        return getHeight() - Skin.getSkin().getEditBox().getBottomOffset() - (line + 1) * getFont().getHeight() + getOffsetY();
    }

    private void renderSelection(@NonNull GUIRenderer renderer, @NonNull TextLayout layout, int left, int right) {
        int start = Math.min(anchor, index);
        int end = Math.max(anchor, index);
        int first = layout.getCursorLine(start);
        int last = layout.getCursorLine(end);
        for (int line = first; line <= last; line++) {
            int from = line == first ? layout.getCursorAdvance(start) : 0;
            // A line the selection runs on past shows a little of its end selected too.
            int to = line == last ? layout.getCursorAdvance(end) : layout.getCursorAdvance(layout.getLineEndCharIndex(
                    line)) + getFont().getWidth(" ");
            int x0 = Math.max(left + from, left);
            int x1 = Math.min(left + to, right);
            if (x1 > x0)
                renderer.drawColoredQuad(x0, lineY(line), x1 - x0, getFont().getHeight(), SELECTION_COLOR);
        }
    }

    // ---- Keys ----

    @Override
    protected void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (handleKey(event)) {
                Index.resetBlinking();
                scrollToIndex();
                event.consume();
                return;
            }
            // A letter's key can come before its character; the form around must not take it as a shortcut.
            if (event.getPhase() == InputPhase.PRESSED && !event.isControlDown() && !event.isAltDown()
                    && !event.isMetaDown() && isPrintable(event.getCharacter())) {
                event.consume();
                return;
            }
        }
        super.handleInput(event);
    }

    /** @return whether the key was taken */
    private boolean handleKey(@NonNull InputEvent event) {
        boolean shift = event.isShiftDown();
        boolean ctrl = event.isControlDown() || event.isMetaDown();
        Key key = event.getKeyCode();
        if (ctrl && !event.isAltDown()) {
            if (key == Key.A) {
                anchor = 0;
                index = length();
                return true;
            } else if (key == Key.C) {
                copy();
                return true;
            } else if (key == Key.X) {
                copy();
                deleteSelection();
                return true;
            } else if (key == Key.V) {
                paste();
                return true;
            } else if (key == Key.LEFT) {
                // Word jumps go by the keys themselves, the convention everywhere, rather than rebindable actions.
                moveTo(wordLeft(index), shift);
                return true;
            } else if (key == Key.RIGHT) {
                moveTo(wordRight(index), shift);
                return true;
            } else if (key == Key.HOME) {
                moveTo(0, shift);
                return true;
            } else if (key == Key.END) {
                moveTo(length(), shift);
                return true;
            }
        }
        // Enter starts a new line; Space is also bound to activate, but types a space.
        if (key == Key.RETURN && event.consumeAction(GameAction.UI_ACTIVATE)) {
            type("\n");
        } else if (event.consumeAction(GameAction.UI_NAV_LEFT)) {
            if (hasSelection() && !shift)
                moveTo(Math.min(anchor, index), false);
            else
                moveTo(Math.max(0, index - 1), shift);
        } else if (event.consumeAction(GameAction.UI_NAV_RIGHT)) {
            if (hasSelection() && !shift)
                moveTo(Math.max(anchor, index), false);
            else
                moveTo(Math.min(length(), index + 1), shift);
        } else if (event.consumeAction(GameAction.UI_NAV_UP)) {
            moveLines(-1, shift);
        } else if (event.consumeAction(GameAction.UI_NAV_DOWN)) {
            moveLines(1, shift);
        } else if (event.consumeAction(GameAction.UI_NAV_HOME)) {
            moveTo(getTextLayout().getLineStartCharIndex(getTextLayout().getCursorLine(index)), shift);
        } else if (event.consumeAction(GameAction.UI_NAV_END)) {
            moveTo(getTextLayout().getLineEndCharIndex(getTextLayout().getCursorLine(index)), shift);
        } else if (event.consumeAction(GameAction.UI_BACKSPACE)) {
            if (!deleteSelection() && index > 0)
                remove(index - 1, index);
        } else if (event.consumeAction(GameAction.UI_DELETE)) {
            if (!deleteSelection() && index < length())
                remove(index, index + 1);
        } else if (!ctrl && !event.isAltDown() && isPrintable(event.getCharacter())) {
            type(String.valueOf(event.getCharacter()));
        } else {
            return false;
        }
        return true;
    }

    private boolean isPrintable(char c) {
        return c != 0 && !Character.isISOControl(c) && getFont().getQuad(c) != null;
    }

    /** Moves the insertion point, selecting from where it was if asked to, else dropping any selection. */
    private void moveTo(int new_index, boolean select) {
        if (select && anchor == -1)
            anchor = index;
        else if (!select)
            anchor = -1;
        index = Math.clamp(new_index, 0, length());
        preferred_x = -1f;
    }

    private void moveLines(int lines, boolean select) {
        TextLayout layout = getTextLayout();
        float x = preferred_x >= 0f ? preferred_x : layout.getCursorAdvance(index);
        int line = layout.getCursorLine(index) + lines;
        int new_index;
        if (line < 0)
            new_index = 0;
        else if (line >= layout.getLines().size())
            new_index = length();
        else
            new_index = layout.getIndexInLine(line, x);
        moveTo(new_index, select);
        preferred_x = x;
    }

    private int wordLeft(int from) {
        int i = from;
        while (i > 0 && Character.isWhitespace(charAt(i - 1)))
            i--;
        while (i > 0 && !Character.isWhitespace(charAt(i - 1)))
            i--;
        return i;
    }

    private int wordRight(int from) {
        int i = from;
        while (i < length() && Character.isWhitespace(charAt(i)))
            i++;
        while (i < length() && !Character.isWhitespace(charAt(i)))
            i++;
        return i;
    }

    // ---- Editing ----

    private boolean hasSelection() {
        return anchor != -1 && anchor != index;
    }

    /** @return whether there was a selection to delete */
    private boolean deleteSelection() {
        if (!hasSelection())
            return false;
        remove(Math.min(anchor, index), Math.max(anchor, index));
        return true;
    }

    private void remove(int start, int end) {
        getText().delete(start, end);
        index = start;
        anchor = -1;
        preferred_x = -1f;
        textEdited();
    }

    /** Types over the selection, as far as the box has room, leaving the insertion point after what was typed. */
    private void type(@NonNull String typed) {
        deleteSelection();
        int room = max_chars < 0 ? typed.length() : Math.max(0, max_chars - length());
        String fitted = typed.length() > room ? typed.substring(0, room) : typed;
        if (fitted.isEmpty())
            return;
        getText().insert(index, fitted);
        index += fitted.length();
        anchor = -1;
        preferred_x = -1f;
        textEdited();
    }

    private void copy() {
        if (!hasSelection())
            return;
        String selected = getContents().substring(Math.min(anchor, index), Math.max(anchor, index));
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selected), null);
        } catch (Exception e) {
            System.err.println("Error accessing clipboard: " + e.getMessage());
        }
    }

    private void paste() {
        String pasted;
        try {
            pasted = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (Exception e) {
            System.err.println("Error accessing clipboard: " + e.getMessage());
            return;
        }
        if (pasted == null)
            return;
        // Line ends as Enter types them, tabs as spaces, and nothing the font cannot draw.
        StringBuilder clean = new StringBuilder(pasted.length());
        String text = pasted.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || isPrintable(c))
                clean.append(c);
        }
        type(clean.toString());
    }

    // ---- Scrolling ----

    /** Scrolls just enough for the insertion point's line to show. */
    private void scrollToIndex() {
        Box edit_box = Skin.getSkin().getEditBox();
        int line_height = getFont().getHeight();
        int inner_height = getHeight() - edit_box.getTopOffset() - edit_box.getBottomOffset();
        int top = getTextLayout().getCursorLine(index) * line_height;
        if (top < getOffsetY())
            setOffsetY(top);
        else if (top + line_height > getOffsetY() + inner_height)
            setOffsetY(top + line_height - inner_height);
    }

    // ---- Mouse ----

    @Override
    protected @NonNull CursorType getCursorType() {
        return isDisabled() ? CursorType.NORMAL : CursorType.TEXT;
    }

    @Override
    protected void mousePressed(@NonNull MouseButton button, int x, int y) {
        if (button != MouseButton.LEFT)
            return;
        Index.resetBlinking();
        moveTo(indexAt(x, y), isShiftHeld());
        scrollToIndex();
    }

    @Override
    protected void mouseDragged(@NonNull MouseButton button, int x, int y, int relative_x, int relative_y,
            int absolute_x, int absolute_y) {
        if (button != MouseButton.LEFT)
            return;
        // Dragging comes in screen coordinates, unlike pressing.
        moveTo(indexAt(translateXToLocal(x), translateYToLocal(y)), true);
        scrollToIndex();
    }

    @Override
    protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        // A double click selects the word under the pointer.
        if (button == MouseButton.LEFT && clicks == 2) {
            int at = indexAt(x, y);
            anchor = wordLeft(at);
            index = wordRight(at);
            if (anchor == index)
                anchor = -1;
        }
    }

    /** The insertion point nearest a point in the box. */
    private int indexAt(int x, int y) {
        int left = Skin.getSkin().getEditBox().getLeftOffset();
        int line = Math.floorDiv(lineY(0) + getFont().getHeight() - y, getFont().getHeight());
        TextLayout layout = getTextLayout();
        if (line >= layout.getLines().size())
            return length();
        return layout.getIndexInLine(Math.max(0, line), x - left - getFont().getXBorder() / 2f);
    }

    /** Whether Shift is held, from the per key state, as the mouse events carry no modifiers. */
    private static boolean isShiftHeld() {
        var input = Renderer.getLocalInput();
        return input.isKeyDown(Key.LSHIFT) || input.isKeyDown(Key.RSHIFT);
    }
}
