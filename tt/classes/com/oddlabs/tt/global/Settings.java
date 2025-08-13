package com.oddlabs.tt.global;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.GLUtils;

import org.lwjgl.opengl.GL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Properties;

public final strictfp class Settings implements Serializable {
    private static final long serialVersionUID = 1l;

    private static Settings settings;

    // event logging
    public String last_event_log_dir = "";
    public int last_revision = -1;
    public boolean crashed = false;

    // network
    // TODO: Why is domain name stuck on tribaltrouble.org?
    // when it is loaded from the settings file?
    private String domain_name = "tribaltrouble.org";
    public String username = "";
    public String pw_digest = "";
    public boolean remember_login = false;

    public int graphic_detail = Globals.DETAIL_NORMAL;

    // sound
    public boolean play_music = true;
    public boolean play_sfx = true;
    public float music_gain = .5f;
    public float sound_gain = 1f;

    // language
    public String language = "default";

    // window
    public int view_width = 800;
    public int view_height = 600;
    public int view_freq = 75;

    public int new_view_width = view_width;
    public int new_view_height = view_height;
    public int new_view_freq = view_freq;

    public boolean fullscreen = true;
    public boolean vsync = true;
    //	public int view_bpp = 32;
    public int samples = 0;

    // control
    public boolean invert_camera_pitch = false;
    public boolean aggressive_units = false;

    public float mapmode_delay = .5f;
    public float tooltip_delay = .5f;
    private String developer_mode = "";
    private String beta_mode = "";
    public boolean has_native_campaign = false;

    public boolean save_event_log = true;
    public boolean fullscreen_depth_workaround = true;
    public boolean generate_dummy_worlds = false;
    public boolean first_run = true;

    public boolean warning_no_sound = true;

    // affiliate
    public String affiliate_id = "oddlabs.com";
    public String affiliate_logo_file = "";
    public String buy_url = "http://tribaltrouble.com/order";

    // reg key
    public boolean online = true;
    public String reg_key = "";

    // portal stuff
    public boolean hide_update = false;
    public boolean hide_register = false;
    public boolean hide_multiplayer = false;
    public boolean hide_bugreporter = false;
    public boolean hide_regkey = false;
    public boolean buy_now_only_quit = false;

    /* optional extensions */
    public boolean use_vbo_draw_range_elements = false;
    private boolean use_vbo = false;
    private boolean use_pbuffer = false;
    private boolean use_fbo = true;
    public boolean use_copyteximage = false;
    private boolean use_texture_compression = true;

    public float mouse_sensitivity = 1.0f;

    public int frame_grab_milliseconds_per_frame = 40;

    public static final void setSettings(Settings new_settings) {
        settings = new_settings;
        settings.setDomain(settings.getDomainName());
    }

    public static final Settings getSettings() {
        return settings;
    }

    /**
     * The default keybindings setup with Action Name -> Tribal Trouble Key Code. See Globals.KB_*
     * constants for action names.
     */
    private static final HashMap<String, Integer> default_keybinds =
            new HashMap<String, Integer>() {
                {
                    // Camera controls
                    put(Globals.KB_PAN_CAMERA_LEFT, Keyboard.KEY_LEFT);
                    put(Globals.KB_PAN_CAMERA_RIGHT, Keyboard.KEY_RIGHT);
                    put(Globals.KB_PAN_CAMERA_UP, Keyboard.KEY_UP);
                    put(Globals.KB_PAN_CAMERA_DOWN, Keyboard.KEY_DOWN);

                    // Unit actions
                    put(Globals.KB_MOVE, Keyboard.KEY_M);
                    put(Globals.KB_ATTACK, Keyboard.KEY_A);
                    put(Globals.KB_GATHER_REPAIR, Keyboard.KEY_G);

                    // Building actions
                    put(Globals.KB_BUILD_QUARTERS, Keyboard.KEY_Q);
                    put(Globals.KB_BUILD_ARMORY, Keyboard.KEY_R);
                    put(Globals.KB_BUILD_TOWER, Keyboard.KEY_T);

                    // Quarters actions
                    put(Globals.KB_QUARTERS_CHIEFTAIN, Keyboard.KEY_C);
                    put(Globals.KB_QUARTERS_DEPLOY_PEON, Keyboard.KEY_P);
                    put(Globals.KB_QUARTERS_SET_RALLY_POINT, Keyboard.KEY_R);

                    // Armory main actions
                    put(Globals.KB_ARMORY_DEPLOY_WARRIORS, Keyboard.KEY_A);
                    put(Globals.KB_ARMORY_HARVEST, Keyboard.KEY_G);
                    put(Globals.KB_ARMORY_MAKE_WEAPONS, Keyboard.KEY_W);
                    put(Globals.KB_ARMORY_TRANSPORT, Keyboard.KEY_T);
                    put(Globals.KB_ARMORY_RALLY_POINT, Keyboard.KEY_R);

                    // Armory harvest actions
                    put(Globals.KB_ARMORY_HARVEST_TREE, Keyboard.KEY_W);
                    put(Globals.KB_ARMORY_HARVEST_ROCK, Keyboard.KEY_R);
                    put(Globals.KB_ARMORY_HARVEST_IRON, Keyboard.KEY_I);
                    put(Globals.KB_ARMORY_HARVEST_CHICKEN, Keyboard.KEY_C);

                    // Armory weapon creation actions
                    put(Globals.KB_ARMORY_CREATE_ROCK_WEAPON, Keyboard.KEY_R);
                    put(Globals.KB_ARMORY_CREATE_IRON_WEAPON, Keyboard.KEY_I);
                    put(Globals.KB_ARMORY_CREATE_CHICKEN_WEAPON, Keyboard.KEY_C);

                    // Armory army deployment actions
                    put(Globals.KB_ARMORY_DEPLOY_PEON, Keyboard.KEY_P);
                    put(Globals.KB_ARMORY_DEPLOY_ROCK_WARRIORS, Keyboard.KEY_R);
                    put(Globals.KB_ARMORY_DEPLOY_IRON_WARRIORS, Keyboard.KEY_I);
                    put(Globals.KB_ARMORY_DEPLOY_CHICKEN_WARRIORS, Keyboard.KEY_C);

                    // Armory transport actions
                    put(Globals.KB_ARMORY_TRANSPORT_TREE, Keyboard.KEY_W);
                    put(Globals.KB_ARMORY_TRANSPORT_ROCK, Keyboard.KEY_R);
                    put(Globals.KB_ARMORY_TRANSPORT_IRON, Keyboard.KEY_I);
                    put(Globals.KB_ARMORY_TRANSPORT_CHICKEN, Keyboard.KEY_C);

                    // Tower actions
                    put(Globals.KB_TOWER_ATTACK, Keyboard.KEY_A);
                    put(Globals.KB_TOWER_EXIT, Keyboard.KEY_X);

                    // Magic actions
                    put(Globals.KB_CHIEFTAIN_MAGIC1, Keyboard.KEY_S);
                    put(Globals.KB_CHIEFTAIN_MAGIC2, Keyboard.KEY_C);
                }
            };

    /**
     * The current keybindings for the client running the game. Used as Action Name -> Tribal
     * Trouble Key Code. See Globals.KB_* constants for action names.
     */
    private static HashMap<String, Integer> keybinds =
            new HashMap<String, Integer>(default_keybinds);

    /**
     * Gets the stored keybind for the specified action. Use Globals.KB_* constants for action
     * names.
     *
     * @param action_name
     * @return
     */
    public Integer getKeybind(String action_name) {
        return keybinds.get(action_name);
    }

    /**
     * Sets a tribal trouble key code to the specified action.
     *
     * @param action_name
     * @param key_code
     */
    public void setKeybind(String action_name, int key_code) {
        System.err.println(
                "Setting keybind for action: " + action_name + " to key code: " + key_code);
        keybinds.put(action_name, key_code);
    }

    /**
     * Gets the hashmap of keybinds
     *
     * @return
     */
    public HashMap<String, Integer> getKeybinds() {
        return keybinds;
    }

    /**
     * Gets the keybind for the specified action as a string for display in tooltips. Use
     * Globals.KB_* constants for action names.
     *
     * @param action_name
     * @return
     */
    public String getKeybindString(String action_name) {
        Integer keyCode = keybinds.get(action_name);
        if (keyCode == null) {
            return "?";
        }
        return Keyboard.keyToString(keyCode);
    }

    public final boolean useFBO() {
        return use_fbo
                && GL.getCapabilities().GL_EXT_framebuffer_object
                && !GLUtils.isIntelGMA950();
    }

    public final boolean usePbuffer() {
        return false;
    }

    public final boolean useTextureCompression() {
        return use_texture_compression
                && (GL.getCapabilities().GL_ARB_texture_compression
                        || GL.getCapabilities().OpenGL13);
    }

    public final boolean useVBO() {
        return use_vbo && GL.getCapabilities().GL_ARB_vertex_buffer_object;
    }

    public final boolean inDeveloperMode() {
        return developer_mode.equals("randomgryf") && Renderer.isRegistered();
    }

    public final boolean inBetaMode() {
        return beta_mode.equals("mythol");
    }

    public final void save() {
        if (LocalEventQueue.getQueue().getDeterministic().isPlayback()) return;
        Settings original_settings = new Settings();
        Properties props = new Properties();
        Field[] pref_fields = Settings.class.getDeclaredFields();
        for (int i = 0; i < pref_fields.length; i++) {
            Field field = pref_fields[i];
            int mods = field.getModifiers();
            if (!hasValidModifiers(mods)) continue;
            assert !Modifier.isStatic(mods);
            Class field_type = field.getType();
            try {
                if (field_type.equals(boolean.class)) {
                    boolean field_value = field.getBoolean(this);
                    if (field_value != field.getBoolean(original_settings))
                        props.setProperty(field.getName(), "" + field_value);
                } else if (field_type.equals(int.class)) {
                    int field_value = field.getInt(this);
                    if (field_value != field.getInt(original_settings))
                        props.setProperty(field.getName(), "" + field_value);
                } else if (field_type.equals(float.class)) {
                    float field_value = field.getFloat(this);
                    if (field_value != field.getFloat(original_settings))
                        props.setProperty(field.getName(), "" + field_value);
                } else if (field_type.equals(String.class)) {
                    String field_value = (String) field.get(this);
                    if (!field_value.equals(field.get(original_settings)))
                        props.setProperty(field.getName(), "" + field_value);
                } else if (field_type.equals(HashMap.class)) {
                    // skip - handled below
                } else throw new RuntimeException("Unsupported Settings type " + field_type);
            } catch (IllegalAccessException e) {
                System.out.println("Exception: " + e);
                throw new RuntimeException(e);
            }
        }

        // Save keybinds HashMap
        saveKeybinds(props);

        File settings_file = new File(LocalInput.getGameDir(), Globals.SETTINGS_FILE_NAME);
        try {
            OutputStream out = new FileOutputStream(settings_file);
            props.store(out, "comment");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            System.err.println("Failed to write settings to " + settings_file + " exception: " + e);
        }
    }

    /** Save keybinds to properties with a prefix to avoid naming conflicts */
    private void saveKeybinds(Properties props) {
        HashMap<String, Integer> original_keybinds = Settings.default_keybinds;
        for (String action : keybinds.keySet()) {
            Integer keyCode = keybinds.get(action);
            Integer originalKeyCode = original_keybinds.get(action);

            // Only save if different from default
            if (keyCode != null && !keyCode.equals(originalKeyCode)) {
                props.setProperty("keybind." + action, keyCode.toString());
            }
        }
    }

    /** Updates settings related to the domain name for the client */
    public void setDomain(String new_domain) {
        domain_name = new_domain;
    }

    public String getDomainName() {
        return domain_name;
    }

    public String getRegistrationAddress() {
        return "registration." + domain_name;
    }

    public String getMatchmakingAddress() {
        return "matchmaking." + domain_name;
    }

    public String getBugReportAddress() {
        return "bugreport." + domain_name;
    }

    public String getRouterAddress() {
        return "router." + domain_name;
    }

    public final void load(File game_dir) {
        System.out.println("Loading settings from " + game_dir);
        Field[] pref_fields = getClass().getDeclaredFields();
        Properties props = new Properties();
        File settings_file = new File(game_dir, Globals.SETTINGS_FILE_NAME);
        try {
            InputStream in = new FileInputStream(settings_file);
            props.load(in);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            System.err.println("Could not read settings from " + settings_file);
            return;
        }

        for (int i = 0; i < pref_fields.length; i++) {
            Field field = pref_fields[i];
            int mods = field.getModifiers();
            if (!hasValidModifiers(mods)) continue;
            assert !Modifier.isStatic(mods);
            String value = props.getProperty(field.getName());
            if (value == null) continue;

            Class field_type = field.getType();
            try {
                if (field_type.equals(boolean.class)) {
                    boolean field_value = (new Boolean(value)).booleanValue();
                    field.setBoolean(this, field_value);
                } else if (field_type.equals(int.class)) {
                    int field_value = (new Integer(value)).intValue();
                    field.setInt(this, field_value);
                } else if (field_type.equals(float.class)) {
                    System.out.println("Loading float setting " + field.getName() + " = " + value);
                    float field_value = (new Float(value)).floatValue();
                    field.setFloat(this, field_value);
                } else if (field_type.equals(String.class)) {
                    field.set(this, value);
                } else if (field_type.equals(HashMap.class)) {
                    // skip - handled below
                } else throw new RuntimeException("Unsupported Settings type " + field_type);
            } catch (Exception e) {
                System.out.println("Exception: " + e);
                System.out.println(
                        "WARNING: "
                                + field.getName()
                                + " is not of type: "
                                + field.getType()
                                + ". Skipped");
            }
        }

        // Load keybinds
        loadKeybinds(props);
    }

    /** Load keybinds from properties with the "keybind." prefix */
    private void loadKeybinds(Properties props) {
        // Start with a copy of default keybinds
        keybinds = new HashMap<String, Integer>(default_keybinds);

        // Override with saved values
        for (String propertyName : props.stringPropertyNames()) {
            if (propertyName.startsWith("keybind.")) {
                String action = propertyName.substring("keybind.".length());
                String valueStr = props.getProperty(propertyName);
                try {
                    Integer keyCode = Integer.valueOf(valueStr);
                    if (default_keybinds.containsKey(action)) {
                        keybinds.put(action, keyCode);
                    }
                } catch (NumberFormatException e) {
                    System.err.println(
                            "Failed to parse keybind value for " + action + ": " + valueStr);
                }
            }
        }
    }

    private static final boolean hasValidModifiers(int mods) {
        return !Modifier.isStatic(mods) && !Modifier.isFinal(mods);
    }
}
