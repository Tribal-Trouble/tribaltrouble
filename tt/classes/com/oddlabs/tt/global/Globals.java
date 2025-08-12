package com.oddlabs.tt.global;

import org.lwjgl.opengl.*;

public final strictfp class Globals {
    // #region Keybinds - Actions

    // Basic Unit Actions
    public static final String KB_ATTACK = "KB_ATTACK";
    public static final String KB_GATHER_REPAIR = "KB_GATHER_REPAIR";
    public static final String KB_MOVE = "KB_MOVE";

    // Building Construction (Peon Actions)
    public static final String KB_BUILD_ARMORY = "KB_BUILD_ARMORY";
    public static final String KB_BUILD_QUARTERS = "KB_BUILD_QUARTERS";
    public static final String KB_BUILD_TOWER = "KB_BUILD_TOWER";

    // Armory Actions
    public static final String KB_ARMORY_DEPLOY_WARRIORS = "KB_ARMORY_DEPLOY_WARRIORS";
    public static final String KB_ARMORY_HARVEST = "KB_ARMORY_HARVEST";
    public static final String KB_ARMORY_MAKE_WEAPONS = "KB_ARMORY_MAKE_WEAPONS";
    public static final String KB_ARMORY_RALLY_POINT = "KB_ARMORY_RALLY_POINT";
    public static final String KB_ARMORY_TRANSPORT = "KB_ARMORY_TRANSPORT";

    // Armory - Deploy Units
    public static final String KB_ARMORY_DEPLOY_CHICKEN_WARRIORS =
            "KB_ARMORY_DEPLOY_CHICKEN_WARRIORS";
    public static final String KB_ARMORY_DEPLOY_IRON_WARRIORS = "KB_ARMORY_DEPLOY_IRON_WARRIORS";
    public static final String KB_ARMORY_DEPLOY_PEON = "KB_ARMORY_DEPLOY_PEON";
    public static final String KB_ARMORY_DEPLOY_ROCK_WARRIORS = "KB_ARMORY_DEPLOY_ROCK_WARRIORS";

    // Armory - Resource Harvesting
    public static final String KB_ARMORY_HARVEST_CHICKEN = "KB_ARMORY_HARVEST_CHICKEN";
    public static final String KB_ARMORY_HARVEST_IRON = "KB_ARMORY_HARVEST_IRON";
    public static final String KB_ARMORY_HARVEST_ROCK = "KB_ARMORY_HARVEST_ROCK";
    public static final String KB_ARMORY_HARVEST_TREE = "KB_ARMORY_HARVEST_TREE";

    // Armory - Resource Transportation
    public static final String KB_ARMORY_TRANSPORT_CHICKEN = "KB_ARMORY_TRANSPORT_CHICKEN";
    public static final String KB_ARMORY_TRANSPORT_IRON = "KB_ARMORY_TRANSPORT_IRON";
    public static final String KB_ARMORY_TRANSPORT_ROCK = "KB_ARMORY_TRANSPORT_ROCK";
    public static final String KB_ARMORY_TRANSPORT_TREE = "KB_ARMORY_TRANSPORT_TREE";

    // Armory - Weapon Creation
    public static final String KB_ARMORY_CREATE_CHICKEN_WEAPON = "KB_ARMORY_CREATE_CHICKEN_WEAPON";
    public static final String KB_ARMORY_CREATE_IRON_WEAPON = "KB_ARMORY_CREATE_IRON_WEAPON";
    public static final String KB_ARMORY_CREATE_ROCK_WEAPON = "KB_ARMORY_CREATE_ROCK_WEAPON";

    // Quarters Actions
    public static final String KB_QUARTERS_CHIEFTAIN = "KB_QUARTERS_CHIEFTAIN";
    public static final String KB_QUARTERS_DEPLOY_PEON = "KB_QUARTERS_DEPLOY_PEON";
    public static final String KB_QUARTERS_SET_RALLY_POINT = "KB_QUARTERS_SET_RALLY_POINT";

    // Chieftain Magic
    public static final String KB_CHIEFTAIN_MAGIC1 = "KB_CHIEFTAIN_MAGIC1";
    public static final String KB_CHIEFTAIN_MAGIC2 = "KB_CHIEFTAIN_MAGIC2";

    // Tower Actions
    public static final String KB_TOWER_ATTACK = "KB_TOWER_ATTACK";
    public static final String KB_TOWER_EXIT = "KB_TOWER_EXIT";

    // #endregion Keybinds - Actions
    public static final String DOMAIN_NAME = Settings.getSettings().getDomainName();
    public static final String SUPPORT_ADDRESS = "http://" + DOMAIN_NAME + "/support";

    public static final int BOUNDING_NONE = 0;
    public static final int BOUNDING_UNIT_GRID = 1;
    public static final int BOUNDING_LANDSCAPE = 2;
    public static final int BOUNDING_TREES = 3;
    public static final int BOUNDING_PLAYERS = 4;
    public static final int BOUNDING_OCCUPATION = 5;
    public static final int BOUNDING_REGIONS = 6;
    public static final int BOUNDING_ALL = 7;

    public static final int DETAIL_LOW = 0;
    public static final int DETAIL_NORMAL = 1;
    public static final int DETAIL_HIGH = 2;

    public static final int[] TEXTURE_MIP_SHIFT = new int[] {1, 0, 0};
    public static final int[] UNIT_HIGH_POLY_COUNT = new int[] {7500, 20000, 40000};
    public static final int[] LANDSCAPE_POLY_COUNT = new int[] {5000, 10000, 20000};
    public static final boolean[] INSERT_PLANTS = new boolean[] {false, false, true};

    public static final String GAME_NAME = "TribalTrouble";
    public static final String REG_FILE_NAME = "registration";
    public static final String SETTINGS_FILE_NAME = "settings";

    public static final String AFFILIATE_ID_KEY = "affiliate_id";

    public static boolean run_ai = true;

    public static int gamespeed = 2;

    public static boolean process_landscape = true;
    public static boolean process_trees = true;
    public static boolean process_misc = true;
    public static boolean process_shadows = true;

    public static boolean draw_status = false;
    public static boolean draw_landscape = true;
    public static boolean draw_trees = true;
    public static boolean draw_misc = true;
    public static boolean draw_particles = true;
    public static boolean draw_water = true;
    public static boolean draw_sky = true;
    public static boolean draw_axes = false;
    public static boolean draw_detail = true;
    public static boolean draw_shadows = true;
    public static boolean draw_light = true;
    public static boolean draw_plants = true;

    public static boolean line_mode = false;
    public static boolean clear_frame_buffer = false;
    public static boolean frustum_freeze = false;

    public static boolean slowmotion = false;

    public static boolean checksum_error_in_last_game = false;

    private static int bounding = BOUNDING_NONE;

    public static final void switchBoundingMode() {
        bounding = (bounding + 1) % (BOUNDING_ALL + 1);
    }

    public static final boolean isBoundsEnabled(int bounding_type) {
        return bounding == bounding_type || bounding == BOUNDING_ALL;
    }

    public static int COMPRESSED_RGB_FORMAT = GL13.GL_COMPRESSED_RGB;
    public static int COMPRESSED_RGBA_FORMAT = GL13.GL_COMPRESSED_RGBA;
    public static int COMPRESSED_A_FORMAT = GL13.GL_COMPRESSED_ALPHA;
    /*
     * public static int COMPRESSED_LUMINANCE_FORMAT = GL13.GL_COMPRESSED_LUMINANCE;
     */

    /*
     * public static int COMPRESSED_RGB_FORMAT = GL11.GL_RGB;
     * public static int COMPRESSED_RGBA_FORMAT = GL11.GL_RGBA;
     * public static int COMPRESSED_A_FORMAT = GL11.GL_ALPHA8;
     */
    public static int COMPRESSED_LUMINANCE_FORMAT = GL11.GL_LUMINANCE;
    public static int LOW_DETAIL_TEXTURE_SHIFT = 1;

    public static final void disableTextureCompression() {
        System.out.println("Disabling texture compression");
        COMPRESSED_RGB_FORMAT = GL11.GL_RGB;
        COMPRESSED_RGBA_FORMAT = GL11.GL_RGBA;
        COMPRESSED_A_FORMAT = GL11.GL_ALPHA;
        COMPRESSED_LUMINANCE_FORMAT = GL11.GL_LUMINANCE;
    }

    public static final float LANDSCAPE_HILLS = 1f;
    public static final float LANDSCAPE_VEGETATION = 2f;
    public static final float LANDSCAPE_RESOURCES = 0f;
    public static final int LANDSCAPE_SEED = 1;

    public static final int VIEW_BIT_DEPTH = 16;
    public static final float FOV = 45.0f;
    public static final float VIEW_MIN = 2f;
    public static final float VIEW_MAX = 8000.0f;
    public static final float GUI_Z = VIEW_MIN + 0.1f;

    public static final int NET_PORT = 21000;

    public static final int NO_MIPMAP_CUTOFF = 1000;

    public static final int STRUCTURE_SIZE = 256;
    public static final int DETAIL_SIZE = 256;
    public static final int TEXELS_PER_GRID_UNIT = 8;

    public static final float LANDSCAPE_DETAIL_REPEAT_RATE = 0.25f;
    public static final float WATER_REPEAT_RATE = 0.001f;
    public static final float WATER_DETAIL_REPEAT_RATE = 0.01f;
    public static final int LANDSCAPE_DETAIL_FADEOUT_BASE_LEVEL = 2;
    public static final float LANDSCAPE_DETAIL_FADEOUT_FACTOR = 0.75f;

    public static final int MAX_RENDERNODE_DEPTH = 5;

    public static final String SCREENSHOT_DEFAULT = "screenshot";

    public static final float[][] SEA_BOTTOM_COLOR = {{0.45f, 0.25f, 0.6f}, {0f, 0f, 0f}};

    public static final float TREE_ERROR_DISTANCE = 100f;

    public static final float WHEEL_SCALE = 0.01f;

    public static final int CURSOR_BLINK_TIME = 1000;

    public static final int MENU_HOVER_DELAY = 500;

    public static final int FPS_WIDTH = 800;

    public static final int SHELL_HISTORY_SIZE = 50;
    public static final int SHELL_HISTORY_PAGE_SIZE = 10;

    // max texture size (for generated textures)
    public static final int MIN_TEXTURE_POWER = 2;
    public static final int MIN_TEXTURE_SIZE = 1 << MIN_TEXTURE_POWER;
    public static int MAX_TEXTURE_POWER;
    public static int MAX_TEXTURE_SIZE;
    // How to divide images in 2^n textures - 1 means split most memory preserving 0
    // means split
    // least
    public static final float TEXTURE_WEIGHT = 0.5f;
    public static int[] TEXTURE_SIZES;
    public static byte[] TEXTURE_SPLITS;
    public static int[] BEST_SIZES;

    public static final float SEA_LEVEL = .1f;
    public static final int TEXELS_PER_CHUNK_BORDER = 4;

    public static final int BLOCK_SCROLL_AMOUNT = 20;

    public static final float ERROR_TOLERANCE = 10f;
    // #region Keybinds
    public static final String KB_PAN_CAMERA_LEFT = "KB_PAN_CAMERA_LEFT";
    public static final String KB_PAN_CAMERA_RIGHT = "KB_PAN_CAMERA_RIGHT";
    public static final String KB_PAN_CAMERA_UP = "KB_PAN_CAMERA_UP";
    public static final String KB_PAN_CAMERA_DOWN = "KB_PAN_CAMERA_DOWN";
    // #endregion Keybinds
}
