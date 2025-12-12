package com.oddlabs.tt.input;

import com.oddlabs.tt.input.InputProvider;
import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.render.NativeCursor;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.util.Image;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.opengl.GL11;

import java.util.EnumSet;
import java.util.Set;

public final class PointerInput {
	private static final Set<@NonNull MouseButton> buttons = EnumSet.noneOf(MouseButton.class);
	private static short last_x;
	private static short last_y;
	private static Cursor active_cursor;
	private static @Nullable MouseButton drag_button = null;

	private static final @NonNull NativeCursor debug_cursor;

	static {
		Image image_16_1 = Image.read(Utils.makeURL("/textures/gui/pointer_clientload_16_1.image"));
		GLIntImage img_16_1 = new GLIntImage(image_16_1.getWidth(), image_16_1.getHeight(), image_16_1.getPixels(), GL11.GL_RGBA);
		Image image_32_1 = Image.read(Utils.makeURL("/textures/gui/pointer_clientload_32_1.image"));
		GLIntImage img_32_1 = new GLIntImage(image_32_1.getWidth(), image_32_1.getHeight(), image_32_1.getPixels(), GL11.GL_RGBA);
		Image image_32_8 = Image.read(Utils.makeURL("/textures/gui/pointer_clientload_32_8.image"));
		GLIntImage img_32_8 = new GLIntImage(image_32_8.getWidth(), image_32_8.getHeight(), image_32_8.getPixels(), GL11.GL_RGBA);
		debug_cursor = new NativeCursor(img_16_1, 2, 14,
											 img_32_1, 4, 27,
											 img_32_8, 4, 27);
	}

	public static void setActiveCursor(@Nullable Cursor cursor) {
        InputProvider input = LocalInput.getInputProvider();
        if (input == null) return;
        
		if (cursor != null && input.isGrabbed()) {
			input.setGrabbed(false);
			resetCursorPos();
		} else if (cursor == null && !input.isGrabbed()) {
			input.setGrabbed(true);
			resetCursorPos();
		}
		if (active_cursor != cursor) {
			doSetActiveCursor(cursor);
		}
	}

	public static void setCursorPosition(int x, int y) {
        InputProvider input = LocalInput.getInputProvider();
		if (input != null && !LocalEventQueue.getQueue().getDeterministic().isPlayback())
			input.setCursorPosition(x, y);
	}

	private static void resetCursorPos() {
		setCursorPosition(LocalInput.getMouseX(), LocalInput.getMouseY());
		// clear event queue
        InputProvider input = LocalInput.getInputProvider();
		while (input != null && input.nextMouseEvent())
            ;
	}

	private static void doSetActiveCursor(Cursor cursor) {
		active_cursor = cursor;
        InputProvider input = LocalInput.getInputProvider();
        if (input != null) {
		    input.setNativeCursor(LocalEventQueue.getQueue().getDeterministic().isPlayback() ? debug_cursor.getCursor() : cursor);
        }
	}

	public static void deletingCursor(Cursor cursor) {
		if (active_cursor == cursor)
			doSetActiveCursor(null);
	}

	private static void updateMouse(@NonNull GUIRoot gui_root, int x, int y, int dz) {
		if (x != last_x || y != last_y) {
			last_x = (short)x;
			last_y = (short)y;
			if (drag_button != null && buttons.contains(drag_button)) {
				LocalInput.mouseDragged(gui_root, drag_button, last_x, last_y);
			} else {
				LocalInput.mouseMoved(gui_root, last_x, last_y);
			}
		}
		if (dz != 0)
			LocalInput.mouseScrolled(gui_root, dz);
	}

	public static void poll(@NonNull GUIRoot gui_root) {
        InputProvider input = LocalInput.getInputProvider();
        if (input == null) return;
        
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
						LocalInput.mousePressed(gui_root, button);
					}
                } else {
                    if (buttons.remove(button)) {
						drag_button = null;
						LocalInput.mouseReleased(gui_root, button);
					}
                }
			}
		}
		updateMouse(gui_root, accum_x, accum_y, accum_dz);
	}

    private PointerInput() {
    }
}
