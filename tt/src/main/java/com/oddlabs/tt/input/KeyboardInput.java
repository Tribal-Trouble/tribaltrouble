package com.oddlabs.tt.input;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

public final class KeyboardInput {
	private static final int LITTLE_WARP = 1000;
	public static final int MEDIUM_WARP = 10000;
	public static final int LARGE_WARP = 100000;
	private static final int GOTO_END_OF_LOG_WARP = Integer.MAX_VALUE/2;

	private static final KeyboardInput instance = new KeyboardInput();

	private boolean left_shift_down;
	private boolean right_shift_down;
	private boolean left_control_down;
	private boolean right_control_down;
	private boolean left_alt_down;
	private boolean right_alt_down;
	private boolean left_super_down;
	private boolean right_super_down;

	public static void reset() {
		while (Renderer.getLocalInput().getInputProvider() != null && Renderer.getLocalInput().getInputProvider().nextKeyboardEvent())
			;
	}

	public static boolean isAltDown() {
		return instance.left_alt_down || instance.right_alt_down;
	}

	private boolean checkMagicKey(Deterministic deterministic, boolean event_key_state, @NonNull Key event_key, boolean override, boolean repeat) {
        boolean control_down = left_control_down || right_control_down;
		boolean shift_down = left_shift_down || right_shift_down;
		boolean keys_enabled = Settings.getSettings().inDeveloperMode() && control_down && shift_down && !repeat;
		if (event_key_state && (keys_enabled || override)) {
			// check for special events that shouldn't generate events
            switch (event_key) {
                case RIGHT -> { AnimationManager.warpTime(LITTLE_WARP); return true; }
                case UP -> { AnimationManager.warpTime(MEDIUM_WARP); return true; }
                case PAGE_UP -> { AnimationManager.warpTime(LARGE_WARP); return true; }
                case Q -> {
                    IO.println("Exit forced with ctrl+Q");
                    Renderer.shutdown();
					return true;
                }
                case SPACE -> { AnimationManager.toggleTimeStop(); return true; }
                case END -> {
                    IO.println("WARP UNTIL END OF EVENT LOG");
                    AnimationManager.warpTime(GOTO_END_OF_LOG_WARP);
					return true;
                }
                default -> {
                }
            }
		}
		return false;
	}

	public static void checkMagicKeys() {
		instance.doCheckMagicKeys();
	}

	public void doCheckMagicKeys() {
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		if (deterministic.isPlayback()) {
            InputProvider<?> input = Renderer.getLocalInput().getInputProvider();
			input.pollKeyboard();
			while (input.nextKeyboardEvent()) {
				int event_key_code = input.getEventKey();
				var event_key = Key.fromLwjglCode(event_key_code);
				if (Key.KEY_UNKNOWN != event_key) {
					boolean event_key_state = input.getEventKeyState();
					checkMagicKey(deterministic, event_key_state, event_key, true, input.isRepeatEvent());
				}
			}
		}
	}

	public static boolean poll(@NonNull GUIRoot gui_root) {
		return instance.doPoll(gui_root);
	}

	public boolean doPoll(@NonNull GUIRoot gui_root) {
        InputProvider<?> input = Renderer.getLocalInput().getInputProvider();
        
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		boolean result = false;
		input.pollKeyboard();
        // Update modifiers from raw state to handle lost events or initial state
        left_shift_down = input.isKeyDown(Key.LSHIFT.getLwjglCode());
        right_shift_down = input.isKeyDown(Key.RSHIFT.getLwjglCode());
        left_control_down = input.isKeyDown(Key.LCONTROL.getLwjglCode());
        right_control_down = input.isKeyDown(Key.RCONTROL.getLwjglCode());
        left_alt_down = input.isKeyDown(Key.LALT.getLwjglCode());
        right_alt_down = input.isKeyDown(Key.RALT.getLwjglCode());

		while (deterministic.log(input.nextKeyboardEvent())) {
			result = true;
			int event_key_code = deterministic.log(input.getEventKey());
			Key event_key = Key.fromLwjglCode(event_key_code);
			boolean event_key_state = deterministic.log(input.getEventKeyState());
			char event_character = deterministic.log(input.getEventCharacter());
			boolean repeat_event = deterministic.log(input.isRepeatEvent());

			switch (event_key) {
				case LSHIFT:
					left_shift_down = event_key_state;
					break;
				case RSHIFT:
					right_shift_down = event_key_state;
					break;
				case LCONTROL:
					left_control_down = event_key_state;
					break;
				case RCONTROL:
					right_control_down = event_key_state;
					break;
				case LALT:
					left_alt_down = event_key_state;
					break;
				case RALT:
					right_alt_down = event_key_state;
					break;
				case LSUPER:
					left_super_down = event_key_state;
					break;
				case RSUPER:
					right_super_down = event_key_state;
					break;
			}
			boolean control_down = left_control_down || right_control_down;
			boolean shift_down = left_shift_down || right_shift_down;
			boolean alt_down = left_alt_down || right_alt_down;
			boolean super_down = left_super_down || right_super_down;

			if (checkMagicKey(deterministic, event_key_state, event_key, false, repeat_event))
				continue;

			var localInput = Renderer.getLocalInput();
			if (event_key_code == 0 && !(control_down || shift_down || alt_down || super_down)) {
				localInput.keyTyped(gui_root, event_key_code, event_character);
			} else if (event_key_state) {
				localInput.keyPressed(gui_root, event_key_code, event_character, shift_down, control_down, alt_down, super_down, repeat_event);
			} else {
				localInput.keyReleased(gui_root, event_key_code, event_character, shift_down, control_down, alt_down, super_down);
			}
		}
		return result;
	}
}
