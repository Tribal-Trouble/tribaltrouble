package com.oddlabs.tt.gui;

import java.util.ArrayList;
import java.util.List;

public final class RadioButtonGroup {
	private final List<RadioButtonGroupElement> buttons;

	public RadioButtonGroup() {
		buttons = new ArrayList<>();
	}

	public void mark(RadioButtonGroupElement button) {
		RadioButtonGroupElement marked = getMarked();
		if (marked != null)
			marked.setMarked(false);
		button.setMarked(true);
	}

	public void add(RadioButtonGroupElement button) {
		buttons.add(button);
//		button.setMarked(false);
	}

	public RadioButtonGroupElement getMarked() {
        for (RadioButtonGroupElement button : buttons) {
            if (button.isMarked())
                return button;
        }
		return null;
	}
}
