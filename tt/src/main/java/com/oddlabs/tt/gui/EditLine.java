package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Index;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * A single-line text input field. Supports basic text editing, cursor navigation,
 * input filtering, and horizontal scrolling for text that exceeds the visual width.
 */
public class EditLine extends TextField implements Clipped {
    @SuppressWarnings("TimeUnitConversionChecker")
    private static final long ERROR_DURATION = TimeUnit.MILLISECONDS.toMillis(200);
    private final Set<@NonNull EnterListener> enter_listeners = new CopyOnWriteArraySet<>();
    private final @NonNull Origin alignment;
    private final @Nullable String allowed_chars;
    protected final int max_text_width;
    private final int max_chars;

    private int offset_x;
    private int index;
    private int selectionStart = -1;
    private int selectionEnd = -1;

    private long errorFlashStart = 0;

    public EditLine(int width, int max_codepoints) {
        this(width, max_codepoints, Origin.AT_START);
    }

    public EditLine(int width, int max_codepoints, @NonNull Origin alignment) {
        this(width, max_codepoints, null, alignment);
    }

    public EditLine(int width, int max_codepoints, @Nullable String allowed_chars, @NonNull Origin alignment) {
        super(Skin.getSkin().getEditFont(), max_codepoints);
        this.allowed_chars = allowed_chars;
        this.alignment = alignment;
        this.max_chars = max_codepoints;
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
        var mode = isDisabled() ? ModeIconQuads.Mode.DISABLED : (isActive() ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL);
        edit_box.render(renderer, 0f, 0f, getWidth(), getHeight(), mode);

        long elapsed = System.currentTimeMillis() - errorFlashStart;
        if (elapsed < ERROR_DURATION) {
            var elapsed_percent = 1.0f - ((float) elapsed / ERROR_DURATION);
            var color = Color.Linear.WHITE.alpha(0.5f * elapsed_percent);
            renderer.drawColoredQuad(2, 2, getWidth() - 4, getHeight() - 4, color);
        }

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
        TextLineRenderer.render(renderer, getFont(), displayText, box.getLeftOffset() + offset_x, box.getBottomOffset(),
                box.getLeftOffset() + 1, getWidth() - box.getRightOffset() - 1, Color.Linear.WHITE);
        if (render_index != -1) {
            int cursorX = getRenderedWidth(displayText.subSequence(0, render_index));
            Index.renderIndex(renderer, box.getLeftOffset() + offset_x + cursorX, box.getBottomOffset(), getFont(),
                    Color.Linear.WHITE);
        }
        renderSelectionHighlight(renderer, displayText, box, offset_x);
    }

    @Override
    protected boolean insert(int index, int codepoint) {
        boolean result = super.insert(index, codepoint);
        if (result) {
            this.index += Character.charCount(codepoint);
        }
        return result;
    }

    private static final boolean ERROR_SOUND_ENABLED = false;

    public void triggerError() {
        errorFlashStart = System.currentTimeMillis();
        if (ERROR_SOUND_ENABLED) {
            try {
                Renderer.getRenderer().getAudioManager().newAudio(0f, 0f, 0f, AudioAssets.ERROR_SOUND);
            } catch (Exception _) {
                // Ignore audio errors
            }
        }
    }

