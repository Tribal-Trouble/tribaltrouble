package com.oddlabs.tt.input;

public enum GameAction {
    // Global
    GLOBAL_QUIT,
    GLOBAL_TOGGLE_FULLSCREEN,
    GLOBAL_SCREENSHOT,
    GLOBAL_CHAT,
    GLOBAL_MENU,
    GLOBAL_TOGGLE_STATUS,
    GLOBAL_AGGRESSIVE_UNITS,

    // Camera
    CAMERA_PAN_LEFT,
    CAMERA_PAN_RIGHT,
    CAMERA_PAN_UP,
    CAMERA_PAN_DOWN,
    CAMERA_PITCH_UP,
    CAMERA_PITCH_DOWN,
    CAMERA_ROTATE_LEFT,
    CAMERA_ROTATE_RIGHT,
    CAMERA_ZOOM_IN,
    CAMERA_ZOOM_OUT,
    CAMERA_RESET,
    CAMERA_MAP_MODE,
    CAMERA_FIRST_PERSON,
    CAMERA_ZOOM_MODE,

    // UI
    UI_ACTIVATE,      // Space/Return
    UI_CANCEL,        // Escape
    UI_FOCUS_NEXT,    // Tab
    UI_FOCUS_PREV,    // Shift+Tab
    UI_NEXT_PANEL,    // Ctrl+Tab
    UI_PREV_PANEL,    // Shift+Ctrl+Tab
    UI_NAV_UP,
    UI_NAV_DOWN,
    UI_NAV_LEFT,
    UI_NAV_RIGHT,
    UI_NAV_HOME,
    UI_NAV_END,
    UI_NAV_PAGE_UP,
    UI_NAV_PAGE_DOWN,
    UI_BACKSPACE,
    UI_DELETE,

    // Gameplay
    UNIT_MOVE,        // M
    UNIT_ATTACK,      // A
    UNIT_GATHER,      // G
    UNIT_BUILD_QUARTERS, // Q
    UNIT_BUILD_ARMORY,   // R
    UNIT_BUILD_TOWER,    // T
    UNIT_EXIT_TOWER,     // X
    UNIT_BEACON,         // Ctrl+B
    UNIT_NEXT_IDLE,      // N
    UNIT_SET_RALLY,      // R
    GAMEPLAY_BACK,       // Backspace
    
    // Army Shortcuts
    ARMY_SELECT_0,
    ARMY_SELECT_1,
    ARMY_SELECT_2,
    ARMY_SELECT_3,
    ARMY_SELECT_4,
    ARMY_SELECT_5,
    ARMY_SELECT_6,
    ARMY_SELECT_7,
    ARMY_SELECT_8,
    ARMY_SELECT_9,
    
    ARMY_CREATE_0,
    ARMY_CREATE_1,
    ARMY_CREATE_2,
    ARMY_CREATE_3,
    ARMY_CREATE_4,
    ARMY_CREATE_5,
    ARMY_CREATE_6,
    ARMY_CREATE_7,
    ARMY_CREATE_8,
    ARMY_CREATE_9,

    // Production/Harvest (In Armory/Quarters submenus)
    PROD_WEAPONS,     // W
    PROD_HARVEST,     // G
    PROD_ARMY,        // A
    PROD_TRANSPORT,   // T
    
    // Resource Specific
    RES_TREE,         // W
    RES_ROCK,         // R
    RES_IRON,         // I
    RES_CHICKEN,      // C
    
    // Unit Specific
    TRAIN_PEON,       // P
    TRAIN_CHIEFTAIN,  // C
    
    // Magic
    MAGIC_1,          // S
    MAGIC_2,          // C
    
    // Misc
    GAME_SPEED_UP,    // +
    GAME_SPEED_DOWN,  // -
    NOTIFICATION_JUMP, // TAB
    
    // Cheats
    CHEAT_1,
    CHEAT_2,
    CHEAT_3,
    CHEAT_4,
    CHEAT_5,
    CHEAT_6,
    CHEAT_7,
    CHEAT_8,
    CHEAT_9,
    
    DEBUG_PRINT_INFO, // Ctrl+I
    DEBUG_KILL_SELECTED, // Ctrl+K
    DEBUG_TOGGLE_LIGHT, // L or O
    DEBUG_TOGGLE_PLANTS, // P
    DEBUG_TOGGLE_PARTICLES, // E
    DEBUG_TOGGLE_AXES, // A
    DEBUG_TOGGLE_MISC, // Ctrl+M
    DEBUG_PROCESS_MISC, // M
    DEBUG_RESET_CURSOR, // J
    DEBUG_TOGGLE_DETAIL, // S
    DEBUG_CRASH, // Ctrl+C
    DEBUG_TOGGLE_FRAME_BUFFER, // C
    DEBUG_TOGGLE_BOUNDING, // D
    DEBUG_TOGGLE_FRUSTUM_FREEZE, // V
    DEBUG_FORCE_GC, // F12
    DEBUG_START_RECORDING, // U
    DEBUG_TOGGLE_WATER, // Ctrl+W
    DEBUG_TOGGLE_AI, // Ctrl+R
    DEBUG_DUMP_ANIMATIONS // F1
}
