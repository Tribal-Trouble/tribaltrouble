package com.oddlabs.tt.input;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.LWJGLException;

public final class LWJGL2InputProvider implements InputProvider {

    @Override
    public void create() {
        try {
            if (!Keyboard.isCreated()) Keyboard.create();
            if (!Mouse.isCreated()) Mouse.create();
        } catch (LWJGLException e) {
            throw new RuntimeException("Failed to create input", e);
        }
    }

    @Override
    public void destroy() {
        if (Keyboard.isCreated()) Keyboard.destroy();
        if (Mouse.isCreated()) Mouse.destroy();
    }

    // Keyboard
    @Override
    public void pollKeyboard() {
        Keyboard.poll();
    }

    @Override
    public boolean nextKeyboardEvent() {
        return Keyboard.next();
    }

    @Override
    public int getEventKey() {
        return Keyboard.getEventKey();
    }

    @Override
    public boolean getEventKeyState() {
        return Keyboard.getEventKeyState();
    }

    @Override
    public char getEventCharacter() {
        return Keyboard.getEventCharacter();
    }

    @Override
    public boolean isRepeatEvent() {
        return Keyboard.isRepeatEvent();
    }

    // Mouse
    @Override
    public void pollMouse() {
        Mouse.poll();
    }

    @Override
    public boolean nextMouseEvent() {
        return Mouse.next();
    }

    @Override
    public int getEventButton() {
        return Mouse.getEventButton();
    }

    @Override
    public boolean getEventButtonState() {
        return Mouse.getEventButtonState();
    }

    @Override
    public int getEventDWheel() {
        return Mouse.getEventDWheel();
    }

    @Override
    public int getEventX() {
        return Mouse.getEventX();
    }

    @Override
    public int getEventY() {
        return Mouse.getEventY();
    }

    @Override
    public int getMouseX() {
        return Mouse.getX();
    }

    @Override
    public int getMouseY() {
        return Mouse.getY();
    }

    @Override
    public boolean isButtonDown(int button) {
        return Mouse.isButtonDown(button);
    }

    @Override
    public void setCursorPosition(int x, int y) {
        Mouse.setCursorPosition(x, y);
    }

    @Override
    public void setGrabbed(boolean grabbed) {
        Mouse.setGrabbed(grabbed);
    }

    @Override
    public boolean isGrabbed() {
        return Mouse.isGrabbed();
    }

    @Override
    public void setNativeCursor(Object cursor) {
        try {
            Mouse.setNativeCursor((org.lwjgl.input.Cursor) cursor);
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }
    }
}
