package com.oddlabs.tt.gui;

import com.codedisaster.steamworks.SteamAPI;
import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.Main;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.KeyboardInput;
import com.oddlabs.tt.render.Display;
import com.oddlabs.updater.UpdateInfo;
import com.oddlabs.util.Utils;

import org.lwjgl.system.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public final strictfp class LocalInput {
    public static final int LEFT_BUTTON = 0;
    public static final int RIGHT_BUTTON = 1;
    public static final int MIDDLE_BUTTON = 2;

    private static int mouse_x;
    private static int mouse_y;
    private static boolean global_menu_state = false;
    private static boolean global_control_state = false;
    private static boolean global_shift_state = false;
    private static final boolean[] keys = new boolean[256];

    private static boolean fullscreen;
    private static File game_dir;
    private static int revision;
    private static UpdateInfo update_info;

    private static LocalInput instance;

    public static final void setKeys(
            int key_code,
            boolean state,
            boolean shift_down,
            boolean control_down,
            boolean menu_down) {
        keys[key_code] = state;
        global_menu_state = menu_down;
        global_control_state = control_down;
        global_shift_state = shift_down;
    }

    public static final void keyTyped(GUIRoot gui_root, int key_code, char key_char) {
        gui_root.getInputState().keyTyped(key_code, key_char);
    }

    public static final void keyPressed(
            GUIRoot gui_root,
            int key_code,
            char key_char,
            boolean shift_down,
            boolean control_down,
            boolean menu_down,
            boolean repeat) {
        setKeys(key_code, true, shift_down, control_down, menu_down);
        gui_root.getInputState()
                .keyPressed(key_code, key_char, shift_down, control_down, menu_down, repeat);
    }

    public static final void keyReleased(
            GUIRoot gui_root,
            int key_code,
            char key_char,
            boolean shift_down,
            boolean control_down,
            boolean menu_down) {
        setKeys(key_code, false, shift_down, control_down, menu_down);
        gui_root.getInputState()
                .keyReleased(key_code, key_char, shift_down, control_down, menu_down);
    }

    public static final void mouseDragged(GUIRoot gui_root, int button, short x, short y) {
        setPos(x, y);
        gui_root.getInputState().mouseDragged(button, x, y);
    }

    public static final void mouseReleased(GUIRoot gui_root, int button) {
        gui_root.getInputState().mouseReleased(button);
    }

    public static final void mousePressed(GUIRoot gui_root, int button) {
        gui_root.getInputState().mousePressed(button);
    }

    public static final void mouseScrolled(GUIRoot gui_root, int dz) {
        gui_root.getInputState().mouseScrolled(dz);
    }

    public static final void mouseMoved(GUIRoot gui_root, short x, short y) {
        setPos(x, y);
        gui_root.getInputState().mouseMoved(x, y);
    }

    public static final boolean isShiftDownCurrently() {
        return global_shift_state;
    }

    public static final boolean isControlDownCurrently() {
        return global_control_state;
    }

    public static final boolean isMenuDownCurrently() {
        return global_menu_state;
    }

    public static final void resetKeys() {
        // Clear event queue
        KeyboardInput.reset();
        for (int i = 0; i < keys.length; i++) keys[i] = false;
    }

    public static final boolean isKeyDown(int key_code) {
        if (key_code >= keys.length) {
            System.out.println("Unsupported key " + key_code);
            return false;
        }
        return keys[key_code];
    }

    public static final void setPos(int x, int y) {
        mouse_x = x;
        mouse_y = y;
    }

    public static final void resetKeyboard() {
        resetKeys();
        global_menu_state = false;
        global_control_state = false;
        global_shift_state = false;
    }

    public static final int getMouseY() {
        return mouse_y;
    }

    public static final int getMouseX() {
        return mouse_x;
    }

    public static final boolean alIsCreated() {
        return LocalEventQueue.getQueue().getDeterministic().log(Display.isALCreated());
    }

    public static final File getGameDir() {
        // Figure out where the game is running from and use that as the game dir
        if (game_dir == null) {
            String platform_dir;
            if (Platform.get() == Platform.MACOSX) {
                platform_dir = "Library/Application Support" + File.separator;
            } else if (Platform.get() == Platform.LINUX) {
                platform_dir = ".";
            } else {
                platform_dir = "";
            }
            String game_dir_path =
                    System.getProperty("user.home")
                            + File.separator
                            + platform_dir
                            + Globals.GAME_NAME;

            // System.out.println("Running executable path: " + absolute_path);
            // In reality this is the save file path not the actual game dir...
            if (SteamAPI.isSteamRunning()) {
                try {
                    File jarFile =
                            new File(
                                    Main.class
                                            .getProtectionDomain()
                                            .getCodeSource()
                                            .getLocation()
                                            .toURI());
                    String exePath =
                            jarFile.getParentFile()
                                            .getParentFile()
                                            .getAbsolutePath()
                                    + File.separator
                                    + "save_data";
                    System.out.println("Save file path: " + exePath);
                    game_dir_path = exePath;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Game dir path: " + game_dir_path);
            game_dir = new File(game_dir_path);
        }
        return game_dir;
    }

    public static final UpdateInfo getUpdateInfo() {
        return update_info;
    }

    public static final int getRevision() {
        return revision;
    }

    public LocalInput() {
        assert instance == null;
        instance = this;
    }

    public static final int getViewWidth() {
        return Display.getWidth();
    }

    public static final int getViewHeight() {
        return Display.getHeight();
    }

    public static final boolean inFullscreen() {
        return fullscreen;
    }

    public static final LocalInput getLocalInput() {
        return instance;
    }

    public static final void settings(
            UpdateInfo update_info, File game_dir, File event_log_dir, Settings settings) {
        int revision;
        try {
            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    Utils.tryMakeURL("/revision_number").openStream()));
            String revision_string = in.readLine();
            in.close();
            revision = (new Integer(revision_string)).intValue();
        } catch (Exception e) {
            revision = -1;
        }
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        instance.setSettings(update_info, game_dir, event_log_dir, revision, settings);
    }

    public final void setSettings(
            UpdateInfo update_info,
            File game_dir,
            File event_log_dir,
            int revision,
            Settings settings) {
        System.out.println("revision = " + revision);
        System.out.println("Game dir: " + game_dir.getAbsolutePath());
        LocalInput.game_dir = game_dir;
        LocalInput.revision = revision;
        LocalInput.update_info = update_info;
        settings.last_event_log_dir = event_log_dir.getAbsolutePath();
        settings.last_revision = revision;
        settings.crashed = true;
        settings.save();
        settings.crashed = false;
        fullscreen = settings.fullscreen;
    }

    public static final void init() {
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        mouse_x = deterministic.log(com.oddlabs.tt.input.Mouse.getX());
        mouse_y = deterministic.log(com.oddlabs.tt.input.Mouse.getY());
    }

    public static final float getViewAspect() {
        return (float) getViewWidth() / (float) getViewHeight();
    }

    private static final float getUnitsPerPixel() {
        return (float)
                (Globals.VIEW_MIN
                        * StrictMath.tan(Globals.FOV * (StrictMath.PI / 180.0f) * 0.5d)
                        / (getViewHeight() * 0.5d));
    }

    public static final float getErrorConstant() {
        return Globals.VIEW_MIN / (getUnitsPerPixel() * Globals.ERROR_TOLERANCE);
    }
}
