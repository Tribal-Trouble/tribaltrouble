package com.oddlabs.tt.input;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines groups of {@link GameAction} values that are active at the same time and therefore
 * cannot safely share the same key+modifier binding. Two actions conflict when they appear in
 * the same group and a binding's key+modifiers match exactly.
 *
 * <p>The always-active group (globals, camera, army control-groups, game speed, beacon, idle
 * peon, notification jump) is live in every in-game context, so it additionally conflicts with
 * the unit/building/resource selection groups. It is NOT reserved against the UI-navigation
 * group, whose keys (Space/Return/Esc/Tab/arrows) intentionally double as in-game keys across
 * modes.
 */
public final class KeyBindingConflicts {

    // Active in every in-game context: globals, camera, army control-groups, game speed, beacon, idle peon, notification jump.
    private static final GameAction[] ALWAYS_ACTIVE = {GameAction.GLOBAL_QUIT, GameAction.GLOBAL_TOGGLE_FULLSCREEN, GameAction.GLOBAL_CHAT, GameAction.GLOBAL_CHAT_TEAM, GameAction.GLOBAL_MENU, GameAction.GLOBAL_AGGRESSIVE_UNITS, GameAction.CAMERA_PAN_LEFT, GameAction.CAMERA_PAN_RIGHT, GameAction.CAMERA_PAN_UP, GameAction.CAMERA_PAN_DOWN, GameAction.CAMERA_PITCH_UP, GameAction.CAMERA_PITCH_DOWN, GameAction.CAMERA_ROTATE_LEFT, GameAction.CAMERA_ROTATE_RIGHT, GameAction.CAMERA_ZOOM_IN, GameAction.CAMERA_ZOOM_OUT, GameAction.CAMERA_MAP_MODE, GameAction.CAMERA_FIRST_PERSON, GameAction.CAMERA_ZOOM_MODE, GameAction.UNIT_BEACON, GameAction.UNIT_NEXT_IDLE, GameAction.NOTIFICATION_JUMP, GameAction.GAME_SPEED_UP, GameAction.GAME_SPEED_DOWN, GameAction.ARMY_SELECT_0, GameAction.ARMY_SELECT_1, GameAction.ARMY_SELECT_2, GameAction.ARMY_SELECT_3, GameAction.ARMY_SELECT_4, GameAction.ARMY_SELECT_5, GameAction.ARMY_SELECT_6, GameAction.ARMY_SELECT_7, GameAction.ARMY_SELECT_8, GameAction.ARMY_SELECT_9, GameAction.ARMY_CREATE_0, GameAction.ARMY_CREATE_1, GameAction.ARMY_CREATE_2, GameAction.ARMY_CREATE_3, GameAction.ARMY_CREATE_4, GameAction.ARMY_CREATE_5, GameAction.ARMY_CREATE_6, GameAction.ARMY_CREATE_7, GameAction.ARMY_CREATE_8, GameAction.ARMY_CREATE_9,
    };

    // Active when a unit/group is selected: unit actions, build, magic, exit tower, gameplay back.
    private static final GameAction[] UNIT_CONTEXT = {GameAction.UNIT_MOVE, GameAction.UNIT_ATTACK, GameAction.UNIT_GATHER, GameAction.UNIT_BUILD_QUARTERS, GameAction.UNIT_BUILD_ARMORY, GameAction.UNIT_BUILD_TOWER, GameAction.UNIT_BUILD_SHIP, GameAction.UNIT_EXIT_TOWER, GameAction.MAGIC_1, GameAction.MAGIC_2, GameAction.GAMEPLAY_BACK,
    };

    // Armory/Quarters top-level: production category, set-rally, train chieftain,
    private static final GameAction[] BUILDING_CONTEXT = {GameAction.PROD_WEAPONS, GameAction.PROD_HARVEST, GameAction.PROD_ARMY, GameAction.PROD_TRANSPORT, GameAction.UNIT_SET_RALLY, GameAction.TRAIN_CHIEFTAIN,
    };

    // Resource sub-menus (harvest / transport / weapons all share the same resource keys by design).
    private static final GameAction[] RESOURCE_CONTEXT = {GameAction.RES_TREE, GameAction.RES_TREE_DEC, GameAction.RES_TREE_BATCH, GameAction.RES_TREE_BATCH_DEC, GameAction.RES_ROCK, GameAction.RES_ROCK_DEC, GameAction.RES_ROCK_BATCH, GameAction.RES_ROCK_BATCH_DEC, GameAction.RES_IRON, GameAction.RES_IRON_DEC, GameAction.RES_IRON_BATCH, GameAction.RES_IRON_BATCH_DEC, GameAction.RES_CHICKEN, GameAction.RES_CHICKEN_DEC, GameAction.RES_CHICKEN_BATCH, GameAction.RES_CHICKEN_BATCH_DEC,
    };

