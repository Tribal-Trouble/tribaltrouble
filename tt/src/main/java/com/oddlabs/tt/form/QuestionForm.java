package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.OKListener;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import org.jspecify.annotations.NonNull;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class QuestionForm extends Form {
    private final @NonNull HorizButton yes_button;

    public QuestionForm(@NonNull String message, @NonNull MouseClickListener yes_action) {
        int message_width = Skin.getSkin().getEditFont().getWidth(message);
        LabelBox info_label = new LabelBox(message, Skin.getSkin().getEditFont(), Math.min(400, message_width));
        addChild(info_label);
        Group button_group = new Group();
        yes_button = new OKButton(80);
        yes_button.addMouseClickListener(new OKListener(this));
        yes_button.addMouseClickListener(yes_action);
        button_group.addChild(yes_button);
        HorizButton no_button = new CancelButton(80);
        no_button.addMouseClickListener((_, _, _, _) -> this.cancel());
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
