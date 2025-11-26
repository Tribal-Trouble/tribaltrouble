package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RadioButtonGroup {
	private final List<@NonNull RadioButtonGroupElement> buttons = new ArrayList<>();

	public RadioButtonGroup() {
	}

	public void mark(@NonNull RadioButtonGroupElement button) {
		RadioButtonGroupElement marked = getMarked();
		if (marked != null)
			marked.setMarked(false);
		button.setMarked(true);
	}

	public void add(@NonNull RadioButtonGroupElement button) {
		buttons.add(button);
//		button.setMarked(false);
	}

	public @Nullable RadioButtonGroupElement getMarked() {
        for (RadioButtonGroupElement button : buttons) {
            if (button.isMarked())
                return button;
        }
		return null;
	}
}
