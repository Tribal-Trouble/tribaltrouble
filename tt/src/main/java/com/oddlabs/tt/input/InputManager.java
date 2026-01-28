package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class InputManager {
    private static final Logger logger = Logger.getLogger(InputManager.class.getName());
    private static final Map<GameAction, Set<InputBinding>> DEFAULT_BINDINGS = new EnumMap<>(GameAction.class);

    static {
        // Global
        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        if (isMac) {
            def(GameAction.GLOBAL_QUIT, Key.Q, false, false, false, true); // Meta+Q
        } else {
            def(GameAction.GLOBAL_QUIT, Key.Q, false, true, false, false); // Ctrl+Q
        }

        def(GameAction.GLOBAL_TOGGLE_FULLSCREEN, Key.F11);
        def(GameAction.GLOBAL_SCREENSHOT, Key.S, false, true, false); // Ctrl+S
        def(GameAction.GLOBAL_SCREENSHOT, Key.P, false, true, false); // Ctrl+P
        def(GameAction.GLOBAL_CHAT, Key.RETURN);
        def(GameAction.GLOBAL_MENU, Key.ESCAPE);
        def(GameAction.GLOBAL_TOGGLE_STATUS, Key.I, false, true, false); // Ctrl+I
        def(GameAction.DEBUG_PRINT_INFO, Key.I, false, true, false); // Ctrl+I
        def(GameAction.GLOBAL_AGGRESSIVE_UNITS, Key.A, false, true, false); // Ctrl+A

        // Camera
        def(GameAction.CAMERA_PAN_LEFT, Key.LEFT);
        def(GameAction.CAMERA_PAN_RIGHT, Key.RIGHT);
        def(GameAction.CAMERA_PAN_UP, Key.UP);
        def(GameAction.CAMERA_PAN_DOWN, Key.DOWN);
        
        def(GameAction.CAMERA_PITCH_UP, Key.HOME);
        def(GameAction.CAMERA_PITCH_UP, Key.NUMPAD8);
        def(GameAction.CAMERA_PITCH_UP, Key.UP, false, false, true); // Alt+Up
        
        def(GameAction.CAMERA_PITCH_DOWN, Key.END);
        def(GameAction.CAMERA_PITCH_DOWN, Key.NUMPAD2);
        def(GameAction.CAMERA_PITCH_DOWN, Key.DOWN, false, false, true); // Alt+Down
        
        def(GameAction.CAMERA_ROTATE_RIGHT, Key.INSERT);
        def(GameAction.CAMERA_ROTATE_RIGHT, Key.NUMPAD6);
        def(GameAction.CAMERA_ROTATE_RIGHT, Key.RIGHT, false, false, true); // Alt+Right
        
        def(GameAction.CAMERA_ROTATE_LEFT, Key.DELETE);
        def(GameAction.CAMERA_ROTATE_LEFT, Key.NUMPAD4);
        def(GameAction.CAMERA_ROTATE_LEFT, Key.LEFT, false, false, true); // Alt+Left
        
        def(GameAction.CAMERA_ZOOM_IN, Key.PAGE_UP);
        def(GameAction.CAMERA_ZOOM_IN, Key.NUMPAD9);
        def(GameAction.CAMERA_ZOOM_OUT, Key.PAGE_DOWN);
        def(GameAction.CAMERA_ZOOM_OUT, Key.NUMPAD3);
        
        def(GameAction.CAMERA_MAP_MODE, Key.SPACE);
        def(GameAction.CAMERA_MAP_MODE, Key.NUMPAD5);
        
        def(GameAction.CAMERA_FIRST_PERSON, Key.F);
        def(GameAction.CAMERA_ZOOM_MODE, Key.Z);

        // UI
        def(GameAction.UI_ACTIVATE, Key.SPACE);
        def(GameAction.UI_ACTIVATE, Key.RETURN);
        def(GameAction.UI_CANCEL, Key.ESCAPE);
        def(GameAction.UI_FOCUS_NEXT, Key.TAB);
        def(GameAction.UI_FOCUS_PREV, Key.TAB, true, false, false); // Shift+Tab
        def(GameAction.UI_NEXT_PANEL, Key.TAB, false, true, false); // Ctrl+Tab
        def(GameAction.UI_PREV_PANEL, Key.TAB, true, true, false); // Shift+Ctrl+Tab
        
        def(GameAction.UI_NAV_UP, Key.UP);
        def(GameAction.UI_NAV_DOWN, Key.DOWN);
        def(GameAction.UI_NAV_LEFT, Key.LEFT);
        def(GameAction.UI_NAV_RIGHT, Key.RIGHT);
        def(GameAction.UI_NAV_HOME, Key.HOME);
        def(GameAction.UI_NAV_END, Key.END);
        
        // Shift + Nav (Treat as Nav for now to prevent text insertion fallthrough)
        def(GameAction.UI_NAV_UP, Key.UP, true, false, false);
        def(GameAction.UI_NAV_DOWN, Key.DOWN, true, false, false);
        def(GameAction.UI_NAV_LEFT, Key.LEFT, true, false, false);
        def(GameAction.UI_NAV_RIGHT, Key.RIGHT, true, false, false);
        def(GameAction.UI_NAV_HOME, Key.HOME, true, false, false);
        def(GameAction.UI_NAV_END, Key.END, true, false, false);

        def(GameAction.UI_NAV_PAGE_UP, Key.PAGE_UP);
        def(GameAction.UI_NAV_PAGE_DOWN, Key.PAGE_DOWN);
        def(GameAction.UI_BACKSPACE, Key.BACK);
        def(GameAction.UI_DELETE, Key.DELETE);

        // Gameplay
        def(GameAction.UNIT_MOVE, Key.M);
        def(GameAction.UNIT_ATTACK, Key.A);
        def(GameAction.UNIT_GATHER, Key.G);
        def(GameAction.UNIT_BUILD_QUARTERS, Key.Q);
        def(GameAction.UNIT_BUILD_ARMORY, Key.R);
        def(GameAction.UNIT_BUILD_TOWER, Key.T);
        def(GameAction.UNIT_EXIT_TOWER, Key.X);
        def(GameAction.UNIT_BEACON, Key.B, false, true, false); // Ctrl+B
        def(GameAction.UNIT_NEXT_IDLE, Key.N);
        def(GameAction.UNIT_SET_RALLY, Key.R);
        def(GameAction.GAMEPLAY_BACK, Key.BACK);
        
        // Army Shortcuts (0-9)
        def(GameAction.ARMY_SELECT_0, Key.KEY_0);
        def(GameAction.ARMY_SELECT_1, Key.KEY_1);
        def(GameAction.ARMY_SELECT_2, Key.KEY_2);
        def(GameAction.ARMY_SELECT_3, Key.KEY_3);
        def(GameAction.ARMY_SELECT_4, Key.KEY_4);
        def(GameAction.ARMY_SELECT_5, Key.KEY_5);
        def(GameAction.ARMY_SELECT_6, Key.KEY_6);
        def(GameAction.ARMY_SELECT_7, Key.KEY_7);
        def(GameAction.ARMY_SELECT_8, Key.KEY_8);
        def(GameAction.ARMY_SELECT_9, Key.KEY_9);
        
        def(GameAction.ARMY_CREATE_0, Key.KEY_0, false, true, false); // Ctrl+0
        def(GameAction.ARMY_CREATE_1, Key.KEY_1, false, true, false);
        def(GameAction.ARMY_CREATE_2, Key.KEY_2, false, true, false);
        def(GameAction.ARMY_CREATE_3, Key.KEY_3, false, true, false);
        def(GameAction.ARMY_CREATE_4, Key.KEY_4, false, true, false);
        def(GameAction.ARMY_CREATE_5, Key.KEY_5, false, true, false);
        def(GameAction.ARMY_CREATE_6, Key.KEY_6, false, true, false);
        def(GameAction.ARMY_CREATE_7, Key.KEY_7, false, true, false);
        def(GameAction.ARMY_CREATE_8, Key.KEY_8, false, true, false);
        def(GameAction.ARMY_CREATE_9, Key.KEY_9, false, true, false);

        // Production
        def(GameAction.PROD_WEAPONS, Key.W);
        def(GameAction.PROD_HARVEST, Key.G);
        def(GameAction.PROD_ARMY, Key.A);
        def(GameAction.PROD_TRANSPORT, Key.T);
        
        // Resources
        def(GameAction.RES_TREE, Key.W);
        def(GameAction.RES_ROCK, Key.R);
        def(GameAction.RES_IRON, Key.I);
        def(GameAction.RES_CHICKEN, Key.C);
        
        // Units
        def(GameAction.TRAIN_PEON, Key.P);
        def(GameAction.TRAIN_CHIEFTAIN, Key.C);
        
        // Magic
        def(GameAction.MAGIC_1, Key.S);
        def(GameAction.MAGIC_2, Key.C);
        
        // Misc
        def(GameAction.GAME_SPEED_UP, Key.EQUALS, true, false, false, false); // Shift + = (+)
        def(GameAction.GAME_SPEED_UP, Key.ADD); // Numpad +
        def(GameAction.GAME_SPEED_DOWN, Key.MINUS); // -
        def(GameAction.GAME_SPEED_DOWN, Key.SUBTRACT); // Numpad -
        def(GameAction.NOTIFICATION_JUMP, Key.TAB);
        
        // Cheats
        def(GameAction.CHEAT_1, Key.F1);
        def(GameAction.CHEAT_2, Key.F2);
        def(GameAction.CHEAT_3, Key.F3);
        def(GameAction.CHEAT_4, Key.F4);
        def(GameAction.CHEAT_5, Key.F5);
        def(GameAction.CHEAT_6, Key.F6);
        def(GameAction.CHEAT_7, Key.F7);
        def(GameAction.CHEAT_8, Key.F8);
        def(GameAction.CHEAT_9, Key.F9);
        
        // Debug
        def(GameAction.DEBUG_PRINT_INFO, Key.I, false, true, false); // Ctrl+I
        def(GameAction.DEBUG_KILL_SELECTED, Key.K, false, true, false); // Ctrl+K
        def(GameAction.DEBUG_TOGGLE_LIGHT, Key.L);
        def(GameAction.DEBUG_TOGGLE_LIGHT, Key.O);
        def(GameAction.DEBUG_TOGGLE_PLANTS, Key.P);
        def(GameAction.DEBUG_TOGGLE_PARTICLES, Key.E);
        def(GameAction.DEBUG_TOGGLE_AXES, Key.A);
        def(GameAction.DEBUG_TOGGLE_MISC, Key.M, false, true, false); // Ctrl+M
        def(GameAction.DEBUG_PROCESS_MISC, Key.M);
        def(GameAction.DEBUG_RESET_CURSOR, Key.J);
        def(GameAction.DEBUG_TOGGLE_DETAIL, Key.S);
        def(GameAction.DEBUG_CRASH, Key.C, false, true, false); // Ctrl+C
        def(GameAction.DEBUG_TOGGLE_FRAME_BUFFER, Key.C);
        def(GameAction.DEBUG_TOGGLE_BOUNDING, Key.D);
        def(GameAction.DEBUG_TOGGLE_FRUSTUM_FREEZE, Key.V);
        def(GameAction.DEBUG_FORCE_GC, Key.F12);
        def(GameAction.DEBUG_START_RECORDING, Key.U);
        def(GameAction.DEBUG_TOGGLE_WATER, Key.W, false, true, false); // Ctrl+W
        def(GameAction.DEBUG_TOGGLE_AI, Key.R, false, true, false); // Ctrl+R
        def(GameAction.DEBUG_DUMP_ANIMATIONS, Key.F1);
    }

    private static void def(@NonNull GameAction action, @NonNull Key key) {
        def(action, key, false, false, false, false);
    }

    private static void def(@NonNull GameAction action, @NonNull Key key, boolean shift, boolean control, boolean alt) {
        def(action, key, shift, control, alt, false);
    }

    private static void def(@NonNull GameAction action, @NonNull Key key, boolean shift, boolean control, boolean alt, boolean meta) {
        DEFAULT_BINDINGS.computeIfAbsent(action, k -> new CopyOnWriteArraySet<>()) 
                .add(new InputBinding(key, shift, control, alt, meta, action));
    }

    private final List<@NonNull InputBinding> bindings = new ArrayList<>();
    private final Set<@NonNull GameAction> activeActions = EnumSet.noneOf(GameAction.class);
    private final Map<Key, Set<GameAction>> keyState = new EnumMap<>(Key.class);

    public InputManager() {
        loadDefaultBindings();
    }

    public void loadDefaultBindings() {
        bindings.clear();
        for (Set<InputBinding> set : DEFAULT_BINDINGS.values()) {
            bindings.addAll(set);
        }
    }

    public void loadBindings(@NonNull Properties props) {
        bindings.clear();
        for (GameAction action : GameAction.values()) {
            String key = "key_binding." + action.name();
            String value = props.getProperty(key);
            if (value != null) {
                try {
                    Set<InputBinding> loaded = parseBindings(value, action);
                    if (!loaded.isEmpty()) {
                        bindings.addAll(loaded);
                        continue;
                    }
                } catch (Exception e) {
                    logger.warning("Failed to parse binding for " + action + ": " + value + ". Using defaults.");
                }
            }
            // Fallback to default if property missing or parsing failed/empty
            Set<InputBinding> defaults = DEFAULT_BINDINGS.get(action);
            if (defaults != null) {
                bindings.addAll(defaults);
            }
        }
    }

    public void saveBindings(@NonNull Properties props) {
        // Group current bindings by action
        Map<GameAction, Set<InputBinding>> currentMap = new EnumMap<>(GameAction.class);
        for (InputBinding b : bindings) {
            currentMap.computeIfAbsent(b.action(), k -> new CopyOnWriteArraySet<>()).add(b);
        }

        for (GameAction action : GameAction.values()) {
            Set<InputBinding> current = currentMap.get(action);
            Set<InputBinding> defaults = DEFAULT_BINDINGS.get(action);
            
            // Only save if different from defaults
            if (current != null && !Objects.equals(current, defaults)) {
                props.setProperty("key_binding." + action.name(), serializeBindings(current));
            } else if (current == null && defaults != null) {
                // To support "unbound", we have to save empty list.
                props.setProperty("key_binding." + action.name(), "[]");
            }
        }
    }

    public @NonNull List<InputBinding> getBindings(GameAction action) {
        return bindings.stream()
                .filter(b -> b.action() == action)
                .collect(Collectors.toList());
    }

    public @NonNull List<@NonNull InputBinding> getDefaultBindings(GameAction action) {
        Set<InputBinding> defaults = DEFAULT_BINDINGS.get(action);
        return defaults == null ? new ArrayList<>() : new ArrayList<>(defaults);
    }

    public void setBindings(GameAction action, @NonNull Collection<InputBinding> newBindings) {
        bindings.removeIf(b -> b.action() == action);
        bindings.addAll(newBindings);
    }

    public void resetToDefaults() {
        bindings.clear();
        for (Set<InputBinding> set : DEFAULT_BINDINGS.values()) {
            bindings.addAll(set);
        }
    }

    public @NonNull String exportBindings() {
        // Group by action
        Map<GameAction, Set<InputBinding>> currentMap = new EnumMap<>(GameAction.class);
        for (InputBinding b : bindings) {
            currentMap.computeIfAbsent(b.action(), k -> new CopyOnWriteArraySet<>()).add(b);
        }
        // Serialize each
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        boolean first = true;
        for (Map.Entry<GameAction, Set<InputBinding>> entry : currentMap.entrySet()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  \"").append(entry.getKey().name()).append("\": ");
            sb.append(serializeBindings(entry.getValue()));
        }
        sb.append("\n}");
        return sb.toString();
    }

    public void importBindings(@NonNull String json) {
        // Regex to find "ACTION": [ ... ]
        String patternStr = "\\\"(" + "(\\w+)" + ")\\\"" + "\\s*:\\s*" + "(\\[[^\\]]*\\])";
        Pattern p = Pattern.compile(patternStr);
        Matcher m = p.matcher(json);
        
        List<InputBinding> newBindings = new ArrayList<>();
        boolean foundAny = false;

        while (m.find()) {
            String actionName = m.group(1);
            String arrayContent = m.group(2);
            
            try {
                GameAction action = GameAction.valueOf(actionName);
                Set<InputBinding> parsed = parseBindings(arrayContent, action);
                if (!parsed.isEmpty()) {
                    newBindings.addAll(parsed);
                    foundAny = true;
                }
            } catch (Exception e) {
                logger.warning("Failed to import bindings for " + actionName);
            }
        }
        
        if (foundAny) {
            bindings.clear();
            bindings.addAll(newBindings);
        } else {
            logger.warning("No valid bindings found in JSON import.");
        }
    }

    private @NonNull String serializeBindings(@NonNull Collection<InputBinding> set) {
        return set.stream()
                .map(b -> "{\"key\":\"" + b.key().name() + "\"" +
                                 (b.shift() ? ", \"shift\":true" : "") +
                                 (b.control() ? ", \"control\":true" : "") +
                                 (b.alt() ? ", \"alt\":true" : "") +
                                 (b.meta() ? ", \"meta\":true" : "") +
                                 "}")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private @NonNull Set<InputBinding> parseBindings(@NonNull String json, @NonNull GameAction action) {
        Set<InputBinding> set = new CopyOnWriteArraySet<>();
        String trimmed = json.trim();
        if (trimmed.length() < 2 || !trimmed.startsWith("[") || !trimmed.endsWith("]")) return set;
        
        String content = trimmed.substring(1, trimmed.length() - 1);
        if (content.isBlank()) return set;

        String[] parts = content.split("(?<=})" + "\\s*,\\s*" + "(?=\\{)");
        
        for (String part : parts) {
            InputBinding b = parseBindingObject(part, action);
            if (b != null) set.add(b);
        }
        return set;
    }

    private @Nullable InputBinding parseBindingObject(@NonNull String content, @NonNull GameAction action) {
        // Expected: {"key"="KEY", "mod":true}
        String inner = content.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}")) inner = inner.substring(0, inner.length() - 1);
        
        String[] pairs = inner.split(",");
        Key key = Key.KEY_UNKNOWN;
        boolean shift = false, ctrl = false, alt = false, meta = false;
        
        for (String pair : pairs) {
            // Split by : or =
            String[] kv = pair.split("[:=]");
            if (kv.length != 2) continue;
            String k = unquote(kv[0].trim());
            String v = unquote(kv[1].trim());
            
            try {
                switch (k) {
                    case "key" -> key = Key.valueOf(v);
                    case "shift" -> shift = Boolean.parseBoolean(v);
                    case "control" -> ctrl = Boolean.parseBoolean(v);
                    case "alt" -> alt = Boolean.parseBoolean(v);
                    case "meta" -> meta = Boolean.parseBoolean(v);
                }
            } catch (Exception e) {
                // Ignore invalid keys
                logger.warning("ignoring " + k + ": " + v);
            }
        }
        
        if (key != Key.KEY_UNKNOWN) {
            return new InputBinding(key, shift, ctrl, alt, meta, action);
        }
        return null;
    }

    private @NonNull String unquote(@NonNull String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
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