    // UI navigation: active inside modal forms / focus traversal. A separate mode, so its keys may double as in-game keys.
    private static final GameAction[] UI_NAV = {GameAction.UI_ACTIVATE, GameAction.UI_CANCEL, GameAction.UI_FOCUS_NEXT, GameAction.UI_FOCUS_PREV, GameAction.UI_NEXT_PANEL, GameAction.UI_PREV_PANEL, GameAction.UI_NAV_UP, GameAction.UI_NAV_DOWN, GameAction.UI_NAV_LEFT, GameAction.UI_NAV_RIGHT, GameAction.UI_NAV_HOME, GameAction.UI_NAV_END, GameAction.UI_NAV_PAGE_UP, GameAction.UI_NAV_PAGE_DOWN, GameAction.UI_BACKSPACE, GameAction.UI_DELETE,
    };
    // Army/Quarters peon training: active only inside army submenu or quarters building.
    // Separated peon context from building context..
    private static final GameAction[] PEON_CONTEXT = {GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC, GameAction.TRAIN_PEON_BATCH, GameAction.TRAIN_PEON_BATCH_DEC,
    };

    // Quarters selected: the peon spinner, train chieftain, and set-rally are all active at the
    // same time, so they may not share keys even though peon training is otherwise free to reuse
    // production/resource keys.
    private static final GameAction[] QUARTERS_CONTEXT = {GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC, GameAction.TRAIN_PEON_BATCH, GameAction.TRAIN_PEON_BATCH_DEC, GameAction.TRAIN_CHIEFTAIN, GameAction.UNIT_SET_RALLY,
    };

    // In-game selection contexts. Mutually exclusive with each other (you have a unit XOR a building
    // selected), so they may safely reuse keys among themselves, but each is active alongside ALWAYS_ACTIVE.
    // Added Peon Context to allow same bind with train peon, prod resource, and build quarters.
    private static final GameAction[][] IN_GAME_CONTEXTS = {UNIT_CONTEXT, BUILDING_CONTEXT, PEON_CONTEXT, RESOURCE_CONTEXT};
    // train peon actions still conflict check against always_active
    private static final GameAction[][] CONFLICT_GROUPS = {ALWAYS_ACTIVE, UNIT_CONTEXT, PEON_CONTEXT, QUARTERS_CONTEXT, BUILDING_CONTEXT, RESOURCE_CONTEXT, UI_NAV};

    private static final Map<GameAction, Set<GameAction>> CONFLICT_MAP = buildConflictMap();

    private static Map<GameAction, Set<GameAction>> buildConflictMap() {
        EnumMap<GameAction, Set<GameAction>> map = new EnumMap<>(GameAction.class);
        for (GameAction[] group : CONFLICT_GROUPS) {
            for (GameAction a : group) {
                Set<GameAction> peers = map.computeIfAbsent(a, k -> EnumSet.noneOf(GameAction.class));
                for (GameAction other : group) {
                    if (other != a) peers.add(other);
                }
            }
        }
        // ALWAYS_ACTIVE actions are live in every in-game context, so they additionally conflict
        // with the selection/building/resource groups (but NOT the UI group, whose keys
        // intentionally double as in-game keys across modes).
        for (GameAction[] context : IN_GAME_CONTEXTS) {
            for (GameAction a : ALWAYS_ACTIVE) {
                Set<GameAction> peers = map.computeIfAbsent(a, k -> EnumSet.noneOf(GameAction.class));
                for (GameAction other : context) {
                    peers.add(other);
                    map.computeIfAbsent(other, k -> EnumSet.noneOf(GameAction.class)).add(a);
                }
            }
        }
        return map;
    }

    private KeyBindingConflicts() {
        // no instances
    }

    /**
     * Returns the first action whose existing bindings collide with the proposed binding.
     * Two bindings collide when the actions can be active at the same time (see class doc) and the
     * key + modifiers match exactly. Returns {@code null} if no conflict.
     */
    public static @Nullable GameAction findConflict(
            @NonNull GameAction action,
            @NonNull InputBinding proposed,
            @NonNull InputManager manager) {
        Set<GameAction> peers = CONFLICT_MAP.get(action);
        if (peers == null) return null;
        for (GameAction other : peers) {
            for (InputBinding existing : manager.getBindings(other)) {
                if (bindingsCollide(existing, proposed)) return other;
            }
        }
        return null;
    }

    /**
     * Returns every other action whose binding collides with any of {@code action}'s current bindings.
     * Used by the panel to highlight rows where the saved bindings already overlap with peers
     * (e.g. after importing or loading older settings).
     */
    public static @NonNull List<GameAction> findExistingConflicts(
            @NonNull GameAction action,
            @NonNull InputManager manager) {
        Set<GameAction> peers = CONFLICT_MAP.get(action);
        if (peers == null) return List.of();
        Set<InputBinding> mine = manager.getBindings(action);
        if (mine.isEmpty()) return List.of();
        List<GameAction> conflicts = new ArrayList<>();
        outer:
        for (GameAction other : peers) {
            for (InputBinding existing : manager.getBindings(other)) {
                for (InputBinding ours : mine) {
                    if (bindingsCollide(existing, ours)) {
                        conflicts.add(other);
                        continue outer;
                    }
                }
            }
        }
        return conflicts;
    }

    private static boolean bindingsCollide(@NonNull InputBinding a, @NonNull InputBinding b) {
        return a.key() == b.key() && a.modifiers().equals(b.modifiers());
    }
}
