package com.oddlabs.tt.input;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;
import com.oddlabs.tt.input.InputProvider;

public final class KeyboardInput {
	private static final int LITTLE_WARP = 1000;
	public static final int MEDIUM_WARP = 10000;
	public static final int LARGE_WARP = 100000;
	private static final int GOTO_END_OF_LOG_WARP = Integer.MAX_VALUE/2;

	private static final KeyboardInput instance = new KeyboardInput();

	private boolean shift_down;
	private boolean control_down;
	private boolean menu_down;

	public static void reset() {
		while (LocalInput.getInputProvider() != null && LocalInput.getInputProvider().nextKeyboardEvent())
			;
	}

	public static boolean isMenuDown() {
		return instance.menu_down;
	}

	private boolean checkMagicKey(Deterministic deterministic, boolean event_key_state, @NonNull Key event_key, boolean override, boolean repeat) {
		boolean keys_enabled = Settings.getSettings().inDeveloperMode() && control_down && shift_down && !repeat;
		if (event_key_state && (keys_enabled || override)) {
			// check for special events that shouldn't generate events
            switch (event_key) {
                case RIGHT -> AnimationManager.warpTime(LITTLE_WARP);
                case UP -> AnimationManager.warpTime(MEDIUM_WARP);
                case PRIOR -> AnimationManager.warpTime(LARGE_WARP);
                case Q -> {
                    IO.println("Exit forced with ctrl+Q");
                    Renderer.shutdown();
                }
                case SPACE -> AnimationManager.toggleTimeStop();
                case END -> {
                    IO.println("WARP UNTIL END OF EVENT LOG");
                    AnimationManager.warpTime(GOTO_END_OF_LOG_WARP);
                }
                default -> {
                }
            }
		}
		return keys_enabled;
	}

	public static void checkMagicKeys() {
		instance.doCheckMagicKeys();
	}

	public void doCheckMagicKeys() {
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		if (deterministic.isPlayback()) {
            InputProvider input = LocalInput.getInputProvider();
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
        InputProvider input = LocalInput.getInputProvider();
        if (input == null) return false;
        
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		boolean result = false;
		input.pollKeyboard();
		while (deterministic.log(input.nextKeyboardEvent())) {
			result = true;
			int event_key_code = deterministic.log(input.getEventKey());
			Key event_key = Key.fromLwjglCode(event_key_code);
			boolean event_key_state = deterministic.log(input.getEventKeyState());
			char event_character = deterministic.log(input.getEventCharacter());
			boolean repeat_event = deterministic.log(input.isRepeatEvent());
			if (Key.KEY_UNKNOWN != event_key) {
				switch (event_key) {
					case LSHIFT:
					case RSHIFT:
						shift_down = event_key_state;
						break;
					case LCONTROL:
					case RCONTROL:
						control_down = event_key_state;
						break;
					case LMENU:
					case RMENU:
						menu_down = event_key_state;
						break;
				}
				if (checkMagicKey(deterministic, event_key_state, event_key, false, repeat_event))
					continue;
			}
			if (event_key_code == 0) {
				LocalInput.keyTyped(gui_root, event_key_code, event_character);
			} else if (event_key_state) {
				LocalInput.keyPressed(gui_root, event_key_code, event_character, shift_down, control_down, menu_down, repeat_event);
			} else {
				LocalInput.keyReleased(gui_root, event_key_code, event_character, shift_down, control_down, menu_down);
			}
		}
		return result;
	}
}
