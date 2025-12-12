package com.oddlabs.tt.gui;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.InputProvider;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.KeyboardInput;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SerializableDisplayMode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

public final class LocalInput {
    private static final Logger logger = Logger.getLogger(LocalInput.class.getName());

	private static int mouse_x;
	private static int mouse_y;
    
    private static InputProvider inputProvider;

	private static boolean global_menu_state = false;
	private static boolean global_control_state = false;
	private static boolean global_shift_state = false;
	private static final Set<Key> keys = EnumSet.noneOf(Key.class);

	private static int view_width;
	private static int view_height;
	private static boolean fullscreen;
	private static @Nullable Path game_dir;
	private static int revision;

	private static final LocalInput instance = new LocalInput();

	public static void setKeys(@NonNull Key key, boolean state, boolean shift_down, boolean control_down, boolean menu_down) {
		if (state)
			keys.add(key);
		else
			keys.remove(key);
		global_menu_state = menu_down;
		global_control_state = control_down;
		global_shift_state = shift_down;
	}

	public static void keyTyped(@NonNull GUIRoot gui_root, int key_code, char key_char) {
		gui_root.getInputState().keyTyped(key_code, key_char);
	}

	public static void keyPressed(@NonNull GUIRoot gui_root, int key_code, char key_char, boolean shift_down, boolean control_down, boolean menu_down, boolean repeat) {
		var key = Key.fromLwjglCode(key_code);
		if (Key.KEY_UNKNOWN != key) {
			setKeys(key, true, shift_down, control_down, menu_down);
			gui_root.getInputState().keyPressed(key, key_char, shift_down, control_down, menu_down, repeat);
		}
	}

	public static void keyReleased(@NonNull GUIRoot gui_root, int key_code, char key_char, boolean shift_down, boolean control_down, boolean menu_down) {
		var key = Key.fromLwjglCode(key_code);
		if (Key.KEY_UNKNOWN != key) {
			setKeys(key, false, shift_down, control_down, menu_down);
			gui_root.getInputState().keyReleased(key, key_char, shift_down, control_down, menu_down);
		}
	}

	public static void mouseDragged(@NonNull GUIRoot gui_root, @NonNull MouseButton button, short x, short y) {
		mouse_x = x;
		mouse_y = y;
        gui_root.getInputState().mouseDragged(button, x, y);
    }

	public static void mouseReleased(@NonNull GUIRoot gui_root, @NonNull MouseButton button) {
        gui_root.getInputState().mouseReleased(button);
    }

	public static void mousePressed(@NonNull GUIRoot gui_root, @NonNull MouseButton button) {
        gui_root.getInputState().mousePressed(button);
    }

	public static void mouseScrolled(@NonNull GUIRoot gui_root, int dz) {
		gui_root.getInputState().mouseScrolled(dz);
	}

	public static void mouseMoved(@NonNull GUIRoot gui_root, short x, short y) {
		mouse_x = x;
		mouse_y = y;
		gui_root.getInputState().mouseMoved(x, y);
	}

	public static boolean isShiftDownCurrently() {
		return global_shift_state;
	}

	public static boolean isControlDownCurrently() {
		return global_control_state;
	}

	public static boolean isMenuDownCurrently() {
		return global_menu_state;
	}

	public static void resetKeys() {
		// Clear event queue
		KeyboardInput.reset();
		keys.clear();
	}

	public static boolean isKeyDown(@NonNull Key key) {
		return keys.contains(key);
	}

	public static void resetKeyboard() {
		resetKeys();
		global_menu_state = false;
		global_control_state = false;
		global_shift_state = false;
	}

	public static int getMouseY() {
		return mouse_y;
	}

	public static int getMouseX() {
		return mouse_x;
	}

	public static boolean audioIsCreated() {
		return LocalEventQueue.getQueue().getDeterministic().log(AudioManager.getManager() != null);
	}

	public static @Nullable Path getGameDir() {
		return game_dir;
	}

	public static int getRevision() {
		return revision;
	}

	public static int getViewWidth() {
		return view_width;
	}

	public static int getViewHeight() {
		return view_height;
	}

	public static void setViewDimensions(int width, int height) {
		view_width = width;
		view_height = height;
	}

	public static boolean inFullscreen() {
		return fullscreen;
	}

	public static LocalInput getLocalInput() {
		return instance;
	}

