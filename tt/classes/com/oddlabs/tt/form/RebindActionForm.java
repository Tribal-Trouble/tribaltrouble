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
    // Simplified UI: no extra input boxes, capture keypress only

    /**
     * Creates a rebind keyform setup to rebind the specified action. See Globals.KB_* constants for
     * action names.
     *
     * @param action_name
     */
    public RebindActionForm(String action_name) {
        changing_action_name = action_name;
        // Compact form, as before
        setDim(320, 120);
        // Place controls tat should be placed via origin
        Label press_any_key_label =
                new Label(
                        "Press any key to rebind "
                                + KeybindPanel.KEYBIND_DISPLAY_NAMES.getOrDefault(
                                        action_name, action_name),
                        Skin.getSkin().getEditFont());
        addChild(press_any_key_label);
        press_any_key_label.place(ORIGIN_TOP_LEFT);

        HorizButton done_button = new HorizButton("Save", 90);
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
    // No text inputs; simply press a key then click Save
    }

    protected final void keyPressed(KeyboardEvent event) {
        // Capture physical key presses to update the current binding preview
        current_binding_label.set(Keyboard.keyToString(event.getKeyCode()));
        current_key_code = event.getKeyCode();
    // Consume so parent/menus don't act on it
        event.consume(); // prevent parent/menus from also handling this key
    }

    // keyReleased/keyRepeat in Form are final; rely on EditLine's EnterListener instead.

    public void saveKeybind() {
        // Apply the last captured key
        int codeToApply = current_key_code;
        System.out.println(
                "Saving keybind for action: "
                        + changing_action_name
                        + " to key code: "
                        + codeToApply
                        + " ("
                        + Keyboard.keyToString(codeToApply)
                        + ")");
        Settings.getSettings().setKeybind(changing_action_name, codeToApply);
        // Persist immediately so changes survive and reflect elsewhere
        Settings.getSettings().save();
        this.remove();
    }

    public void cancelSave() {
        remove();
    }

    private final class RebindSaveListener implements MouseClickListener {
        private final RebindActionForm form;

        public RebindSaveListener(RebindActionForm form) {
            this.form = form;
        }

    @Override
    public final void mouseClicked(int button, int x, int y, int clicks) {
            System.out.println("saving...");
            form.saveKeybind();
        }
    }

    // No error UI needed in simplified version
}
