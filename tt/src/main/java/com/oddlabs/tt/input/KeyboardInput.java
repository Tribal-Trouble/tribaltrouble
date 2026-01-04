package com.oddlabs.tt.input;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
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
	private boolean left_menu_down;
	private boolean right_menu_down;
	private long lastControlReleaseTime;

	public static void reset() {
		while (LocalInput.getInputProvider() != null && LocalInput.getInputProvider().nextKeyboardEvent())
			;
	}

	public static boolean isMenuDown() {
		return instance.left_menu_down || instance.right_menu_down;
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
                case PRIOR -> { AnimationManager.warpTime(LARGE_WARP); return true; }
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
            InputProvider<?> input = LocalInput.getInputProvider();
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
        InputProvider<?> input = LocalInput.getInputProvider();
        if (input == null) return false;
        
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		boolean result = false;
		input.pollKeyboard();
        // Update modifiers from raw state to handle lost events or initial state
        left_shift_down = input.isKeyDown(Key.LSHIFT.getLwjglCode());
        right_shift_down = input.isKeyDown(Key.RSHIFT.getLwjglCode());
        left_control_down = input.isKeyDown(Key.LCONTROL.getLwjglCode());
        right_control_down = input.isKeyDown(Key.RCONTROL.getLwjglCode());
        left_menu_down = input.isKeyDown(Key.LMENU.getLwjglCode());
        right_menu_down = input.isKeyDown(Key.RMENU.getLwjglCode());

		while (deterministic.log(input.nextKeyboardEvent())) {
			result = true;
			int event_key_code = deterministic.log(input.getEventKey());
			Key event_key = Key.fromLwjglCode(event_key_code);
			boolean event_key_state = deterministic.log(input.getEventKeyState());
			char event_character = deterministic.log(input.getEventCharacter());
			boolean repeat_event = deterministic.log(input.isRepeatEvent());

            boolean control_down = left_control_down || right_control_down;
            boolean shift_down = left_shift_down || right_shift_down;
            boolean menu_down = left_menu_down || right_menu_down;

			if (Key.KEY_UNKNOWN != event_key) {
				switch (event_key) {
					case LSHIFT:
						left_shift_down = event_key_state;
						break;
					case RSHIFT:
						right_shift_down = event_key_state;
						break;
					case LCONTROL:
						if (left_control_down && !event_key_state) lastControlReleaseTime = System.currentTimeMillis();
						left_control_down = event_key_state;
						break;
					case RCONTROL:
						if (right_control_down && !event_key_state) lastControlReleaseTime = System.currentTimeMillis();
						right_control_down = event_key_state;
						break;
					case LMENU:
						left_menu_down = event_key_state;
						break;
					case RMENU:
						right_menu_down = event_key_state;
						break;
				}
                // Update locals after state change
                control_down = left_control_down || right_control_down;
                shift_down = left_shift_down || right_shift_down;
                menu_down = left_menu_down || right_menu_down;

				if (checkMagicKey(deterministic, event_key_state, event_key, false, repeat_event))
					continue;
			}
			
			boolean effective_control_down = control_down || (System.currentTimeMillis() - lastControlReleaseTime < 150);
			
			if (event_key_code == 0) {
				LocalInput.keyTyped(gui_root, event_key_code, event_character);
			} else if (event_key_state) {
				LocalInput.keyPressed(gui_root, event_key_code, event_character, shift_down, effective_control_down, menu_down, repeat_event);
			} else {
				LocalInput.keyReleased(gui_root, event_key_code, event_character, shift_down, effective_control_down, menu_down);
			}
		}
		return result;
	}
}
