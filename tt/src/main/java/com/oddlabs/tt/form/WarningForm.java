package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

public final class WarningForm extends Form {
    private static final int MAX_WIDTH = 500;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(WarningForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull CheckBox show_next_time;

    public WarningForm(@NonNull String head, @NonNull String message) {
        int head_width = Math.min(MAX_WIDTH, Skin.getSkin().getHeadlineFont().getWidth(head));
        int message_width = Math.min(MAX_WIDTH, Skin.getSkin().getEditFont().getWidth(message));
        int width = Math.max(head_width, message_width);

        Group group = new Group();
        addChild(group);
        LabelBox head_label = new LabelBox(head, Skin.getSkin().getHeadlineFont(), width);
        group.addChild(head_label);
        LabelBox info_label = new LabelBox(message, Skin.getSkin().getEditFont(), width);
        group.addChild(info_label);
        show_next_time = new CheckBox(false, i18n("dont_show"));
        group.addChild(show_next_time);

        head_label.place();
        info_label.place(head_label, BOTTOM_LEFT);
        show_next_time.place(info_label, BOTTOM_LEFT);
        group.compileCanvas();

        HorizButton ok_button = new OKButton(70);
        addChild(ok_button);
        ok_button.addMouseClickListener((_, _, _, _) -> {
            Settings.getSettings().warning_no_sound = !show_next_time.isMarked();
            remove();
        });
        // Place objects
        group.place();
        ok_button.place(group, BOTTOM_MID);

        // headline
        compileCanvas();
        centerPos();
    }
}
