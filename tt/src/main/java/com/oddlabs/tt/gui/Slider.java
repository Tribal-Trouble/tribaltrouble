package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseButtonListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import com.oddlabs.tt.guievent.ValueListener;

public final class Slider extends GUIObject {
	private final java.util.List<ValueListener> value_listeners = new java.util.ArrayList<>();

	private final SliderButton button;
	private final int left_offset;
	private final int right_offset;
	private final int cardinality;
	private final float step;
	private final int min;
	private int value;

	public Slider(int width, int min, int max, int init_value) {
		cardinality = max - min + 1;
		assert cardinality > 0 && max >= init_value && init_value >= min: "Invalid values. cardinality = " + cardinality + " | max = " + max + " | min = " + min + " | init_value = " + init_value;
		this.min = min;
		left_offset = Skin.getSkin().getSliderData().getLeftOffset();
		right_offset = Skin.getSkin().getSliderData().getRightOffset();
		setDim(width, Skin.getSkin().getSliderData().getSlider().getHeight());
		setCanFocus(true);

		button = new SliderButton(this, Skin.getSkin().getSliderData().getButton());
		step = (getWidth() - left_offset - right_offset - button.getWidth())/(float)(cardinality - 1);
		setValue(init_value);
		addChild(button);

		DragListener drag_listener = new DragListener();
		button.addMouseMotionListener(drag_listener);
		button.addMouseButtonListener(drag_listener);
	}

        @Override
	protected void renderGeometry() {
		Horizontal slider = Skin.getSkin().getSliderData().getSlider();
		if (isDisabled())
			slider.render(0, 0, getWidth(), Skin.DISABLED);
		else
			slider.render(0, 0, getWidth(), Skin.NORMAL);
	}

	public int getValue() {
		return min + value;
	}

	private int valueToOffset(int value) {
		return (int)(value*step) + left_offset;
	}

	private int offsetToValue(int offset) {
		return (int)(offset/step + .5f);
	}
	
        @Override
	public void mouseHeld(int button, int x, int y) {
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
	public void mousePressed(int button, int x, int y) {
		if (!isDisabled()) {
			int dx = x - this.button.getX();
			if (dx < -step/2)
				setValue(value - 1 + min);
			else if (dx > step/2)
				setValue(value + 1 + min);
			this.button.setFocus();
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
            if (listener != null)
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

	public void addValueListener(ValueListener listener) {
		value_listeners.add(listener);
	}

	public void removeValueListener(ValueListener listener) {
		value_listeners.remove(listener);
	}

	private final class DragListener implements MouseMotionListener, MouseButtonListener {
		private int start_offset;

                @Override
		public void mousePressed(int button, int x, int y) {
			start_offset = valueToOffset(value);
		}

                @Override
		public void mouseDragged(int button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
			if (!isDisabled()) {
				setValue(offsetToValue(start_offset + abs_x) + min);
			}
		}

                @Override
		public void mouseMoved(int x, int y) {}
                @Override
		public void mouseEntered() {}
                @Override
		public void mouseExited() {}
                @Override
		public void mouseReleased(int button, int x, int y) {}
                @Override
		public void mouseHeld(int button, int x, int y) {}
                @Override
		public void mouseClicked(int button, int x, int y, int clicks) {}
	}
}
