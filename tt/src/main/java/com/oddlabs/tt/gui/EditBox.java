package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Index;
import com.oddlabs.tt.font.TextLayout;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

public final class EditBox extends TextBox {
    private int index;

    public EditBox(int width, int height, int max_chars) {
        super(width, height, Skin.getSkin().getEditFont(), max_chars);
        index = 0;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        Box edit_box = Skin.getSkin().getEditBox();
        super.renderBox(renderer, isDisabled() ? ModeIconQuads.Mode.DISABLED : ModeIconQuads.Mode.NORMAL);
        var c = isDisabled() ? Label.DISABLED_COLOR : Color.WHITE;

        TextLineRenderer.render(renderer, getTextLayout(), edit_box.getLeftOffset(),
                getHeight() - edit_box.getBottomOffset() - getFont().getHeight() + getOffsetY(),
                edit_box.getLeftOffset(), getWidth() - edit_box.getRightOffset(), c);

        if (isActive()) {
            TextLayout layout = getTextLayout();
            int cursorLine = layout.getCursorLine(index);
            int cursorX = layout.getCursorX(index);
            int cursorY = getHeight() - edit_box.getBottomOffset() - getFont().getHeight() - (cursorLine * getFont().getHeight()) + getOffsetY();
            Index.renderIndex(renderer, edit_box.getLeftOffset() + cursorX, cursorY, getFont(), c);
        }
    }

    @Override
    protected void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            boolean consumed = true;

            if (event.consumeAction(GameAction.UI_ACTIVATE)) {
                if (insert(index, '\n')) index++;
            } else if (event.consumeAction(GameAction.UI_NAV_LEFT)) {
                if (index > 0) index--;
            } else if (event.consumeAction(GameAction.UI_NAV_RIGHT)) {
                if (index < getText().length()) index++;
            } else if (event.consumeAction(GameAction.UI_NAV_UP)) {
                int currentLine = getTextLayout().getCursorLine(index);
                if (currentLine > 0) {
                    int xPos = getTextLayout().getCursorX(index);
                    index = getTextLayout().getCharacterIndexAt(xPos, (currentLine - 0.5f) * getFont().getHeight(),
                            getTextLayout().getTextHeight());
                } else {
                    index = 0;
                }
            } else if (event.consumeAction(GameAction.UI_NAV_DOWN)) {
                int currentLine = getTextLayout().getCursorLine(index);
                if (currentLine < getTextLayout().getLines().size() - 1) {
                    int xPos = getTextLayout().getCursorX(index);
                    index = getTextLayout().getCharacterIndexAt(xPos, (currentLine + 1.5f) * getFont().getHeight(),
                            getTextLayout().getTextHeight());
                } else {
                    index = getText().length();
                }
            } else if (event.consumeAction(GameAction.UI_NAV_HOME)) {
                index = getTextLayout().getLineStartCharIndex(getTextLayout().getCursorLine(index));
            } else if (event.consumeAction(GameAction.UI_NAV_END)) {
                index = getTextLayout().getLineEndCharIndex(getTextLayout().getCursorLine(index));
            } else if (event.consumeAction(GameAction.UI_BACKSPACE)) {
                if (index > 0) delete(--index);
            } else if (event.consumeAction(GameAction.UI_DELETE)) {
                if (index < getText().length()) delete(index);
            } else if (!event.isControlDown() && !event.isMetaDown() && !event.isAltDown()) {
                char key = event.getCharacter();
                if (key != 0 && !Character.isISOControl(key) && getFont().getQuad(key) != null) {
                    if (insert(index, key)) index++;
                } else {
                    consumed = false;
                }
            } else {
                consumed = false;
            }

            if (consumed) {
                correctOffsetY();
                event.consume();
                return;
            }
        }

        super.handleInput(event);
    }

    private void correctOffsetY() {
        TextLayout layout = getTextLayout();
        int cursorLine = layout.getCursorLine(index);
        int lineHeight = getFont().getHeight();

        Box edit_box = Skin.getSkin().getEditBox();
        int visibleHeight = getHeight() - edit_box.getTopOffset() - edit_box.getBottomOffset();
        int visibleLines = visibleHeight / lineHeight;

        int currentTopLine = -getOffsetY() / lineHeight;

        if (cursorLine < currentTopLine) {
            setOffsetY(-cursorLine * lineHeight);
        } else if (cursorLine >= currentTopLine + visibleLines) {
            setOffsetY(-(cursorLine - visibleLines + 1) * lineHeight);
        }
    }

    @Override
    protected @NonNull CursorType getCursorType() {
        return isDisabled() ? CursorType.NORMAL : CursorType.TEXT;
    }

    @Override
    protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        if (button == MouseButton.LEFT) {
            Box edit_box = Skin.getSkin().getEditBox();
            float relativeX = x - (getRootX() + edit_box.getLeftOffset());
            float relativeY = y - (getRootY() + edit_box.getBottomOffset() + getOffsetY());
            index = getTextLayout().getCharacterIndexAt(relativeX, relativeY, getTextLayout().getTextHeight());
            correctOffsetY();
        }
    }
}
