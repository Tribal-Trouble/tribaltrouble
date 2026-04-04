package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.font.Index;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.KeyboardEvent;
import com.oddlabs.tt.input.Modifier;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public final class InputState {
    private static final float MOUSE_REPEAT_DELAY = .5f;
    private static final float MOUSE_REPEAT_RATE = .05f;

    private static final float DOUBLE_CLICK_TIMEOUT = .4f;
    private static final int DOUBLE_CLICK_THRESHOLD = 2;

    private final @NonNull TimerAnimation double_click_timer;
    private final @NonNull TimerAnimation double_key_timer;
    private final GUIRoot gui_root;
    private TimerAnimation mouse_timer;

    private int drag_x;
    private int drag_y;
    private int absolute_drag_x;
    private int absolute_drag_y;
    private @Nullable GUIObject drag_obj;
    private MouseButton drag_button;
    private GUIObject press_obj;
    private @Nullable GUIObject clicked_obj;
    private int click_counter = 0;
    private int clicked_x;
    private int clicked_y;
    private @Nullable KeyboardEvent key_event;
    private int key_counter = 0;

    // Keyboard handling
    private KeyboardEvent held_event;

    public InputState(GUIRoot gui_root) {
        this.gui_root = gui_root;
        double_click_timer = new TimerAnimation(new DoubleClickTimer(), 0);
        double_key_timer = new TimerAnimation(new DoubleKeyTimer(), 0);
        press_obj = gui_root;
    }

    private @NonNull GUIObject pick() {
        gui_root.mousePick();
        return gui_root.getCurrentGUIObject();
    }

    public void mouseMoved(short x, short y) {
        GUIObject gui_hit = pick();
        float scale = gui_root.getGlobalScale();
        gui_hit.mouseMovedAll((short) Math.round(x / scale), (short) Math.round(y / scale));
    }

    private void resetKeyTimer() {
        Index.resetBlinking();
    }

    public void mouseScrolled(int dz) {
        GUIObject gui_hit = pick();
        int scroll_amount = Math.round(dz * Globals.WHEEL_SCALE);
        gui_hit.setFocus();
        gui_hit.mouseScrolledAll(scroll_amount);
    }

    public void mousePressed(@NonNull MouseButton button) {
        GUIObject gui_hit = pick();
        var localInput = Renderer.getLocalInput();
        float scale = gui_root.getGlobalScale();
        int scaledX = Math.round(localInput.getMouseX() / scale);
        int scaledY = Math.round(localInput.getMouseY() / scale);

        int local_x = gui_hit.translateXToLocal(scaledX);
        int local_y = gui_hit.translateYToLocal(scaledY);
        Index.resetBlinking();
        drag_x = scaledX;
        drag_y = scaledY;
        absolute_drag_x = drag_x;
        absolute_drag_y = drag_y;
        if (drag_obj == null) {
            drag_obj = gui_hit;
            drag_button = button;
        }
        gui_hit.setFocus();
        press_obj = gui_hit;
        press_obj.mousePressedAll(button, local_x, local_y);
        if (mouse_timer != null)
            mouse_timer.stop();
        mouse_timer = new TimerAnimation(timer -> {
            timer.setTimerInterval(MOUSE_REPEAT_RATE);
            timer.resetTime();
            if (press_obj == gui_root.getCurrentGUIObject()) {
                int local_x1 = press_obj.translateXToLocal(Math.round(localInput.getMouseX() / scale));
                int local_y1 = press_obj.translateYToLocal(Math.round(localInput.getMouseY() / scale));
                press_obj.mouseHeldAll(button, local_x1, local_y1);
            }
        }, MOUSE_REPEAT_DELAY);

        mouse_timer.start();
    }

    public void mouseReleased(@NonNull MouseButton button) {
        GUIObject gui_hit = pick();
        var localInput = Renderer.getLocalInput();
        float scale = gui_root.getGlobalScale();
        int scaledX = Math.round(localInput.getMouseX() / scale);
        int scaledY = Math.round(localInput.getMouseY() / scale);

        Index.resetBlinking();
        if (button == drag_button)
            drag_obj = null;
        if (press_obj == null)
            return;

        int local_x = press_obj.translateXToLocal(scaledX);
        int local_y = press_obj.translateYToLocal(scaledY);

        if (gui_hit == press_obj) {
            if (gui_hit != clicked_obj || !clickedSameArea()) {
                if (double_click_timer.isRunning()) {
                    stopDoubleClickTimer();
                }
                double_click_timer.setTimerInterval(DOUBLE_CLICK_TIMEOUT);
                double_click_timer.start();
            }
            click_counter++;
            clicked_obj = gui_hit;
            clicked_x = localInput.getMouseX(); // Keep raw for consistent threshold check? Or scale? Raw is fine for distance check.
            clicked_y = localInput.getMouseY();
            press_obj.mouseClickedAll(button, local_x, local_y, click_counter);
        }
        press_obj.mouseReleasedAll(button, local_x, local_y);
        if (mouse_timer != null)
            mouse_timer.stop();
    }

    public void mouseDragged(@NonNull MouseButton button, short x, short y) {
        float scale = gui_root.getGlobalScale();
        int scaledX = Math.round(x / scale);
        int scaledY = Math.round(y / scale);

        if (drag_obj != null)
            drag_obj.mouseDraggedAll(button, scaledX, scaledY, scaledX - drag_x, scaledY - drag_y, scaledX - absolute_drag_x, scaledY - absolute_drag_y);
        drag_x = scaledX;
        drag_y = scaledY;
    }

    private boolean clickedSameArea() {
        var localInput = Renderer.getLocalInput();
        return Math.abs(localInput.getMouseX() - clicked_x) < DOUBLE_CLICK_THRESHOLD && Math.abs(localInput.getMouseY() - clicked_y) < DOUBLE_CLICK_THRESHOLD;
    }

    private void stopDoubleClickTimer() {
        double_click_timer.stop();
        double_click_timer.resetTime();
        clicked_obj = null;
        click_counter = 0;
    }

    private void stopDoubleKeyTimer() {
        double_key_timer.stop();
        double_key_timer.resetTime();
        key_event = null;
        key_counter = 0;
    }

    public void keyTyped(int key_code, char key_char) {
        var key = Key.fromGlfwCode(key_code);
        if (Key.KEY_UNKNOWN != key || key_char != 0) {
            GUIObject focused = gui_root.getGlobalFocus();
            KeyboardEvent keyEvent = new KeyboardEvent(key, key_char, EnumSet.noneOf(Modifier.class), 1);
            Set<GameAction> actions = Renderer.getLocalInput().getInputManager().getActions(keyEvent);
            InputEvent event = new InputEvent(keyEvent, actions, InputPhase.REPEAT);
            focused.handleInputAll(event);
        }
    }

    public void keyPressed(@NonNull Key key, char key_char, @NonNull Set<@NonNull Modifier> modifiers, boolean repeat) {
        GUIObject focused = gui_root.getGlobalFocus();
        resetKeyTimer();
        if (!repeat && (key_event == null
                || key_event.keyCode() != key
                || key_event.keyChar() != key_char
                || !key_event.modifiers().equals(modifiers))) {
            if (double_key_timer.isRunning()) {
                stopDoubleKeyTimer();
            }
            double_key_timer.setTimerInterval(DOUBLE_CLICK_TIMEOUT);
            double_key_timer.start();
        }
        if (!repeat)
            key_counter++;
        KeyboardEvent keyEvent = new KeyboardEvent(key, key_char, modifiers, key_counter);
        key_event = keyEvent;

        Set<GameAction> actions = Renderer.getLocalInput().getInputManager().getActions(keyEvent);
        Renderer.getLocalInput().getInputManager().updateState(keyEvent, true); // Update polling state

        if (!repeat) {
            InputEvent event = new InputEvent(keyEvent, actions, InputPhase.PRESSED);
            focused.handleInputAll(event);
        } else {
            // Repeat phase
            InputEvent repeatEvent = new InputEvent(keyEvent, actions, InputPhase.REPEAT);
            focused.handleInputAll(repeatEvent);
        }
    }

    public void keyReleased(@NonNull Key key, char key_char, @NonNull Set<@NonNull Modifier> modifiers) {
        GUIObject focused = gui_root.getGlobalFocus();
        resetKeyTimer();
        KeyboardEvent keyEvent = new KeyboardEvent(key, key_char, modifiers, 0);

        Set<GameAction> actions = Renderer.getLocalInput().getInputManager().getActions(keyEvent);
        Renderer.getLocalInput().getInputManager().updateState(keyEvent, false); // Update polling state

        InputEvent event = new InputEvent(keyEvent, actions, InputPhase.RELEASED);
        focused.handleInputAll(event);
    }

    private final class DoubleClickTimer implements Updatable<TimerAnimation> {
        @Override
        public void update(@NonNull TimerAnimation anim) {
            stopDoubleClickTimer();
        }
    }

    private final class DoubleKeyTimer implements Updatable<TimerAnimation> {
        @Override
        public void update(@NonNull TimerAnimation anim) {
            stopDoubleKeyTimer();
        }
    }

}
