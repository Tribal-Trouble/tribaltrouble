package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.input.Keyboard;

/** A form for rebinding a action */
public class RebindActionForm extends Form {
    Label current_binding_label;
    int current_key_code;
    String changing_action_name;
    private EditLine current_code_field; // copy-friendly numeric code
    private EditLine new_code_field; // accepts pasted name or numeric code
    private Label error_label;

    /**
     * Creates a rebind keyform setup to rebind the specified action. See Globals.KB_* constants for
     * action names.
     *
     * @param action_name
     */
    public RebindActionForm(String action_name) {
        changing_action_name = action_name;
    // Manually set dimensions of the form (taller to fit fields)
    setDim(360, 180);
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

    // Current code field (copyable)
    Label current_code_label = new Label("Current code:", Skin.getSkin().getEditFont());
    addChild(current_code_label);
    current_code_label.place(current_binding_label, BOTTOM_LEFT);
    current_code_field = new EditLine(140, 16);
    addChild(current_code_field);
    current_code_field.place(current_code_label, RIGHT_MID, 8);
    current_code_field.set(Short.toString((short) current_key_code));

    // New code input
    Label new_code_label = new Label("New code:", Skin.getSkin().getEditFont());
    addChild(new_code_label);
    new_code_label.place(current_code_label, BOTTOM_LEFT);
    new_code_field = new EditLine(200, 32);
    addChild(new_code_field);
    new_code_field.place(new_code_label, RIGHT_MID, 8);

    // Error label (hidden until needed)
    error_label = new Label("", Skin.getSkin().getEditFont());
    addChild(error_label);
    error_label.place(new_code_label, BOTTOM_LEFT);
    setError(null);

        // Center the form on screen
        centerPos();
    }

    protected final void keyPressed(KeyboardEvent event) {
    // Capture physical key presses to update the current binding preview
    current_binding_label.set(Keyboard.keyToString(event.getKeyCode()));
    current_key_code = event.getKeyCode();
    current_code_field.set(Short.toString((short) current_key_code));
    setError(null);
    event.consume(); // prevent parent/menus from also handling this key
    }

    public void saveKeybind() {
    // Prefer explicit new code input if provided; otherwise use last captured key press
    String input = new_code_field.getContents().trim();
    int codeToApply = current_key_code;
    if (!input.isEmpty()) {
        Integer parsed = parseKey(input);
        if (parsed == null) {
        setError("Invalid code. Use a number or key name.");
        return;
        }
        codeToApply = parsed;
    }

    System.out.println(
        "Saving keybind for action: "
            + changing_action_name
            + " to key code: "
            + codeToApply
            + " ("
            + Keyboard.keyToString(codeToApply)
            + ")");
    Settings.getSettings().setKeybind(changing_action_name, codeToApply);
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

    private void setError(String message) {
        if (message == null || message.isEmpty()) {
            error_label.set("");
        } else {
            error_label.set(message);
        }
    }

    private Integer parseKey(String token) {
        if (token == null) return null;
        token = token.trim();
        // Try integer first
        try {
            int v = Integer.parseInt(token);
            if (v >= 0 && v < Keyboard.KEYBOARD_SIZE) return v;
        } catch (NumberFormatException ignore) {
            // not a pure number
        }
        // Try by display name via helper
        int k = Keyboard.stringToKey(token);
        if (k != Keyboard.KEY_NONE) return k;
        return null;
    }
}
