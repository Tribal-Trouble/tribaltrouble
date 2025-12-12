package com.oddlabs.tt.input;

import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Key {
    UP(Keyboard.KEY_UP),
    DOWN(Keyboard.KEY_DOWN),
    LEFT(Keyboard.KEY_LEFT),
    RIGHT(Keyboard.KEY_RIGHT),
    ESCAPE(Keyboard.KEY_ESCAPE),
    SPACE(Keyboard.KEY_SPACE),
    RETURN(Keyboard.KEY_RETURN),
    TAB(Keyboard.KEY_TAB),
    F1(Keyboard.KEY_F1),
    F2(Keyboard.KEY_F2),
    F3(Keyboard.KEY_F3),
    F4(Keyboard.KEY_F4),
    F5(Keyboard.KEY_F5),
    F6(Keyboard.KEY_F6),
    F7(Keyboard.KEY_F7),
    F8(Keyboard.KEY_F8),
    F9(Keyboard.KEY_F9),
    F11(Keyboard.KEY_F11),
    F12(Keyboard.KEY_F12),
    A(Keyboard.KEY_A),
    B(Keyboard.KEY_B),
    C(Keyboard.KEY_C),
    D(Keyboard.KEY_D),
    E(Keyboard.KEY_E),
    F(Keyboard.KEY_F),
    G(Keyboard.KEY_G),
    H(Keyboard.KEY_H),
    I(Keyboard.KEY_I),
    J(Keyboard.KEY_J),
    M(Keyboard.KEY_M),
    N(Keyboard.KEY_N),
    O(Keyboard.KEY_O),
    P(Keyboard.KEY_P),
    Q(Keyboard.KEY_Q),
    R(Keyboard.KEY_R),
    S(Keyboard.KEY_S),
    T(Keyboard.KEY_T),
    U(Keyboard.KEY_U),
    V(Keyboard.KEY_V),
    W(Keyboard.KEY_W),
    X(Keyboard.KEY_X),
    Z(Keyboard.KEY_Z),
    NUMPAD2(Keyboard.KEY_NUMPAD2),
    NUMPAD3(Keyboard.KEY_NUMPAD3),
    NUMPAD4(Keyboard.KEY_NUMPAD4),
    NUMPAD5(Keyboard.KEY_NUMPAD5),
    NUMPAD6(Keyboard.KEY_NUMPAD6),
    NUMPAD8(Keyboard.KEY_NUMPAD8),
    NUMPAD9(Keyboard.KEY_NUMPAD9),
    DELETE(Keyboard.KEY_DELETE),
    BACK(Keyboard.KEY_BACK),
    HOME(Keyboard.KEY_HOME),
    END(Keyboard.KEY_END),
    INSERT(Keyboard.KEY_INSERT),
    PRIOR(Keyboard.KEY_PRIOR),
    NEXT(Keyboard.KEY_NEXT),
    LSHIFT(Keyboard.KEY_LSHIFT),
    RSHIFT(Keyboard.KEY_RSHIFT),
    LCONTROL(Keyboard.KEY_LCONTROL),
    RCONTROL(Keyboard.KEY_RCONTROL),
    LMENU(Keyboard.KEY_LMENU),
    RMENU(Keyboard.KEY_RMENU),
    KEY_1(Keyboard.KEY_1),
    KEY_2(Keyboard.KEY_2),
    KEY_3(Keyboard.KEY_3),
    KEY_4(Keyboard.KEY_4),
    KEY_5(Keyboard.KEY_5),
    KEY_6(Keyboard.KEY_6),
    KEY_7(Keyboard.KEY_7),
    KEY_8(Keyboard.KEY_8),
    KEY_9(Keyboard.KEY_9),
    KEY_0(Keyboard.KEY_0),
    KEY_UNKNOWN(Keyboard.KEY_NONE);

    private final int lwjglCode;

    private static final Map<Integer, Key> from_lwjgl_map =
            Arrays.stream(values()).collect(Collectors.toMap(Key::getLwjglCode, Function.identity()));

    Key(int lwjglCode) {
        this.lwjglCode = lwjglCode;
    }

    public int getLwjglCode() {
        return lwjglCode;
    }

    public static Key fromLwjglCode(int code) {
        return from_lwjgl_map.getOrDefault(code, KEY_UNKNOWN);
    }
}
