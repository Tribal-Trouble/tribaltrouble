package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.ValueListener;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class NumberEditLine extends EditLine {
    private final Set<@NonNull ValueListener> value_listeners = new CopyOnWriteArraySet<>();

    private final long min_value;
    private final long max_value;

    private long value;

    public NumberEditLine(int width, int max_chars, int max_value) {
        this(width, max_chars, 0, max_value, 0);
    }

    public NumberEditLine(int width, int max_chars, int min_value, int max_value, int init_value) {
        super(width, max_chars, "0123456789", Origin.AT_END);
        this.min_value = min_value;
        this.max_value = max_value;
        setValue(init_value);
    }

    public final void addValueListener(@NonNull ValueListener listener) {
        value_listeners.add(listener);
    }

    public final void removeValueListener(@NonNull ValueListener listener) {
        value_listeners.remove(listener);
    }

    @Override
    protected final void enterPressed(@NonNull CharSequence text) {
        validate();
    }

    private void validate() {
        String str = getText().toString();

        long value;
        try {
            value = crop(Long.parseLong(str));
        } catch (NumberFormatException _) {
            // ignore exception, assume minimum value
            value = min_value;
        }
        setValue(value);
    }

    private long crop(long value) {
        return Math.clamp(value, min_value, max_value);
    }

    public final void setValue(long value) {
        clear();
        value = crop(value);
        append(value);

        if (value != this.value) {
            this.value = value;
            for (ValueListener listener : value_listeners) {
                listener.valueSet(this.value);
            }
        }
    }

    public final long getValue() {
        validate();
        return value;
    }

    @Override
    protected void focusNotify(boolean focus) {
        validate();
        super.focusNotify(focus);
    }
}
