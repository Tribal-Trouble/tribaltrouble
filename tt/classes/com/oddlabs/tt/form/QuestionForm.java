package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.guievent.*;

public class QuestionForm extends Form {
	private final HorizButton yes_button;

	public QuestionForm(String message, MouseClickListener yes_action) {
		int message_width = Skin.getSkin().getEditFont().getWidth(message);
		LabelBox info_label = new LabelBox(message, Skin.getSkin().getEditFont(), StrictMath.min(400, message_width));
		addChild(info_label);
		Group button_group = new Group();
		yes_button = new OKButton(80);
		yes_button.addMouseClickListener(new OKListener(this));
		yes_button.addMouseClickListener(yes_action);
		button_group.addChild(yes_button);
		HorizButton no_button = new CancelButton(80);
		no_button.addMouseClickListener((int button, int x, int y, int clicks) -> {
			this.cancel();
        });
		button_group.addChild(no_button);
		yes_button.place();
		no_button.place(yes_button, RIGHT_MID);
		button_group.compileCanvas();
		addChild(button_group);

		// Place objects
		info_label.place();
		button_group.place(info_label, BOTTOM_MID);

		compileCanvas();
		centerPos();
	}

        @Override
	public final void setFocus() {
		yes_button.setFocus();
	}

	public final void connectionLost() {
		remove();
	}
}
