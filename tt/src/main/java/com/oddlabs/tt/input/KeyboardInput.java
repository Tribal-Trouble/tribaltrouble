package com.oddlabs.tt.input;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.GLFW_MOD_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER;

public final class KeyboardInput {
    private static final Logger logger = Logger.getLogger(KeyboardInput.class.getName());
	private static final int LITTLE_WARP = 1000;
	public static final int MEDIUM_WARP = 10000;
	public static final int LARGE_WARP = 100000;
	private static final int GOTO_END_OF_LOG_WARP = Integer.MAX_VALUE/2;

	private boolean left_shift_down;
	private boolean right_shift_down;
	private boolean left_control_down;
	private boolean right_control_down;
	private boolean left_alt_down;
	private boolean right_alt_down;
	private boolean left_meta_down;
	private boolean right_meta_down;

	public void reset(@NonNull InputProvider<?> input) {
		while (input != null && input.nextKeyboardEvent())
			;
        left_shift_down = false;
        right_shift_down = false;
        left_control_down = false;
        right_control_down = false;
        left_alt_down = false;
        right_alt_down = false;
        left_meta_down = false;
        right_meta_down = false;
	}

	public boolean isAltDown() {
		return left_alt_down || right_alt_down;
	}

	public boolean isMetaDown() {
		return left_meta_down || right_meta_down;
	}

	/**
	 * process playback controls.
	 *
	 * @param event_key_down true if the key is down
	 * @param event_key the key
	 * @param playback if true allow magic keys without developer mode or control-shift modifier
	 * @param repeat not an initial key-press
	 * @return true if the key was handled
	 */
	private boolean checkMagicKey(boolean event_key_down, @NonNull Key event_key, boolean playback, boolean repeat) {
		boolean control_down = left_control_down || right_control_down;
		boolean shift_down = left_shift_down || right_shift_down;
		boolean keys_enabled = Settings.getSettings().inDeveloperMode() && control_down && shift_down && !repeat;
		if (event_key_down && (keys_enabled || playback)) {
			// check for special events that shouldn't generate events
			switch (event_key) {
				case RIGHT -> { AnimationManager.warpTime(LITTLE_WARP); return true; }
				case UP -> { AnimationManager.warpTime(MEDIUM_WARP); return true; }
				case PAGE_UP -> { AnimationManager.warpTime(LARGE_WARP); return true; }
				case Q -> {
					Renderer.shutdown();
					return true;
				}
				case SPACE -> { AnimationManager.toggleTimeStop(); return true; }
				case END -> {
					logger.info("WARP UNTIL END OF EVENT LOG");
					AnimationManager.warpTime(GOTO_END_OF_LOG_WARP);
					return true;
				}
				default -> {
				}
			}
		}
		return false;
	}

	public void checkMagicKeys(@NonNull InputProvider<?> input) {
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		if (deterministic.isPlayback()) {
			// During playback the keyboard is used for playback control
			input.pollKeyboard();
			while (input.nextKeyboardEvent()) {
				int event_key_code = input.getEventKey();
				var event_key = Key.fromGlfwCode(event_key_code);
				if (Key.KEY_UNKNOWN != event_key) {
					boolean event_key_state = input.getEventKeyState();
					checkMagicKey(event_key_state, event_key, true, input.isRepeatEvent());
				}
			}
		}
	}

	public boolean poll(@NonNull InputProvider<?> input, @NonNull LocalInput localInput, @NonNull GUIRoot gui_root) {
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		boolean result = false;
		input.pollKeyboard();
        // Update modifiers from raw state to handle lost events or initial state
        left_shift_down = input.isKeyDown(Key.LSHIFT.getGlfwCode());
        right_shift_down = input.isKeyDown(Key.RSHIFT.getGlfwCode());
        left_control_down = input.isKeyDown(Key.LCONTROL.getGlfwCode());
        right_control_down = input.isKeyDown(Key.RCONTROL.getGlfwCode());
        left_alt_down = input.isKeyDown(Key.LALT.getGlfwCode());
        right_alt_down = input.isKeyDown(Key.RALT.getGlfwCode());
		left_meta_down = input.isKeyDown(Key.LSUPER.getGlfwCode());
		right_meta_down = input.isKeyDown(Key.RSUPER.getGlfwCode());

		while (deterministic.log(input.nextKeyboardEvent())) {
			result = true;
			int event_key_code = deterministic.log(input.getEventKey());
			int event_key_mods = deterministic.log(input.getEventKeyMods());
			Key event_key = Key.fromGlfwCode(event_key_code);
			boolean event_key_down = deterministic.log(input.getEventKeyState());
			char event_character = deterministic.log(input.getEventCharacter());
			boolean repeat_event = deterministic.log(input.isRepeatEvent());

			switch (event_key) {
				case LSHIFT:
					left_shift_down = event_key_down;
					break;
				case RSHIFT:
					right_shift_down = event_key_down;
					break;
				case LCONTROL:
					left_control_down = event_key_down;
					break;
				case RCONTROL:
					right_control_down = event_key_down;
					break;
				case LALT:
					left_alt_down = event_key_down;
					break;
				case RALT:
					right_alt_down = event_key_down;
					break;
				case LSUPER:
					left_meta_down = event_key_down;
					break;
				case RSUPER:
					right_meta_down = event_key_down;
					break;
			}

			if (checkMagicKey(event_key_down, event_key, false, repeat_event))
				continue;

			boolean control_down = (event_key_mods & GLFW_MOD_CONTROL) != 0;
			boolean shift_down = (event_key_mods & GLFW_MOD_SHIFT) != 0;
			boolean alt_down = (event_key_mods & GLFW_MOD_ALT) != 0;
			boolean meta_down = (event_key_mods & GLFW_MOD_SUPER) != 0;

            // Use passed localInput, not static Renderer
			if (event_key_code == 0 && !(control_down || shift_down || alt_down || meta_down)) {
				gui_root.getInputState().keyTyped(event_key_code, event_character);
			} else if (event_key_down) {
				if (Key.KEY_UNKNOWN != event_key || event_character != 0) {
					localInput.setKeys(event_key, true, shift_down, control_down, alt_down, meta_down);
					gui_root.getInputState().keyPressed(event_key, event_character, shift_down, control_down, alt_down, meta_down, repeat_event);
				}
			} else {
				if (Key.KEY_UNKNOWN != event_key || event_character != 0) {
					localInput.setKeys(event_key, false, shift_down, control_down, alt_down, meta_down);
					gui_root.getInputState().keyReleased(event_key, event_character, shift_down, control_down, alt_down, meta_down);
				}
			}
		}
		return result;
	}
}
