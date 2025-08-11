package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.input.Keyboard;

/** A form for rebinding a action */
public class RebindActionForm extends Form {
    Label current_binding_label;
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
        // Manually set dimensions of the form
        setDim(300, 100);
        // Place controls tat should be placed via origin
        Label press_any_key_label =
                new Label("Press any key to rebind " + action_name, Skin.getSkin().getEditFont());
        addChild(press_any_key_label);
        press_any_key_label.place(ORIGIN_TOP_LEFT);

        HorizButton done_button = new HorizButton("Save", 70);
        done_button.addMouseClickListener(new RebindSaveListener(this));
        addChild(done_button);
        done_button.place(ORIGIN_BOTTOM_RIGHT);

        compileCanvas(8, 16, 16, 8, false);
        press_any_key_label.setPos(0, getHeight() / 2);
        current_key_code = Settings.getSettings().getKeybind(action_name);
        current_binding_label =
                new Label(Keyboard.keyToString(current_key_code), Skin.getSkin().getEditFont());
        addChild(current_binding_label);

        // because we used .setPos on press_any_key_label before this. This will place
        // another_label relative to it.
        // even though we haven't used compile canvas yet

        // Place controls relative to origin placed controls
        current_binding_label.place(press_any_key_label, BOTTOM_LEFT);

        // Center the form on screen
        centerPos();
    }

    protected final void keyPressed(KeyboardEvent event) {
        current_binding_label.set(Keyboard.keyToString(event.getKeyCode()));
        current_key_code = event.getKeyCode();
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
