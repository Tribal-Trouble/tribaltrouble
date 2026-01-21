package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputBinding;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.function.Consumer;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

public class KeyBindingDialog extends Form {
    private final @NonNull GameAction action;
    private final @NonNull Consumer<InputBinding> onBindingChosen;
    private final @NonNull GUIRoot guiRoot;

    public KeyBindingDialog(@NonNull GUIRoot guiRoot, @NonNull GameAction action, @NonNull Consumer<InputBinding> onBindingChosen) {
        this.guiRoot = guiRoot;
        this.action = action;
        this.onBindingChosen = onBindingChosen;
        
        ResourceBundle bundle = ResourceBundle.getBundle(OptionsMenu.class.getName());
        String actionName;
        try {
            actionName = Utils.getBundleString(bundle, "action." + action.name());
        } catch (Exception e) {
            actionName = action.name();
        }
        
        LabelBox info_label = new LabelBox("Press key for: " + actionName, Skin.getSkin().getEditFont(), 300);
        addChild(info_label);
        
        HorizButton cancel_button = new CancelButton(80);
        cancel_button.addMouseClickListener((_, _, _, _) -> cancel());
        addChild(cancel_button);
        
        // Place objects
        info_label.place();
        cancel_button.place(info_label, BOTTOM_MID);
        
        compileCanvas();
        centerPos();
        setCanFocus(true);
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED) {
            Key key = event.getKeyCode();
            
            if (event.hasAction(GameAction.GLOBAL_QUIT)) {
                guiRoot.addModalForm(new QuitForm(guiRoot));
                event.consume();
                return;
            }

            boolean isModifierKey = (key == Key.LSHIFT || key == Key.RSHIFT || key == Key.LCONTROL || key == Key.RCONTROL || key == Key.LALT || key == Key.RALT || key == Key.LSUPER || key == Key.RSUPER);
            
            if (!isModifierKey && key != null && key != Key.KEY_UNKNOWN && key != Key.ESCAPE) {
                InputBinding binding = new InputBinding(key, event.isShiftDown(), event.isControlDown(), event.isAltDown(), event.isMetaDown(), action);
                onBindingChosen.accept(binding);
                remove();
                event.consume();
                return;
            } else if (key == Key.ESCAPE) {
                cancel();
                event.consume();
                return;
            }
            
            // Consume all pressed events to prevent bleed-through
            event.consume();
            return;
        }
        super.handleInput(event);
    }
}