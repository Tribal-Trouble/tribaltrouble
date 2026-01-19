package com.oddlabs.tt.input;

import org.jspecify.annotations.Nullable;

public interface InputProvider<C> extends AutoCloseable {
    // Keyboard
    void pollKeyboard();
    boolean nextKeyboardEvent();
    int getEventKey();
    /** {@return true if there is a key event and the key is down} */
    boolean getEventKeyState();
    int getEventKeyMods();
    char getEventCharacter();
    boolean isRepeatEvent();
    boolean isKeyDown(int keyCode);

    // Mouse
    void pollMouse();
    boolean nextMouseEvent();
    int getEventButton();
    boolean getEventButtonState();
    int getEventDWheel();
    int getEventX();
    int getEventY(); // Note: Coordinate system (Bottom-Left vs Top-Left) depends on implementation.
                     // The engine currently expects Bottom-Left.
    
    // Mouse State
    int getMouseX();
    int getMouseY();
    boolean isButtonDown(int button);
    void setCursorPosition(int x, int y);
    void setGrabbed(boolean grabbed);
    boolean isGrabbed();
    
    // Cursor
    void setNativeCursor(@Nullable C cursor);

    @Override
    void close();
}