	public static @NonNull SerializableDisplayMode @NonNull [] getAvailableModes() {
		try {
            return Renderer.getRenderer().getWindow().getAvailableDisplayModes();
		} catch (Exception e) {
			throw new IllegalStateException("Could not get available modes", e);
		}
	}

	public static @NonNull SerializableDisplayMode getCurrentMode() {
		return LocalEventQueue.getQueue().getDeterministic()
                .log(Renderer.getRenderer().getWindow().getDisplayMode());
	}

	public static int getNativeCursorCaps() {
		return LocalEventQueue.getQueue().getDeterministic()
                .log(Cursor.getCapabilities());
	}

	public static void settings(@NonNull Path game_dir, @NonNull Path event_log_dir, @NonNull Settings settings) {
		instance.setSettings(game_dir, event_log_dir, revision, settings);
	}

	public void setSettings(@NonNull Path game_dir, @NonNull Path event_log_dir, int revision, @NonNull Settings settings) {
		logger.config("revision = " + revision);
		LocalInput.game_dir = game_dir;
		LocalInput.revision = revision;
		settings.last_event_log_dir = event_log_dir.toAbsolutePath();
		settings.last_revision = revision;
		settings.crashed = true;
		settings.save();
		settings.crashed = false;

		fullscreen = settings.fullscreen;
	}

	public static void init() {
        inputProvider = new com.oddlabs.tt.input.LWJGL2InputProvider();
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		mouse_x = deterministic.log(inputProvider.getMouseX());
		mouse_y = deterministic.log(inputProvider.getMouseY());
	}
    
    public static com.oddlabs.tt.input.InputProvider getInputProvider() {
        return inputProvider;
    }

	private void modeSwitchedLater(@NonNull SerializableDisplayMode new_mode) {
		Settings.getSettings().fullscreen = fullscreen;
		Settings.getSettings().new_view_width = new_mode.getWidth();
		Settings.getSettings().new_view_height = new_mode.getHeight();
		Settings.getSettings().new_view_freq = new_mode.getFrequency();
	}

	private void modeSwitchedNow(@NonNull SerializableDisplayMode new_mode) {
		modeSwitchedLater(new_mode);
		modeSwitched();
	}

	private void modeSwitched() {
		SerializableDisplayMode new_mode = LocalEventQueue.getQueue().getDeterministic().log(Renderer.getRenderer().getWindow().getDisplayMode());
		view_width = new_mode.getWidth();
		view_height = new_mode.getHeight();
		logger.info("Switched mode to " + new_mode);
		Settings.getSettings().view_width = new_mode.getWidth();
		Settings.getSettings().view_height = new_mode.getHeight();
		Settings.getSettings().view_freq = new_mode.getFrequency();
	}

	public void fullscreenToggled(boolean fullscreen, boolean switch_now) {
		Settings.getSettings().fullscreen = fullscreen;
		if (switch_now && LocalInput.fullscreen != fullscreen) {
			toggleFullscreen();
			logger.info("Fullscreen toggled");
		}
	}

	public static void toggleFullscreen() {
		fullscreen = !fullscreen;
		try {
            boolean fs = fullscreen && !LocalEventQueue.getQueue().getDeterministic().isPlayback();
            Renderer.getRenderer().getWindow().setFullscreen(fs);
			Renderer.resetInput();
		} catch (Exception e) {
			logger.log(java.util.logging.Level.SEVERE, "Mode switching failed with exception", e);
			throw new RuntimeException("Mode switching failed");
		}
	}

	public void switchMode(@NonNull SerializableDisplayMode mode, boolean switch_now) {
		if (switch_now) {
            try {
                Renderer.getRenderer().getWindow().setDisplayMode(mode);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
			modeSwitchedNow(mode);
		} else
			modeSwitchedLater(mode);
	}

	public void setModeToNearest(@NonNull SerializableDisplayMode mode) throws Exception {
        // Use window create to ensure window is created/resized
        boolean fs = Settings.getSettings().fullscreen;
        Renderer.getRenderer().getWindow().create(mode, fs);
		modeSwitchedNow(mode);
	}

	public static float getViewAspect() {
		return (float)view_width/view_height;
	}

	private static float getUnitsPerPixel() {
		return (float)(Globals.VIEW_MIN*Math.tan(Globals.FOV*(Math.PI/180.0f)*0.5d)/(view_height*0.5d));
	}

	public static float getErrorConstant() {
		return Globals.VIEW_MIN/(getUnitsPerPixel()*Globals.ERROR_TOLERANCE);
	}
}