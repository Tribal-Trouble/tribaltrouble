package com.oddlabs.tt.input;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

public final class InputEvent {
    private final @NonNull Set<@NonNull GameAction> actions;
    private final @NonNull InputPhase phase;
    private final char character;
    private final boolean shiftDown;
    private final boolean controlDown;
    private final boolean altDown;
    private final boolean metaDown;
    private final int clicks;

    // Legacy support: We still track the physical key for low-level logic (e.g. EditLine raw keys)
    private final @Nullable Key keyCode;

    private boolean consumed;

    public InputEvent(@NonNull KeyboardEvent keyboardEvent, @NonNull Set<@NonNull GameAction> actions, @NonNull InputPhase phase) {
        this.actions = EnumSet.copyOf(actions);
        this.phase = phase;
        this.keyCode = keyboardEvent.keyCode();
        this.character = keyboardEvent.keyChar();
        this.shiftDown = keyboardEvent.shiftDown();
        this.controlDown = keyboardEvent.controlDown();
        this.altDown = keyboardEvent.altDown();
        this.metaDown = keyboardEvent.metaDown();
        this.clicks = keyboardEvent.clicks();
        this.consumed = false;
    }

    public @NonNull Set<GameAction> getActions() {
        return actions;
    }

    public @NonNull InputPhase getPhase() {
        return phase;
    }

    public char getCharacter() {
        return character;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public boolean isControlDown() {
        return controlDown;
    }

    public boolean isAltDown() {
        return altDown;
    }

    public boolean isMetaDown() {
        return metaDown;
    }

    public int getClicks() {
        return clicks;
    }

    /**
     * Returns the physical key code, if this event originated from a keyboard.
     */
    public @Nullable Key getKeyCode() {
        return keyCode;
    }

    /**
     * Returns true if the physical event (key press) has been fully consumed and should stop propagating.
     * <p>
     * Note: This is distinct from consuming specific {@link GameAction}s. An event can have actions removed
     * (via {@link #consumeAction}) but still continue bubbling to allow other handlers to process remaining
     * actions or raw input (distributed handling). Calling {@link #consume()} stops propagation immediately
     * (exclusive handling).
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Marks the physical event as fully handled, stopping further propagation.
     */
    public void consume() {
        this.consumed = true;
    }

    /**
     * @return true if there are unconsumed actions
     */
    public boolean hasActions() {
        return !actions.isEmpty();
    }

    public boolean hasAction(@NonNull GameAction action) {
        return actions.contains(action);
    }

    /**
     * Removes a specific logical action from this event, but allows propagation to continue.
     */
    public boolean consumeAction(@NonNull GameAction action) {
        return actions.remove(action);
    }
}