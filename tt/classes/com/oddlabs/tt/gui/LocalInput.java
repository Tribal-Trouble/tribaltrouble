package com.oddlabs.tt.gui;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.List;
import java.util.ArrayList;

import org.lwjgl.openal.AL;
import com.oddlabs.tt.render.Cursor;
import com.oddlabs.tt.render.Display;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.font.Index;
import com.oddlabs.tt.form.QuitForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.KeyboardInput;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.input.PointerInput;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.updater.UpdateInfo;
import com.oddlabs.util.Utils;

public final strictfp class LocalInput {
	public final static int LEFT_BUTTON = 0;
	public final static int RIGHT_BUTTON = 1;
	public final static int MIDDLE_BUTTON = 2;

	private static int mouse_x;
	private static int mouse_y;
	private static boolean global_menu_state = false;
	private static boolean global_control_state = false;
	private static boolean global_shift_state = false;
	private final static boolean[] keys = new boolean[256];

	private static boolean fullscreen;
	private static File game_dir;
	private static int revision;
	private static UpdateInfo update_info;

	private static LocalInput instance;

	public final static void setKeys(int key_code, boolean state, boolean shift_down, boolean control_down, boolean menu_down) {
		keys[key_code] = state;
		global_menu_state = menu_down;
		global_control_state = control_down;
		global_shift_state = shift_down;
	}

	public final static void keyTyped(GUIRoot gui_root, int key_code, char key_char) {
		gui_root.getInputState().keyTyped(key_code, key_char);
	}

	public final static void keyPressed(GUIRoot gui_root, int key_code, char key_char, boolean shift_down, boolean control_down, boolean menu_down, boolean repeat) {
		setKeys(key_code, true, shift_down, control_down, menu_down);
		gui_root.getInputState().keyPressed(key_code, key_char, shift_down, control_down, menu_down, repeat);
	}

	public final static void keyReleased(GUIRoot gui_root, int key_code, char key_char, boolean shift_down, boolean control_down, boolean menu_down) {
		setKeys(key_code, false, shift_down, control_down, menu_down);
		gui_root.getInputState().keyReleased(key_code, key_char, shift_down, control_down, menu_down);
	}

	public final static void mouseDragged(GUIRoot gui_root, int button, short x, short y) {
		setPos(x, y);
		gui_root.getInputState().mouseDragged(button, x, y);
	}

	public final static void mouseReleased(GUIRoot gui_root, int button) {
		gui_root.getInputState().mouseReleased(button);
	}

	public final static void mousePressed(GUIRoot gui_root, int button) {
		gui_root.getInputState().mousePressed(button);
	}

	public final static void mouseScrolled(GUIRoot gui_root, int dz) {
		gui_root.getInputState().mouseScrolled(dz);
	}

	public final static void mouseMoved(GUIRoot gui_root, short x, short y) {
		setPos(x, y);
		gui_root.getInputState().mouseMoved(x, y);
	}

	public final static boolean isShiftDownCurrently() {
		return global_shift_state;
	}

	public final static boolean isControlDownCurrently() {
		return global_control_state;
	}

	public final static boolean isMenuDownCurrently() {
		return global_menu_state;
	}

	public final static void resetKeys() {
		// Clear event queue
		KeyboardInput.reset();
		for (int i = 0; i < keys.length; i++)
			keys[i] = false;
	}

	public final static boolean isKeyDown(int key_code) {
		if (key_code >= keys.length) {
			System.out.println("Unsupported key " + key_code);
			return false;
		}
		return keys[key_code];
	}

	public final static void setPos(int x, int y) {
		mouse_x = x;
		mouse_y = y;
	}

	public final static void resetKeyboard() {
		resetKeys();
		global_menu_state = false;
		global_control_state = false;
		global_shift_state = false;
	}

	public final static int getMouseY() {
		return mouse_y;
	}

	public final static int getMouseX() {
		return mouse_x;
	}

	public final static boolean alIsCreated() {
		return LocalEventQueue.getQueue().getDeterministic().log(Display.isALCreated());
    }

	public final static File getGameDir() {
		return game_dir;
	}

	public final static UpdateInfo getUpdateInfo() {
		return update_info;
	}

	public final static int getRevision() {
		return revision;
	}

	public LocalInput() {
		assert instance == null;
		instance = this;
	}

	public final static int getViewWidth() {
		return Display.getWidth();
	}

	public final static int getViewHeight() {
		return Display.getHeight();
	}

	public final static boolean inFullscreen() {
		return fullscreen;
	}

	public static final LocalInput getLocalInput() {
		return instance;
	}

	public final static void settings(UpdateInfo update_info, File game_dir, File event_log_dir, Settings settings) {
		int revision;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(Utils.tryMakeURL("/revision_number").openStream()));
			String revision_string = in.readLine();
			in.close();
			revision = (new Integer(revision_string)).intValue();
		} catch (Exception e) {
			revision = -1;
		}
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		instance.setSettings(update_info, game_dir, event_log_dir,
				revision, settings);
	}

	public final void setSettings(UpdateInfo update_info, File game_dir, File event_log_dir, int revision, Settings settings) {
		System.out.println("revision = " + revision);
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

	public final static void init() {
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		mouse_x = deterministic.log(com.oddlabs.tt.input.Mouse.getX());
		mouse_y = deterministic.log(com.oddlabs.tt.input.Mouse.getY());
	}

	public static final float getViewAspect() {
		return (float)getViewWidth()/getViewHeight();
	}

	private static final float getUnitsPerPixel() {
		return (float)(Globals.VIEW_MIN*StrictMath.tan(Globals.FOV*(StrictMath.PI/180.0f)*0.5d)/(getViewHeight()*0.5d));
	}

	public static final float getErrorConstant() {
		return Globals.VIEW_MIN/(getUnitsPerPixel()*Globals.ERROR_TOLERANCE);
	}
}
