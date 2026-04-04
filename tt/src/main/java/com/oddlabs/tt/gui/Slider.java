package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import com.oddlabs.tt.guievent.ValueListener;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class Slider extends GUIObject {
    private final Set<@NonNull ValueListener> value_listeners = new CopyOnWriteArraySet<>();
    private final Set<@NonNull Runnable> release_listeners = new CopyOnWriteArraySet<>();

    private final @NonNull SliderButton button;
    private final int left_offset;
    private final int cardinality;
    private final float step;
    private final int min;
    private int value;

    public Slider(int width, int min, int max, int init_value) {
        cardinality = max - min + 1;
        assert cardinality > 0 && max >= init_value && init_value >= min : "Invalid values. cardinality = " + cardinality + " | max = " + max + " | min = " + min + " | init_value = " + init_value;
        this.min = min;
        left_offset = Skin.getSkin().getSliderData().leftOffset();
        int right_offset = Skin.getSkin().getSliderData().rightOffset();
        setDim(width, Skin.getSkin().getSliderData().slider().getHeight());
        setCanFocus(true);

        button = new SliderButton(this, Skin.getSkin().getSliderData().button());
        step = (getWidth() - left_offset - right_offset - button.getWidth()) / (float) (cardinality - 1);
        setValue(init_value);
        addChild(button);

        DragListener drag_listener = new DragListener();
        button.addMouseMotionListener(drag_listener);
        button.addMouseButtonListener(drag_listener);
    }

    public void addReleaseListener(@NonNull Runnable listener) {
        release_listeners.add(listener);
    }

    private void notifyRelease() {
        for (Runnable r : release_listeners) {
            r.run();
        }
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        Skin.getSkin().getSliderData().slider()
                .render(renderer, 0, 0, getWidth(), isDisabled() ? ModeIconQuads.Mode.DISABLED : ModeIconQuads.Mode.NORMAL);
    }

    public int getValue() {
        return min + value;
    }

    private int valueToOffset(int value) {
        return (int) (value * step) + left_offset;
    }

    private int offsetToValue(int offset) {
        return (int) (offset / step + .5f);
    }

    @Override
    public void mouseHeld(@NonNull MouseButton button, int x, int y) {
        mousePressed(button, x, y);
    }

    @Override
    public void mouseScrolled(int amount) {
        if (!isDisabled()) {
            if (amount < 0)
                setValue(value - 1 + min);
            else
                setValue(value + 1 + min);
            button.setFocus();
        }
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
        if (!isDisabled()) {
            int dx = x - this.button.getX();
            if (dx < -step / 2)
                setValue(value - 1 + min);
            else if (dx > step / 2)
                setValue(value + 1 + min);
            this.button.setFocus();
            // notifyRelease(); // Should this fire on press? No, usually Release for "Commit".
            // But for step click, maybe Release is better.
        }
    }

    @Override
    public void mouseReleased(@NonNull MouseButton button, int x, int y) {
        if (!isDisabled()) {
            notifyRelease();
        }
    }

    public void setValue(int value) {
        int start_value = this.value;
        this.value = value - min;
        cropValue();
        button.setPos(valueToOffset(this.value), 0);
        if (start_value != this.value)
            valueSetAll(getValue());
    }

    private void cropValue() {
        if (value < 0) {
            value = 0;
        } else if (value > cardinality - 1) {
            value = cardinality - 1;
        }
    }

    public void valueSetAll(int value) {
        valueSet(value);
        for (ValueListener listener : value_listeners) {
            listener.valueSet(value);
        }
    }

    void valueSet(int value) {
/*		
		GUIObject parent = (GUIObject)getParent();
		if (parent != null)
			parent.valueSetAll(value);
*/
    }

    public void addValueListener(@NonNull ValueListener listener) {
        value_listeners.add(listener);
    }

    public void removeValueListener(@NonNull ValueListener listener) {
        value_listeners.remove(listener);
    }

    private final class DragListener implements MouseMotionListener, MouseButtonListener {
        final SliderData data = Skin.getSkin().getSliderData();
        float start_offset;

        @Override
        public void mousePressed(@NonNull MouseButton button, int x, int y) {
            start_offset = valueToOffset(value);
        }

        @Override
        public void mouseDragged(@NonNull MouseButton button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
            if (!isDisabled()) {
                setValue(offsetToValue((int) (start_offset + abs_x)) + min);
            }
        }

        @Override
        public void mouseMoved(int x, int y) {
        }

        @Override
        public void mouseEntered() {
        }

        @Override
        public void mouseExited() {
        }

        @Override
        public void mouseReleased(@NonNull MouseButton button, int x, int y) {
            notifyRelease();
        }

        @Override
        public void mouseHeld(@NonNull MouseButton button, int x, int y) {
        }

        @Override
        public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        }
    }
}
