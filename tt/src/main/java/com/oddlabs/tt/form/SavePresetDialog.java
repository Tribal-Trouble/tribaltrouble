package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.Set;

import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public final class SavePresetDialog extends Form {
    private static final int BUTTON_WIDTH = 100;
    private static final int EDITLINE_WIDTH = 240;
    private static final int MAX_NAME_LENGTH = 64;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(SavePresetDialog.class.getName());

    @FunctionalInterface
    public interface SaveListener {
        void save(@NonNull String name);
    }

    private final @NonNull Set<@NonNull String> existing_names;
    private final @NonNull SaveListener listener;
    private final @NonNull EditLine editline_name;

    public SavePresetDialog(@NonNull Set<@NonNull String> existing_names, @NonNull SaveListener listener) {
        super(i18n("save_preset_caption"));
        this.existing_names = existing_names;
        this.listener = listener;

        Label label_name = new Label(i18n("preset_name"), Skin.getSkin().getEditFont());
        editline_name = new EditLine(EDITLINE_WIDTH, MAX_NAME_LENGTH);
        editline_name.addEnterListener(_ -> submit());

        HorizButton button_ok = new OKButton(BUTTON_WIDTH);
        button_ok.addMouseClickListener((_, _, _, _) -> submit());
        HorizButton button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> this.cancel());

        addChild(label_name);
        addChild(editline_name);
        addChild(button_ok);
        addChild(button_cancel);

        label_name.place();
        editline_name.place(label_name, RIGHT_MID);
        button_cancel.place(editline_name, BOTTOM_RIGHT);
        button_ok.place(button_cancel, LEFT_MID);

        compileCanvas();
        centerPos();
    }

    private static @NonNull String i18n(@NonNull String key) {
        return Utils.getBundleString(bundle, key);
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            editline_name.setFocus(direction);
        }
    }

    private void submit() {
        String name = editline_name.getContents().trim();
        if (name.isEmpty() || existing_names.contains(name)) {
            editline_name.triggerError();
            return;
        }
        remove();
        listener.save(name);
    }
}
