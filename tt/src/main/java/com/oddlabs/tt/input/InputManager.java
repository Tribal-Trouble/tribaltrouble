package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InputManager {
    private final List<@NonNull InputBinding> bindings = new ArrayList<>();

    private final Set<@NonNull GameAction> activeActions = EnumSet.noneOf(GameAction.class);
    private final Map<Key, Set<GameAction>> keyState = new EnumMap<>(Key.class);

    public InputManager() {
        loadDefaultBindings();
    }

    public void loadDefaultBindings() {
        bindings.clear();
        
        // Global
        bind(Key.F11, GameAction.GLOBAL_TOGGLE_FULLSCREEN);
        bind(Key.S, false, true, false, GameAction.GLOBAL_SCREENSHOT); // Ctrl+S
        bind(Key.P, false, true, false, GameAction.GLOBAL_SCREENSHOT); // Ctrl+P
        bind(Key.RETURN, GameAction.GLOBAL_CHAT);
        bind(Key.ESCAPE, GameAction.GLOBAL_MENU);
        bind(Key.I, false, true, false, GameAction.GLOBAL_TOGGLE_STATUS); // Ctrl+I
        bind(Key.I, false, true, false, GameAction.DEBUG_PRINT_INFO); // Ctrl+I
        bind(Key.A, false, true, false, GameAction.GLOBAL_AGGRESSIVE_UNITS); // Ctrl+A

        // Camera
        bind(Key.LEFT, GameAction.CAMERA_PAN_LEFT);
        bind(Key.RIGHT, GameAction.CAMERA_PAN_RIGHT);
        bind(Key.UP, GameAction.CAMERA_PAN_UP);
        bind(Key.DOWN, GameAction.CAMERA_PAN_DOWN);
        
        bind(Key.HOME, GameAction.CAMERA_PITCH_UP);
        bind(Key.NUMPAD8, GameAction.CAMERA_PITCH_UP);
        bind(Key.UP, false, false, true, GameAction.CAMERA_PITCH_UP); // Alt+Up
        
        bind(Key.END, GameAction.CAMERA_PITCH_DOWN);
        bind(Key.NUMPAD2, GameAction.CAMERA_PITCH_DOWN);
        bind(Key.DOWN, false, false, true, GameAction.CAMERA_PITCH_DOWN); // Alt+Down
        
        bind(Key.INSERT, GameAction.CAMERA_ROTATE_RIGHT);
        bind(Key.NUMPAD6, GameAction.CAMERA_ROTATE_RIGHT);
        bind(Key.RIGHT, false, false, true, GameAction.CAMERA_ROTATE_RIGHT); // Alt+Right
        
        bind(Key.DELETE, GameAction.CAMERA_ROTATE_LEFT);
        bind(Key.NUMPAD4, GameAction.CAMERA_ROTATE_LEFT);
        bind(Key.LEFT, false, false, true, GameAction.CAMERA_ROTATE_LEFT); // Alt+Left
        
        bind(Key.PAGE_UP, GameAction.CAMERA_ZOOM_IN);
        bind(Key.NUMPAD9, GameAction.CAMERA_ZOOM_IN);
        bind(Key.PAGE_DOWN, GameAction.CAMERA_ZOOM_OUT);
        bind(Key.NUMPAD3, GameAction.CAMERA_ZOOM_OUT);
        
        bind(Key.SPACE, GameAction.CAMERA_MAP_MODE);
        bind(Key.NUMPAD5, GameAction.CAMERA_MAP_MODE);
        
        bind(Key.F, GameAction.CAMERA_FIRST_PERSON);
        bind(Key.Z, GameAction.CAMERA_ZOOM_MODE);

        // UI
        bind(Key.SPACE, GameAction.UI_ACTIVATE);
        bind(Key.RETURN, GameAction.UI_ACTIVATE);
        bind(Key.ESCAPE, GameAction.UI_CANCEL);
        bind(Key.TAB, GameAction.UI_FOCUS_NEXT);
        bind(Key.TAB, true, false, false, GameAction.UI_FOCUS_PREV); // Shift+Tab
        // Currently broken on MacOS due to https://github.com/glfw/glfw/issues/2278
        bind(Key.TAB, false, true, false, GameAction.UI_NEXT_PANEL); // Ctrl+Tab
        // Currently broken on MacOS due to https://github.com/glfw/glfw/issues/2278
        bind(Key.TAB, true, true, false, GameAction.UI_PREV_PANEL); // Shift+Ctrl+Tab
        
        bind(Key.UP, GameAction.UI_NAV_UP);
        bind(Key.DOWN, GameAction.UI_NAV_DOWN);
        bind(Key.LEFT, GameAction.UI_NAV_LEFT);
        bind(Key.RIGHT, GameAction.UI_NAV_RIGHT);
        bind(Key.HOME, GameAction.UI_NAV_HOME);
        bind(Key.END, GameAction.UI_NAV_END);
        
        // Shift + Nav (Treat as Nav for now to prevent text insertion fallthrough)
        bind(Key.UP, true, false, false, GameAction.UI_NAV_UP);
        bind(Key.DOWN, true, false, false, GameAction.UI_NAV_DOWN);
        bind(Key.LEFT, true, false, false, GameAction.UI_NAV_LEFT);
        bind(Key.RIGHT, true, false, false, GameAction.UI_NAV_RIGHT);
        bind(Key.HOME, true, false, false, GameAction.UI_NAV_HOME);
        bind(Key.END, true, false, false, GameAction.UI_NAV_END);

        bind(Key.PAGE_UP, GameAction.UI_NAV_PAGE_UP);
        bind(Key.PAGE_DOWN, GameAction.UI_NAV_PAGE_DOWN);
        bind(Key.BACK, GameAction.UI_BACKSPACE);
        bind(Key.DELETE, GameAction.UI_DELETE);

        // Gameplay
        bind(Key.M, GameAction.UNIT_MOVE);
        bind(Key.A, GameAction.UNIT_ATTACK);
        bind(Key.G, GameAction.UNIT_GATHER);
        bind(Key.Q, GameAction.UNIT_BUILD_QUARTERS);
        bind(Key.R, GameAction.UNIT_BUILD_ARMORY);
        bind(Key.T, GameAction.UNIT_BUILD_TOWER);
        bind(Key.X, GameAction.UNIT_EXIT_TOWER);
        bind(Key.B, false, true, false, GameAction.UNIT_BEACON); // Ctrl+B
        bind(Key.N, GameAction.UNIT_NEXT_IDLE);
        bind(Key.R, GameAction.UNIT_SET_RALLY);
        bind(Key.BACK, GameAction.GAMEPLAY_BACK);
        
        // Army Shortcuts (0-9)
        bind(Key.KEY_0, GameAction.ARMY_SELECT_0);
        bind(Key.KEY_1, GameAction.ARMY_SELECT_1);
        bind(Key.KEY_2, GameAction.ARMY_SELECT_2);
        bind(Key.KEY_3, GameAction.ARMY_SELECT_3);
        bind(Key.KEY_4, GameAction.ARMY_SELECT_4);
        bind(Key.KEY_5, GameAction.ARMY_SELECT_5);
        bind(Key.KEY_6, GameAction.ARMY_SELECT_6);
        bind(Key.KEY_7, GameAction.ARMY_SELECT_7);
        bind(Key.KEY_8, GameAction.ARMY_SELECT_8);
        bind(Key.KEY_9, GameAction.ARMY_SELECT_9);
        
        bind(Key.KEY_0, false, true, false, GameAction.ARMY_CREATE_0); // Ctrl+0
        bind(Key.KEY_1, false, true, false, GameAction.ARMY_CREATE_1);
        bind(Key.KEY_2, false, true, false, GameAction.ARMY_CREATE_2);
        bind(Key.KEY_3, false, true, false, GameAction.ARMY_CREATE_3);
        bind(Key.KEY_4, false, true, false, GameAction.ARMY_CREATE_4);
        bind(Key.KEY_5, false, true, false, GameAction.ARMY_CREATE_5);
        bind(Key.KEY_6, false, true, false, GameAction.ARMY_CREATE_6);
        bind(Key.KEY_7, false, true, false, GameAction.ARMY_CREATE_7);
        bind(Key.KEY_8, false, true, false, GameAction.ARMY_CREATE_8);
        bind(Key.KEY_9, false, true, false, GameAction.ARMY_CREATE_9);

        // Production
        bind(Key.W, GameAction.PROD_WEAPONS);
        bind(Key.G, GameAction.PROD_HARVEST);
        bind(Key.A, GameAction.PROD_ARMY);
        bind(Key.T, GameAction.PROD_TRANSPORT);
        
        // Resources
        bind(Key.W, GameAction.RES_TREE);
        bind(Key.R, GameAction.RES_ROCK);
        bind(Key.I, GameAction.RES_IRON);
        bind(Key.C, GameAction.RES_CHICKEN);
        
        // Units
        bind(Key.P, GameAction.TRAIN_PEON);
        bind(Key.C, GameAction.TRAIN_CHIEFTAIN);
        
        // Magic
        bind(Key.S, GameAction.MAGIC_1);
        bind(Key.C, GameAction.MAGIC_2);
        
        // Misc
        bind(Key.EQUALS, true, false, false, false, GameAction.GAME_SPEED_UP); // Shift + = (+)
        bind(Key.ADD, GameAction.GAME_SPEED_UP); // Numpad +
        bind(Key.MINUS, GameAction.GAME_SPEED_DOWN); // -
        bind(Key.SUBTRACT, GameAction.GAME_SPEED_DOWN); // Numpad -
        bind(Key.TAB, GameAction.NOTIFICATION_JUMP);
        
        // Cheats
        bind(Key.F1, GameAction.CHEAT_1);
        bind(Key.F2, GameAction.CHEAT_2);
        bind(Key.F3, GameAction.CHEAT_3);
        bind(Key.F4, GameAction.CHEAT_4);
        bind(Key.F5, GameAction.CHEAT_5);
        bind(Key.F6, GameAction.CHEAT_6);
        bind(Key.F7, GameAction.CHEAT_7);
        bind(Key.F8, GameAction.CHEAT_8);
        bind(Key.F9, GameAction.CHEAT_9);
        
        // Debug
        bind(Key.I, false, true, false, GameAction.DEBUG_PRINT_INFO); // Ctrl+I
        bind(Key.K, false, true, false, GameAction.DEBUG_KILL_SELECTED); // Ctrl+K
        bind(Key.L, GameAction.DEBUG_TOGGLE_LIGHT);
        bind(Key.O, GameAction.DEBUG_TOGGLE_LIGHT);
        bind(Key.P, GameAction.DEBUG_TOGGLE_PLANTS);
        bind(Key.E, GameAction.DEBUG_TOGGLE_PARTICLES);
        bind(Key.A, GameAction.DEBUG_TOGGLE_AXES);
        bind(Key.M, false, true, false, GameAction.DEBUG_TOGGLE_MISC); // Ctrl+M
        bind(Key.M, GameAction.DEBUG_PROCESS_MISC);
        bind(Key.J, GameAction.DEBUG_RESET_CURSOR);
        bind(Key.S, GameAction.DEBUG_TOGGLE_DETAIL);
        bind(Key.C, false, true, false, GameAction.DEBUG_CRASH); // Ctrl+C
        bind(Key.C, GameAction.DEBUG_TOGGLE_FRAME_BUFFER);
        bind(Key.D, GameAction.DEBUG_TOGGLE_BOUNDING);
        bind(Key.V, GameAction.DEBUG_TOGGLE_FRUSTUM_FREEZE);
        bind(Key.F12, GameAction.DEBUG_FORCE_GC);
        bind(Key.U, GameAction.DEBUG_START_RECORDING);
        bind(Key.W, false, true, false, GameAction.DEBUG_TOGGLE_WATER); // Ctrl+W
        bind(Key.R, false, true, false, GameAction.DEBUG_TOGGLE_AI); // Ctrl+R
        bind(Key.F1, GameAction.DEBUG_DUMP_ANIMATIONS);
    }

    private void bind(@NonNull Key key, @NonNull GameAction action) {
        bind(key, false, false, false, false, action);
    }

    private void bind(@NonNull Key key, boolean shift, boolean control, boolean alt,  @NonNull GameAction action) {
        bind(key, shift, control, alt, false, action);
    }

    private void bind(@NonNull Key key, boolean shift, boolean control, boolean alt, boolean meta, @NonNull GameAction action) {
        bindings.add(new InputBinding(key, shift, control, alt, meta, action));
    }

    public @NonNull Set<@NonNull GameAction> getActions(@NonNull KeyboardEvent event) {
        Set<GameAction> actions = EnumSet.noneOf(GameAction.class);
        for (InputBinding binding : bindings) {
            if (binding.matches(event)) {
                actions.add(binding.action());
            }
        }
        return actions;
    }
    
    // Called by LocalInput or InputState to update polling state
    public void updateState(@NonNull KeyboardEvent event, boolean pressed) {
        if (pressed) {
            Set<GameAction> actions = getActions(event);
            if (!actions.isEmpty()) {
                keyState.put(event.keyCode(), actions);
                activeActions.addAll(actions);
            }
        } else {
            Set<GameAction> actions = keyState.remove(event.keyCode());
            if (actions != null) {
                activeActions.removeAll(actions);
            }
        }
    }
    
    public boolean isActive(@NonNull GameAction action) {
        return activeActions.contains(action);
    }

    public void reset() {
        activeActions.clear();
        keyState.clear();
    }
}