    @Override
    protected void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.RELEASED) {
            if (event.consumeAction(GameAction.UI_ACTIVATE)) {
                if (event.getKeyCode() == Key.RETURN) {
                    enterPressedAll();
                }
                return;
            }
        }

        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            // Try selection/clipboard shortcuts first
            if (handleClipboardInput(event) || handleSelectionNavigation(event)) {
                correctOffsetX();
                event.consume();
                return;
            }

            // If there's a selection and the user types/deletes, handle it before basic nav
            if (handleSelectionReplacement(event)) {
                correctOffsetX();
                event.consume();
                return;
            }

            // Basic navigation and editing (Bondolo's original logic)
            boolean consumed = true;

            if (event.consumeAction(GameAction.UI_NAV_LEFT)) {
                if (index > 0) {
                    index -= Character.charCount(getText().codePointBefore(index));
                }
            } else if (event.consumeAction(GameAction.UI_NAV_RIGHT)) {
                if (index < getText().length()) {
                    index += Character.charCount(getText().codePointAt(index));
                }
            } else if (event.consumeAction(GameAction.UI_NAV_HOME)) {
                index = 0;
            } else if (event.consumeAction(GameAction.UI_NAV_END)) {
                index = getText().length();
            } else if (event.consumeAction(GameAction.UI_BACKSPACE)) {
                if (index > 0) {
                    int cp = getText().codePointBefore(index);
                    index -= Character.charCount(cp);
                    delete(index);
                } else triggerError();
            } else if (event.consumeAction(GameAction.UI_DELETE)) {
                if (index < getText().length()) delete(index);
            } else if (!event.isControlDown() && !event.isMetaDown() && !event.isAltDown()) {
                int c = event.getCodepoint();
                if (c != 0 && !Character.isISOControl(c)) {
                    if (c == ' ') { // Check if the character is SPACE
                        event.consumeAction(GameAction.UI_ACTIVATE);
                    }
                    consumed = insert(index, c);
                    if (!consumed) triggerError();
                } else {
                    consumed = false;
                }
            } else {
                consumed = false;
            }

            if (consumed) {
                correctOffsetX();
                event.consume();
                return;
            }

            // Consume printable keys in PRESSED phase to prevent bubbling (e.g. 'D' triggering debug bounds)
            // even if the character hasn't been typed yet (will come in REPEAT phase).
            if (event.getPhase() == InputPhase.PRESSED && !event.isControlDown() && !event.isAltDown() && !event
                    .isMetaDown()) {
                int c = event.getCodepoint();
                if (c != 0 && !Character.isISOControl(c)) {
                    event.consume();
                    return;
                }
            }
        }

        super.handleInput(event);
    }

    @Override
    public final boolean isAllowed(int codepoint) {
        return super.isAllowed(codepoint) && getFont().getQuad(codepoint) != null && (allowed_chars == null
                || allowed_chars.indexOf(codepoint) != -1);
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
        clearSelection();
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
        clearSelection();
        correctOffsetX();
    }

    @Override
    protected final void mouseEntered() {
    }

    @Override
    protected final void mouseExited() {
    }

    @Override
    protected final void mousePressed(@NonNull MouseButton button, int x, int y) {
        if (button == MouseButton.LEFT) {
            Box edit_box = Skin.getSkin().getEditBox();
            float relativeX = x - (edit_box.getLeftOffset() + offset_x);

            int bestIndex = 0;
            float bestDx = Float.MAX_VALUE;

            var displayText = getDisplayText();
            for (int i = 0; i <= displayText.length();) {
                int charX = getRenderedWidth(displayText.subSequence(0, i));
                float dx = Math.abs(relativeX - charX);
                if (dx < bestDx) {
                    bestDx = dx;
                    bestIndex = i;
                }
                if (i < displayText.length()) {
                    i += Character.charCount(Character.codePointAt(displayText, i));
                } else {
                    break;
                }
            }
            index = bestIndex;
            clearSelection();
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

    //region Selection Input Handling

    /**
     * Handle Ctrl+C/V/X/A clipboard shortcuts. Returns true if handled.
     */
    private boolean handleClipboardInput(@NonNull InputEvent event) {
        boolean ctrl = event.isControlDown() || event.isMetaDown();
        if (!ctrl) return false;

        if (event.getKeyCode() == Key.A) {
            selectionStart = 0;
            selectionEnd = getText().length();
            index = selectionEnd;
            return true;
        } else if (event.getKeyCode() == Key.C) {
            copyToClipboard();
            return true;
        } else if (event.getKeyCode() == Key.V) {
            pasteFromClipboard();
            return true;
        } else if (event.getKeyCode() == Key.X) {
            cutToClipboard();
            return true;
        }
        return false;
    }

    /**
     * Handle shift+arrow selection and ctrl+arrow word jump. Returns true if handled.
     */
    private boolean handleSelectionNavigation(@NonNull InputEvent event) {
        boolean shift = event.isShiftDown();
        boolean ctrl = event.isControlDown() || event.isMetaDown();

        // Only handle shift-navigation or ctrl+arrow word jumps here
        if (!shift && !ctrl) return false;

        int oldIndex = index;

        // Word jumping uses raw key codes intentionally � Ctrl+Left/Right is a universal
        // OS text editing convention and should not be rebindable via GameAction bindings.
        if (ctrl && event.getKeyCode() == Key.LEFT) {
            index = wordBoundaryLeft(index);
            updateSelection(shift, oldIndex);
            return true;
        } else if (ctrl && event.getKeyCode() == Key.RIGHT) {
            index = wordBoundaryRight(index);
            updateSelection(shift, oldIndex);
            return true;
        } else if (shift && event.consumeAction(GameAction.UI_NAV_LEFT)) {
            if (index > 0) index--;
            updateSelection(true, oldIndex);
            return true;
        } else if (shift && event.consumeAction(GameAction.UI_NAV_RIGHT)) {
            if (index < getText().length()) index++;
            updateSelection(true, oldIndex);
            return true;
        } else if (shift && event.consumeAction(GameAction.UI_NAV_HOME)) {
            index = 0;
            updateSelection(true, oldIndex);
            return true;
        } else if (shift && event.consumeAction(GameAction.UI_NAV_END)) {
            index = getText().length();
            updateSelection(true, oldIndex);
            return true;
        }
        return false;
    }

    /**
     * If there's an active selection and the user types, backspaces, or deletes,
     * replace/delete the selection. Returns true if handled, false to fall through
     * to basic navigation.
     */
    private boolean handleSelectionReplacement(@NonNull InputEvent event) {
        if (!hasSelection()) return false;

        if (event.hasAction(GameAction.UI_BACKSPACE) || event.hasAction(GameAction.UI_DELETE)) {
            event.consumeAction(GameAction.UI_BACKSPACE);
            event.consumeAction(GameAction.UI_DELETE);
            deleteSelection();
            return true;
        }

        if (!event.isControlDown() && !event.isMetaDown() && !event.isAltDown()) {
            int c = event.getCodepoint();
            if (c != 0 && !Character.isISOControl(c)) {
                deleteSelection();
                boolean result = insert(index, c);
                if (!result) triggerError();
                return true;
            }
        }

        // Arrow keys with selection but no shift � jump to selection edge and clear
        if (!event.isShiftDown()) {
            if (event.hasAction(GameAction.UI_NAV_LEFT) || event.hasAction(GameAction.UI_NAV_HOME)) {
                event.consumeAction(GameAction.UI_NAV_LEFT);
                event.consumeAction(GameAction.UI_NAV_HOME);
                index = Math.min(selectionStart, selectionEnd);
                clearSelection();
                return true;
            }
            if (event.hasAction(GameAction.UI_NAV_RIGHT) || event.hasAction(GameAction.UI_NAV_END)) {
                event.consumeAction(GameAction.UI_NAV_RIGHT);
                event.consumeAction(GameAction.UI_NAV_END);
                index = Math.max(selectionStart, selectionEnd);
                clearSelection();
                return true;
            }
            clearSelection();
        }
        return false;
    }

    //endregion

    //region Selection Rendering

    private void renderSelectionHighlight(@NonNull GUIRenderer renderer, @NonNull CharSequence displayText,
            @NonNull Box box, int offset_x) {
        if (!hasSelection()) return;
        int selStartX = getRenderedWidth(displayText.subSequence(0, selectionStart));
        int selEndX = getRenderedWidth(displayText.subSequence(0, selectionEnd));
        int highlightLeft = Math.max(box.getLeftOffset() + offset_x + Math.min(selStartX, selEndX),
                box.getLeftOffset() + 1);
        int highlightRight = Math.min(box.getLeftOffset() + offset_x + Math.max(selStartX, selEndX),
                getWidth() - box.getRightOffset() - 1);
        if (highlightRight > highlightLeft) {
            renderer.drawColoredQuad(highlightLeft, box.getBottomOffset(), highlightRight - highlightLeft,
                    getFont().getHeight(), new Color.Standard(0.3f, 0.5f, 1.0f, 0.4f).linear());
        }
    }

    //endregion

    //region Selection State & Clipboard

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    private boolean deleteSelection() {
        if (!hasSelection()) return false;
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        String contents = getContents();
        String newContent = contents.substring(0, start) + contents.substring(end);
        set(newContent);
        index = start;
        clearSelection();
        return true;
    }

    private void updateSelection(boolean shiftDown, int oldIndex) {
        if (shiftDown) {
            if (selectionStart == -1) {
                selectionStart = oldIndex;
                selectionEnd = index;
            } else {
                if (oldIndex == selectionStart) {
                    selectionStart = index;
                } else {
                    selectionEnd = index;
                }
            }
            if (selectionStart > selectionEnd) {
                int tmp = selectionStart;
                selectionStart = selectionEnd;
                selectionEnd = tmp;
            }
        } else {
            clearSelection();
        }
    }

    private void copyToClipboard() {
        if (!hasSelection()) return;
        try {
            String selectedText = getContents().substring(
                    Math.min(selectionStart, selectionEnd),
                    Math.max(selectionStart, selectionEnd));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(selectedText), null);
        } catch (Exception e) {
            System.err.println("Error accessing clipboard: " + e.getMessage());
        }
    }

    private void pasteFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String text = (String) clipboard.getData(DataFlavor.stringFlavor);
            if (text == null || text.isEmpty()) return;

            for (char c : text.toCharArray()) {
                if (!isAllowed(c)) return;
            }

            deleteSelection();

            String contents = getContents();
            int insertPos = index;
            String newContent = contents.substring(0, insertPos) + text + contents.substring(insertPos);
            if (max_chars != -1 && newContent.length() > max_chars) return;
            set(newContent);
            index = Math.min(insertPos + text.length(), newContent.length());
            correctOffsetX();
        } catch (Exception e) {
            System.err.println("Error accessing clipboard: " + e.getMessage());
        }
    }

    private void cutToClipboard() {
        if (!hasSelection()) return;
        copyToClipboard();
        deleteSelection();
    }

    private int wordBoundaryLeft(int pos) {
        String contents = getContents();
        int i = pos - 1;
        while (i > 0 && contents.charAt(i) == ' ') i--;
        while (i > 0 && contents.charAt(i - 1) != ' ') i--;
        return Math.max(0, i);
    }

    private int wordBoundaryRight(int pos) {
        String contents = getContents();
        int i = pos;
        while (i < contents.length() && contents.charAt(i) == ' ') i++;
        while (i < contents.length() && contents.charAt(i) != ' ') i++;
        return i;
    }

    //endregion
}
