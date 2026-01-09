package com.oddlabs.tt.gui;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.InputProvider;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.KeyboardInput;
import com.oddlabs.tt.input.LWJGL3InputProvider;
import com.oddlabs.tt.window.LWJGL3Window;
import com.oddlabs.tt.window.Window;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

public final class LocalInput implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(LocalInput.class.getName());

    public static final int CURSOR_ONE_BIT_TRANSPARENCY = 1;
    public static final int CURSOR_8_BIT_ALPHA = 2;

    private int mouse_x;
    private int mouse_y;

    private final @NonNull InputProvider<?> inputProvider;

    private final Set<@NonNull Key> keys = EnumSet.noneOf(Key.class);
    private boolean global_super_state = false;
    private boolean global_alt_state = false;
    private boolean global_control_state = false;
    private boolean global_shift_state = false;

    private @Nullable Path game_dir;
    private int revision;

    public LocalInput(@NonNull Window lwjglWindow) {
        if (lwjglWindow instanceof LWJGL3Window win) {
            inputProvider = new LWJGL3InputProvider(win);
        } else {
            throw new IllegalStateException("Window is not LWJGL3Window");
        }
    }

    public void setKeys(@NonNull Key key, boolean state, boolean shift_down, boolean control_down, boolean alt_down, boolean super_down) {
        if (state)
            keys.add(key);
        else
            keys.remove(key);
        global_alt_state = alt_down;
        global_super_state = super_down;
        global_control_state = control_down;
        global_shift_state = shift_down;
    }

    public static void keyTyped(@NonNull GUIRoot gui_root, int key_code, char key_char) {
        gui_root.getInputState().keyTyped(key_code, key_char);
    }

    public void keyPressed(@NonNull GUIRoot gui_root, int key_code, char key_char, boolean shift_down, boolean control_down, boolean alt_down, boolean super_down, boolean repeat) {
        var key = Key.fromLwjglCode(key_code);
        if (Key.KEY_UNKNOWN != key || key_char != 0) {
            setKeys(key, true, shift_down, control_down, alt_down, super_down);
            gui_root.getInputState().keyPressed(key, key_char, shift_down, control_down, alt_down, super_down, repeat);
        }
    }

    public void keyReleased(@NonNull GUIRoot gui_root, int key_code, char key_char, boolean shift_down, boolean control_down, boolean alt_down, boolean super_down) {
        var key = Key.fromLwjglCode(key_code);
        if (Key.KEY_UNKNOWN != key || key_char != 0) {
            setKeys(key, false, shift_down, control_down, alt_down, super_down);
            gui_root.getInputState().keyReleased(key, key_char, shift_down, control_down, alt_down, super_down);
        }
    }

    public void mouseDragged(@NonNull GUIRoot gui_root, @NonNull MouseButton button, short x, short y) {
        mouse_x = x;
        mouse_y = y;
        gui_root.getInputState().mouseDragged(button, x, y);
    }

    public void mouseReleased(@NonNull GUIRoot gui_root, @NonNull MouseButton button) {
        gui_root.getInputState().mouseReleased(button);
    }

    public void mousePressed(@NonNull GUIRoot gui_root, @NonNull MouseButton button) {
        gui_root.getInputState().mousePressed(button);
    }

    public void mouseScrolled(@NonNull GUIRoot gui_root, int dz) {
        gui_root.getInputState().mouseScrolled(dz);
    }

    public void mouseMoved(@NonNull GUIRoot gui_root, short x, short y) {
        mouse_x = x;
        mouse_y = y;
        gui_root.getInputState().mouseMoved(x, y);
    }

    public boolean isShiftDownCurrently() {
        return global_shift_state;
    }

    public boolean isControlDownCurrently() {
        return global_control_state;
    }

    public boolean isAltDownCurrently() {
        return global_alt_state;
    }

    public boolean isSuperDownCurrently() {
        return global_super_state;
    }

    public void resetKeys() {
        // Clear event queue
        KeyboardInput.reset();
        keys.clear();
    }

    public boolean isKeyDown(@NonNull Key key) {
        return keys.contains(key);
    }

    public void resetKeyboard() {
        resetKeys();
        global_alt_state = false;
        global_control_state = false;
        global_shift_state = false;
        global_super_state = false;
    }

    public int getMouseY() {
        return mouse_y;
    }

    public int getMouseX() {
        return mouse_x;
    }

    public boolean audioIsCreated() {
        return LocalEventQueue.getQueue().getDeterministic().log(AudioManager.getManager() != null);
    }

    public @Nullable Path getGameDir() {
        return game_dir;
    }

    public int getRevision() {
        return revision;
    }

    public int getNativeCursorCaps() {
        return LocalEventQueue.getQueue().getDeterministic()
                .log(CURSOR_8_BIT_ALPHA | CURSOR_ONE_BIT_TRANSPARENCY);
    }

    public void settings(@NonNull Path game_dir, @NonNull Path event_log_dir, @NonNull Settings settings) {
        setSettings(game_dir, event_log_dir, revision, settings);
    }

    public void setSettings(@NonNull Path game_dir, @NonNull Path event_log_dir, int revision, @NonNull Settings settings) {
        logger.config("revision = " + revision);
        this.game_dir = game_dir;
        this.revision = revision;
        settings.last_event_log_dir = event_log_dir.toAbsolutePath();
        settings.last_revision = revision;
        settings.crashed = true;
        settings.save();
        settings.crashed = false;
    }

    public void init() {
        if (inputProvider instanceof LWJGL3InputProvider lwjgl3InputProvider) {
            lwjgl3InputProvider.initCallbacks();
        }
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        mouse_x = deterministic.log(inputProvider.getMouseX());
        mouse_y = deterministic.log(inputProvider.getMouseY());
    }

    public void close() {
        inputProvider.close();
    }

    public <T> @NonNull InputProvider<T> getInputProvider() {
        //noinspection unchecked
        return (InputProvider<T>) inputProvider;
    }
}