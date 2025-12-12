package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;

public interface InputProvider {
    // Keyboard
    void pollKeyboard();
    boolean nextKeyboardEvent();
    int getEventKey();
    boolean getEventKeyState();
    char getEventCharacter();
    boolean isRepeatEvent();

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
    void setNativeCursor(Object cursor);
    
    // Initialization/Teardown
    void create();
    void destroy();
}
