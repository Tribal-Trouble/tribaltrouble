package com.oddlabs.tt.input;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.Cursor;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.resource.CursorFile;
import com.oddlabs.tt.resource.Resources;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PointerInput {
    private final Set<@NonNull MouseButton> buttons = EnumSet.noneOf(MouseButton.class);
    private short last_x;
    private short last_y;
    private @NonNull Cursor active_cursor = Cursor.NULL_CURSOR;
    private @Nullable MouseButton drag_button = null;

    private final @NonNull InputProvider<?> inputProvider;
    private final @NonNull LocalInput localInput;

    private final Map<@NonNull CursorType, @NonNull Cursor> cursors = new EnumMap<>(CursorType.class);
    private @NonNull Cursor debug_cursor = Cursor.NULL_CURSOR;

    public void loadCursors() {
        debug_cursor = Resources.findResource(new CursorFile("/textures/gui/pointer_clientload_32_8.png", 2, 2));
        cursors.put(CursorType.NORMAL, Resources.findResource(new CursorFile("/textures/gui/pointer_32_8.png", 2, 2)));
        cursors.put(CursorType.TARGET, Resources.findResource(new CursorFile("/textures/gui/pointer_target_32_8.png", 14, 14)));
        cursors.put(CursorType.TEXT, Resources.findResource(new CursorFile("/textures/gui/pointer_text_32_8.png", 6, 11)));
        cursors.put(CursorType.DEBUG, debug_cursor);
        cursors.put(CursorType.NULL, Cursor.NULL_CURSOR);
    }

    public PointerInput(@NonNull InputProvider<?> inputProvider, @NonNull LocalInput localInput) {
        this.inputProvider = inputProvider;
        this.localInput = localInput;
    }

    public void setActiveCursor(@NonNull CursorType type) {
        Cursor c = cursors.get(type);
        if (c != null) {
            setActiveCursor(c);
        }
    }

    public void setActiveCursor(@NonNull Cursor cursor) {
        if (cursor != Cursor.NULL_CURSOR) {
            boolean wasGrabbed = inputProvider.isGrabbed();
            inputProvider.setGrabbed(false);
            if (wasGrabbed) {
                resetCursorPos();
            }
        } else if (!inputProvider.isGrabbed()) {
            inputProvider.setGrabbed(true);
            resetCursorPos();
        }
        if (active_cursor != cursor) {
            doSetActiveCursor(cursor);
        }
    }

    public boolean isGrabbed() {
        return inputProvider.isGrabbed();
    }

    public void setCursorPosition(int x, int y) {
        if (!LocalEventQueue.getQueue().getDeterministic().isPlayback())
            inputProvider.setCursorPosition(x, y);
    }

    private void resetCursorPos() {
        setCursorPosition(localInput.getMouseX(), localInput.getMouseY());
        // clear event queue
        while (inputProvider.nextMouseEvent())
            ;
    }

    private void doSetActiveCursor(@NonNull Cursor cursor) {
        active_cursor = cursor;
        //noinspection unchecked
        InputProvider<Long> provider = (InputProvider<Long>) inputProvider;

        var useCursor = LocalEventQueue.getQueue().getDeterministic().isPlayback()
                ? debug_cursor : cursor;
        provider.setNativeCursor(useCursor.getCursor());
    }

    public void deletingCursor(@NonNull Cursor cursor) {
        if (active_cursor == cursor) {
            doSetActiveCursor(Cursor.NULL_CURSOR);
        }
    }

    private void updateMouse(@NonNull GUIRoot gui_root, int x, int y, int dz) {
        if (x != last_x || y != last_y) {
            last_x = (short) x;
            last_y = (short) y;
            if (drag_button != null && buttons.contains(drag_button)) {
                localInput.mouseDragged(gui_root, drag_button, last_x, last_y);
            } else {
                localInput.mouseMoved(gui_root, last_x, last_y);
            }
        }
        if (dz != 0)
            localInput.mouseScrolled(gui_root, dz);
    }

    public void poll(@NonNull GUIRoot gui_root) {
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        inputProvider.pollMouse();
        int accum_x = last_x;
        int accum_y = last_y;
        int accum_dz = 0;
        while (deterministic.log(inputProvider.nextMouseEvent())) {
            accum_x = deterministic.log(inputProvider.getEventX());
            accum_y = deterministic.log(inputProvider.getEventY());
            accum_dz += deterministic.log(inputProvider.getEventDWheel());
            MouseButton button = MouseButton.fromInt(deterministic.log(inputProvider.getEventButton()));
            if (button != null) {
                updateMouse(gui_root, accum_x, accum_y, accum_dz);
                accum_dz = 0;
                if (deterministic.log(inputProvider.getEventButtonState())) {
                    if (buttons.add(button)) {
                        if (drag_button == null) {
                            drag_button = button;
                        }
                        localInput.mousePressed(gui_root, button);
                    }
                } else {
                    if (buttons.remove(button)) {
                        drag_button = null;
                        localInput.mouseReleased(gui_root, button);
                    }
                }
            }
        }
        updateMouse(gui_root, accum_x, accum_y, accum_dz);
    }
}
