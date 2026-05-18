package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.DoNowListener;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Origin.AT_END;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;

public final class DisplayChangeForm extends Form {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(DisplayChangeForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull DoNowListener donow_listener;
    private final @NonNull HorizButton later_button;

    public DisplayChangeForm(@NonNull DoNowListener donow_listener) {
        this.donow_listener = donow_listener;
        LabelBox info_label = new LabelBox(i18n("warning_message"), Skin.getSkin().getEditFont(), 500);
        addChild(info_label);
        HorizButton now_button = new HorizButton(i18n("now"), 120);
        addChild(now_button);
        now_button.addMouseClickListener((_, _, _, _) -> {
            remove();
            donow_listener.doChange(true);
        });
        later_button = new HorizButton(i18n("later"), 120);
        addChild(later_button);
        later_button.addMouseClickListener((_, _, _, _) -> this.cancel());

        // Place objects
        info_label.place();
        now_button.place(AT_END);
        later_button.place(now_button, LEFT_MID);

        compileCanvas();
        centerPos();
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            later_button.setFocus(direction);
        }
    }

    @Override
    protected void doCancel() {
        donow_listener.doChange(false);
    }
}
