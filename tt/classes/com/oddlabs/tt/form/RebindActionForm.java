package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;

/** A form for rebinding a action */
public class RebindActionForm extends Form {
    private final ResourceBundle bundle = ResourceBundle.getBundle(KeybindPanel.class.getName());
    Label current_binding_label;
    Label conflict_label;
    HorizButton done_button;
    int current_key_code;
    String changing_action_name;

    /**
     * Creates a rebind keyform setup to rebind the specified action. See Globals.KB_* constants for
     * action names.
     *
     * @param action_name
     */
    public RebindActionForm(String action_name) {
        changing_action_name = action_name;

        String displayName = Utils.getBundleString(bundle, action_name.toLowerCase());
        Label press_any_key_label =
                new Label("Press any key to rebind " + displayName, Skin.getSkin().getEditFont());
        addChild(press_any_key_label);
        press_any_key_label.place(ORIGIN_TOP_LEFT);

        current_key_code = Settings.getSettings().getKeybind(action_name);
        current_binding_label =
                new Label(Keyboard.keyToString(current_key_code), Skin.getSkin().getEditFont());
        addChild(current_binding_label);
        current_binding_label.place(press_any_key_label, BOTTOM_MID);

        int spacing = Skin.getSkin().getFormData().getObjectSpacing();
        conflict_label =
                new Label("", Skin.getSkin().getEditFont(), press_any_key_label.getWidth());
        conflict_label.setColor(new float[] {1.0f, 0.3f, 0.3f, 1.0f});
        addChild(conflict_label);
        conflict_label.place(
                press_any_key_label, BOTTOM_MID, current_binding_label.getHeight() + 2 * spacing);

        done_button = new HorizButton("Save", 70);
        done_button.addMouseClickListener(new RebindSaveListener(this));
        addChild(done_button);
        done_button.place(ORIGIN_BOTTOM_RIGHT);

        HorizButton cancel_button = new HorizButton("Cancel", 70);
        cancel_button.addMouseClickListener(new RebindCancelListener(this));
        addChild(cancel_button);
        cancel_button.place(done_button, LEFT_MID);

        compileCanvas();
        centerPos();
        updateConflictWarning();
    }

    protected final void keyPressed(KeyboardEvent event) {
        current_binding_label.set(Keyboard.keyToString(event.getKeyCode()));
        current_key_code = event.getKeyCode();
        updateConflictWarning();
    }

    private void updateConflictWarning() {
        String conflicting =
                Globals.getConflictingAction(
                        changing_action_name,
                        current_key_code,
                        Settings.getSettings().getKeybinds());
        if (conflicting != null) {
            String otherName = Utils.getBundleString(bundle, conflicting.toLowerCase());
            conflict_label.set("Conflicts with: " + otherName);
            done_button.setDisabled(true);
        } else {
            conflict_label.set("");
            done_button.setDisabled(false);
        }
    }

    public void saveKeybind() {
        System.out.println(
                "Saving keybind for action: "
                        + changing_action_name
                        + " to key code: "
                        + current_key_code
                        + " "
                        + Keyboard.keyToString(current_key_code));
        Settings.getSettings().setKeybind(changing_action_name, current_key_code);
        this.remove();
    }

    public void cancelSave() {
        remove();
    }

    private final strictfp class RebindCancelListener implements MouseClickListener {
        private final RebindActionForm form;

        public RebindCancelListener(RebindActionForm form) {
            this.form = form;
        }

        public final void mouseClicked(int button, int x, int y, int clicks) {
            form.cancel();
        }
    }

    private final strictfp class RebindSaveListener implements MouseClickListener {
        private final RebindActionForm form;

        public RebindSaveListener(RebindActionForm form) {
            this.form = form;
        }

        public final void mouseClicked(int button, int x, int y, int clicks) {
            System.out.println("saving...");
            form.saveKeybind();
        }
    }
}
