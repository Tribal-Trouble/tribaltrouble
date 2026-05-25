package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Key {
    UP(GLFW.GLFW_KEY_UP),
    DOWN(GLFW.GLFW_KEY_DOWN),
    LEFT(GLFW.GLFW_KEY_LEFT),
    RIGHT(GLFW.GLFW_KEY_RIGHT),
    ESCAPE(GLFW.GLFW_KEY_ESCAPE),
    SPACE(GLFW.GLFW_KEY_SPACE),
    RETURN(GLFW.GLFW_KEY_ENTER),
    TAB(GLFW.GLFW_KEY_TAB),
    F1(GLFW.GLFW_KEY_F1),
    F2(GLFW.GLFW_KEY_F2),
    F3(GLFW.GLFW_KEY_F3),
    F4(GLFW.GLFW_KEY_F4),
    F5(GLFW.GLFW_KEY_F5),
    F6(GLFW.GLFW_KEY_F6),
    F7(GLFW.GLFW_KEY_F7),
    F8(GLFW.GLFW_KEY_F8),
    F9(GLFW.GLFW_KEY_F9),
    F10(GLFW.GLFW_KEY_F10),
    F11(GLFW.GLFW_KEY_F11),
    F12(GLFW.GLFW_KEY_F12),
    A(GLFW.GLFW_KEY_A),
    B(GLFW.GLFW_KEY_B),
    C(GLFW.GLFW_KEY_C),
    D(GLFW.GLFW_KEY_D),
    E(GLFW.GLFW_KEY_E),
    F(GLFW.GLFW_KEY_F),
    G(GLFW.GLFW_KEY_G),
    H(GLFW.GLFW_KEY_H),
    I(GLFW.GLFW_KEY_I),
    J(GLFW.GLFW_KEY_J),
    K(GLFW.GLFW_KEY_K),
    L(GLFW.GLFW_KEY_L),
    M(GLFW.GLFW_KEY_M),
    N(GLFW.GLFW_KEY_N),
    O(GLFW.GLFW_KEY_O),
    P(GLFW.GLFW_KEY_P),
    Q(GLFW.GLFW_KEY_Q),
    R(GLFW.GLFW_KEY_R),
    S(GLFW.GLFW_KEY_S),
    T(GLFW.GLFW_KEY_T),
    U(GLFW.GLFW_KEY_U),
    V(GLFW.GLFW_KEY_V),
    W(GLFW.GLFW_KEY_W),
    X(GLFW.GLFW_KEY_X),
    Y(GLFW.GLFW_KEY_Y),
    Z(GLFW.GLFW_KEY_Z),
    NUMPAD0(GLFW.GLFW_KEY_KP_0),
    NUMPAD1(GLFW.GLFW_KEY_KP_1),
    NUMPAD2(GLFW.GLFW_KEY_KP_2),
    NUMPAD3(GLFW.GLFW_KEY_KP_3),
    NUMPAD4(GLFW.GLFW_KEY_KP_4),
    NUMPAD5(GLFW.GLFW_KEY_KP_5),
    NUMPAD6(GLFW.GLFW_KEY_KP_6),
    NUMPAD7(GLFW.GLFW_KEY_KP_7),
    NUMPAD8(GLFW.GLFW_KEY_KP_8),
    NUMPAD9(GLFW.GLFW_KEY_KP_9),
    MULTIPLY(GLFW.GLFW_KEY_KP_MULTIPLY),
    DIVIDE(GLFW.GLFW_KEY_KP_DIVIDE),
    DECIMAL(GLFW.GLFW_KEY_KP_DECIMAL),
    DELETE(GLFW.GLFW_KEY_DELETE),
    BACK(GLFW.GLFW_KEY_BACKSPACE),
    HOME(GLFW.GLFW_KEY_HOME),
    END(GLFW.GLFW_KEY_END),
    INSERT(GLFW.GLFW_KEY_INSERT),
    PAGE_UP(GLFW.GLFW_KEY_PAGE_UP),
    PAGE_DOWN(GLFW.GLFW_KEY_PAGE_DOWN),
    LSHIFT(GLFW.GLFW_KEY_LEFT_SHIFT),
    RSHIFT(GLFW.GLFW_KEY_RIGHT_SHIFT),
    LCONTROL(GLFW.GLFW_KEY_LEFT_CONTROL),
    RCONTROL(GLFW.GLFW_KEY_RIGHT_CONTROL),
    LALT(GLFW.GLFW_KEY_LEFT_ALT),
    RALT(GLFW.GLFW_KEY_RIGHT_ALT),
    LSUPER(GLFW.GLFW_KEY_LEFT_SUPER),
    RSUPER(GLFW.GLFW_KEY_RIGHT_SUPER),
    COMMA(GLFW.GLFW_KEY_COMMA),
    PERIOD(GLFW.GLFW_KEY_PERIOD),
    SLASH(GLFW.GLFW_KEY_SLASH),
    BACKSLASH(GLFW.GLFW_KEY_BACKSLASH),
    SEMICOLON(GLFW.GLFW_KEY_SEMICOLON),
    APOSTROPHE(GLFW.GLFW_KEY_APOSTROPHE),
    LBRACKET(GLFW.GLFW_KEY_LEFT_BRACKET),
    RBRACKET(GLFW.GLFW_KEY_RIGHT_BRACKET),
    GRAVE(GLFW.GLFW_KEY_GRAVE_ACCENT),
    KEY_1(GLFW.GLFW_KEY_1),
    KEY_2(GLFW.GLFW_KEY_2),
    KEY_3(GLFW.GLFW_KEY_3),
    KEY_4(GLFW.GLFW_KEY_4),
    KEY_5(GLFW.GLFW_KEY_5),
    KEY_6(GLFW.GLFW_KEY_6),
    KEY_7(GLFW.GLFW_KEY_7),
    KEY_8(GLFW.GLFW_KEY_8),
    KEY_9(GLFW.GLFW_KEY_9),
    KEY_0(GLFW.GLFW_KEY_0),
    EQUALS(GLFW.GLFW_KEY_EQUAL),
    MINUS(GLFW.GLFW_KEY_MINUS),
    ADD(GLFW.GLFW_KEY_KP_ADD),
    SUBTRACT(GLFW.GLFW_KEY_KP_SUBTRACT),
    KEY_UNKNOWN(GLFW.GLFW_KEY_UNKNOWN);

    private final int glfwCode;

    private static final Map<Integer, Key> from_glfw_map = Arrays.stream(values()).collect(Collectors.toMap(
            Key::getGlfwCode, Function.identity()));

    Key(int glfwCode) {
        this.glfwCode = glfwCode;
    }

    public int getGlfwCode() {
        return glfwCode;
    }

    public static Key fromGlfwCode(int code) {
        return from_glfw_map.getOrDefault(code, KEY_UNKNOWN);
    }

    public @NonNull String getDisplayName() {
        String name = name();
        if (name.startsWith("KEY_")) return name.substring(4);
        if (name.startsWith("NUMPAD")) return "Num " + name.substring(6);
        return switch (this) {
            case LSHIFT -> "LShift";
            case RSHIFT -> "RShift";
            case LCONTROL -> "LCtrl";
            case RCONTROL -> "RCtrl";
            case LALT -> "LAlt";
            case RALT -> "RAlt";
            case LSUPER -> "LMeta";
            case RSUPER -> "RMeta";
            case RETURN -> "Enter";
            case BACK -> "Backspace";
            case EQUALS -> "=";
            case MINUS -> "-";
            case COMMA -> ",";
            case PERIOD -> ".";
            case SLASH -> "/";
            case BACKSLASH -> "\\";
            case SEMICOLON -> ";";
            case APOSTROPHE -> "'";
            case LBRACKET -> "[";
            case RBRACKET -> "]";
            case GRAVE -> "`";
            case ADD -> "Num +";
            case SUBTRACT -> "Num -";
            case MULTIPLY -> "Num *";
            case DIVIDE -> "Num /";
            case DECIMAL -> "Num .";
            default -> name;
        };
    }
}
