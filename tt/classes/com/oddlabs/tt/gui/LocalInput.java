package com.oddlabs.tt.gui;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.KeyboardInput;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SerializableDisplayMode;
import com.oddlabs.tt.render.SerializableDisplayModeComparator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public final class LocalInput {
	public final static int LEFT_BUTTON = 0;
	public final static int RIGHT_BUTTON = 1;
	public final static int MIDDLE_BUTTON = 2;

	private static int mouse_x;
	private static int mouse_y;
	private static boolean global_menu_state = false;
	private static boolean global_control_state = false;
	private static boolean global_shift_state = false;
	private final static boolean[] keys = new boolean[256];

	private static int view_width;
	private static int view_height;
	private static boolean fullscreen;
	private static Path game_dir;
	private static int revision;

	private final static LocalInput instance = new LocalInput();

	public static void setKeys(int key_code, boolean state, boolean shift_down, boolean control_down, boolean menu_down) {
		keys[key_code] = state;
		global_menu_state = menu_down;
		global_control_state = control_down;
		global_shift_state = shift_down;
	}

	public static void keyTyped(GUIRoot gui_root, int key_code, char key_char) {
		gui_root.getInputState().keyTyped(key_code, key_char);
	}

	public static void keyPressed(GUIRoot gui_root, int key_code, char key_char, boolean shift_down, boolean control_down, boolean menu_down, boolean repeat) {
		setKeys(key_code, true, shift_down, control_down, menu_down);
		gui_root.getInputState().keyPressed(key_code, key_char, shift_down, control_down, menu_down, repeat);
	}

	public static void keyReleased(GUIRoot gui_root, int key_code, char key_char, boolean shift_down, boolean control_down, boolean menu_down) {
		setKeys(key_code, false, shift_down, control_down, menu_down);
		gui_root.getInputState().keyReleased(key_code, key_char, shift_down, control_down, menu_down);
	}

	public static void mouseDragged(GUIRoot gui_root, int button, short x, short y) {
		setPos(x, y);
		gui_root.getInputState().mouseDragged(button, x, y);
	}

	public static void mouseReleased(GUIRoot gui_root, int button) {
		gui_root.getInputState().mouseReleased(button);
	}

	public static void mousePressed(GUIRoot gui_root, int button) {
		gui_root.getInputState().mousePressed(button);
	}

	public static void mouseScrolled(GUIRoot gui_root, int dz) {
		gui_root.getInputState().mouseScrolled(dz);
	}

	public static void mouseMoved(GUIRoot gui_root, short x, short y) {
		setPos(x, y);
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
		for (int i = 0; i < keys.length; i++) {
            keys[i] = false;
        }
	}

	public static boolean isKeyDown(int key_code) {
		if (key_code >= keys.length) {
			System.out.println("Unsupported key " + key_code);
			return false;
		}
		return keys[key_code];
	}

	public static void setPos(int x, int y) {
		mouse_x = x;
		mouse_y = y;
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

	public static boolean alIsCreated() {
		return LocalEventQueue.getQueue().getDeterministic().log(AL.isCreated());
	}

	public static Path getGameDir() {
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

	public static boolean inFullscreen() {
		return fullscreen;
	}

	public static LocalInput getLocalInput() {
		return instance;
	}

	public static SerializableDisplayMode[] getAvailableModes() {
		try {
			DisplayMode[] lwjgl_modes = Display.getAvailableDisplayModes();
			List modes = new ArrayList();
                    for (DisplayMode lwjgl_mode : lwjgl_modes) {
                        assert lwjgl_mode != null;
                        if (SerializableDisplayMode.isModeValid(lwjgl_mode)) {
                            SerializableDisplayMode mode = new SerializableDisplayMode(lwjgl_mode);
                            modes.add(mode);
                        }
                    }
			modes = (List)LocalEventQueue.getQueue().getDeterministic().log(modes);

			SerializableDisplayMode target_mode = new SerializableDisplayMode(0, 0, 0, 0);
			SortedSet set = new TreeSet(new SerializableDisplayModeComparator(target_mode));
			for (int i = 0; i < modes.size(); i++) {
				set.add(modes.get(i));
			}
			SerializableDisplayMode[] available_modes = new SerializableDisplayMode[set.size()];
			set.toArray(available_modes);
			return available_modes;
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
	}

	public static SerializableDisplayMode getCurrentMode() {
		return (SerializableDisplayMode)LocalEventQueue.getQueue().getDeterministic().log(new SerializableDisplayMode(Display.getDisplayMode()));
	}

	public static int getNativeCursorCaps() {
		return LocalEventQueue.getQueue().getDeterministic().log(Cursor.getCapabilities());
	}

	public static void settings(Path game_dir, Path event_log_dir, Settings settings) {
		instance.setSettings(game_dir, event_log_dir,
				revision, settings);
	}

	public void setSettings(Path game_dir, Path event_log_dir, int revision, Settings settings) {
		System.out.println("revision = " + revision);
		LocalInput.game_dir = game_dir;
		LocalInput.revision = revision;
		settings.last_event_log_dir = event_log_dir.toAbsolutePath().toUri();
		settings.last_revision = revision;
		settings.crashed = true;
		settings.save();
		settings.crashed = false;
		fullscreen = settings.fullscreen;
	}

	public static void init() {
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		mouse_x = deterministic.log(org.lwjgl.input.Mouse.getX());
		mouse_y = deterministic.log(org.lwjgl.input.Mouse.getY());
	}

	private void modeSwitchedLater(SerializableDisplayMode new_mode) {
		Settings.getSettings().fullscreen = fullscreen;
		Settings.getSettings().new_view_width = new_mode.getWidth();
		Settings.getSettings().new_view_height = new_mode.getHeight();
		Settings.getSettings().new_view_freq = new_mode.getFrequency();
	}

	private void modeSwitchedNow(SerializableDisplayMode new_mode) {
		modeSwitchedLater(new_mode);
		modeSwitched();
	}

	private void modeSwitched() {
		SerializableDisplayMode new_mode = (SerializableDisplayMode)LocalEventQueue.getQueue().getDeterministic().log(new SerializableDisplayMode(Display.getDisplayMode()));
		view_width = new_mode.getWidth();
		view_height = new_mode.getHeight();
		System.out.println("Switched mode to " + new_mode);
		Settings.getSettings().view_width = new_mode.getWidth();
		Settings.getSettings().view_height = new_mode.getHeight();
		Settings.getSettings().view_freq = new_mode.getFrequency();
	}

	public void fullscreenToggled(boolean fullscreen, boolean switch_now) {
		Settings.getSettings().fullscreen = fullscreen;
		if (switch_now && LocalInput.fullscreen != fullscreen) {
			toggleFullscreen();
			System.out.println("Fullscreen toggled");
		}
	}

	public static void toggleFullscreen() {
		fullscreen = !fullscreen;
		try {
			Display.setFullscreen(fullscreen && !LocalEventQueue.getQueue().getDeterministic().isPlayback());
			Renderer.resetInput();
		} catch (LWJGLException e) {
			System.out.println("Mode switching failed with exception: " + e);
			throw new RuntimeException("Mode switching failed");
		}
	}

	public void switchMode(SerializableDisplayMode mode, boolean switch_now) {
		if (switch_now) {
			SerializableDisplayMode.switchMode(mode);
			modeSwitchedNow(mode);
		} else
			modeSwitchedLater(mode);
	}

	public void setModeToNearest(SerializableDisplayMode mode) throws LWJGLException {
		SerializableDisplayMode.setModeToNearest(mode);
		modeSwitchedNow(mode);
	}

	public static float getViewAspect() {
		return (float)view_width/view_height;
	}

	private static float getUnitsPerPixel() {
		return (float)(Globals.VIEW_MIN*StrictMath.tan(Globals.FOV*(StrictMath.PI/180.0f)*0.5d)/(view_height*0.5d));
	}

	public static float getErrorConstant() {
		return Globals.VIEW_MIN/(getUnitsPerPixel()*Globals.ERROR_TOLERANCE);
	}
}
