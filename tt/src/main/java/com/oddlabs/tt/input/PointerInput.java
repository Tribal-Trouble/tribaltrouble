package com.oddlabs.tt.input;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.Cursor;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.CursorFile;
import com.oddlabs.tt.resource.Resources;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PointerInput {
	private static final Set<@NonNull MouseButton> buttons = EnumSet.noneOf(MouseButton.class);
	private static short last_x;
	private static short last_y;
	private static @NonNull Cursor active_cursor = Cursor.NULL_CURSOR;
	private static @Nullable MouseButton drag_button = null;

	private static final Cursor DEBUG_CURSOR =
			Resources.findResource(new CursorFile("/textures/gui/pointer_clientload_32_8.image", 2, 29));

    private static final Map<@NonNull CursorType, @NonNull Cursor> cursors = new EnumMap<>(Map.of(
			CursorType.NORMAL, Resources.findResource(new CursorFile("/textures/gui/pointer_32_8.image", 2, 29)),
			CursorType.TARGET, Resources.findResource(new CursorFile("/textures/gui/pointer_target_32_8.image", 2, 29)),
			CursorType.TEXT, Resources.findResource(new CursorFile("/textures/gui/pointer_text_32_8.image", 2, 29)),
			CursorType.DEBUG, DEBUG_CURSOR,
			CursorType.NULL, Cursor.NULL_CURSOR
	));

	public static void setActiveCursor(@NonNull CursorType type) {
		setActiveCursor(cursors.get(type));
	}
	public static void setActiveCursor(@NonNull Cursor cursor) {
        InputProvider<?> input = Renderer.getLocalInput().getInputProvider();

		if (cursor != Cursor.NULL_CURSOR && input.isGrabbed()) {
			input.setGrabbed(false);
			resetCursorPos();
		} else if (cursor == Cursor.NULL_CURSOR && !input.isGrabbed()) {
			input.setGrabbed(true);
			resetCursorPos();
		}
		if (active_cursor != cursor) {
			doSetActiveCursor(cursor);
		}
	}

	public static void setCursorPosition(int x, int y) {
        InputProvider<?> input = Renderer.getLocalInput().getInputProvider();
		if (!LocalEventQueue.getQueue().getDeterministic().isPlayback())
			input.setCursorPosition(x, y);
	}

	private static void resetCursorPos() {
		setCursorPosition(Renderer.getLocalInput().getMouseX(), Renderer.getLocalInput().getMouseY());
		// clear event queue
        InputProvider<?> input = Renderer.getLocalInput().getInputProvider();
		while (input.nextMouseEvent())
            ;
	}

	private static void doSetActiveCursor(@NonNull Cursor cursor) {
		active_cursor = cursor;
        InputProvider<Long> input = Renderer.getLocalInput().getInputProvider();
        var useCursor = LocalEventQueue.getQueue().getDeterministic().isPlayback()
                ? DEBUG_CURSOR : cursor;
        input.setNativeCursor(useCursor.getCursor());
    }

	public static void deletingCursor(@NonNull Cursor cursor) {
		if (active_cursor == cursor) {
			doSetActiveCursor(Cursor.NULL_CURSOR);
		}
	}

	private static void updateMouse(@NonNull GUIRoot gui_root, int x, int y, int dz) {
		if (x != last_x || y != last_y) {
			last_x = (short)x;
			last_y = (short)y;
			if (drag_button != null && buttons.contains(drag_button)) {
				Renderer.getLocalInput().mouseDragged(gui_root, drag_button, last_x, last_y);
			} else {
				Renderer.getLocalInput().mouseMoved(gui_root, last_x, last_y);
			}
		}
		if (dz != 0)
			Renderer.getLocalInput().mouseScrolled(gui_root, dz);
	}

	public static void poll(@NonNull GUIRoot gui_root) {
        InputProvider<?> input = Renderer.getLocalInput().getInputProvider();
        
		Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
		input.pollMouse();
		int accum_x = last_x;
		int accum_y = last_y;
		int accum_dz = 0;
		while (deterministic.log(input.nextMouseEvent())) {
			accum_x = deterministic.log(input.getEventX());
			accum_y = deterministic.log(input.getEventY());
			accum_dz += deterministic.log(input.getEventDWheel());
			MouseButton button = MouseButton.fromInt(deterministic.log(input.getEventButton()));
			if (button != null) {
				updateMouse(gui_root, accum_x, accum_y, accum_dz);
				accum_dz = 0;
				if (deterministic.log(input.getEventButtonState())) {
                    if (buttons.add(button)) {
						if (drag_button == null) {
							drag_button = button;
						}
						Renderer.getLocalInput().mousePressed(gui_root, button);
					}
                } else {
                    if (buttons.remove(button)) {
						drag_button = null;
						Renderer.getLocalInput().mouseReleased(gui_root, button);
					}
                }
			}
		}
		updateMouse(gui_root, accum_x, accum_y, accum_dz);
	}

    private PointerInput() {
    }
}