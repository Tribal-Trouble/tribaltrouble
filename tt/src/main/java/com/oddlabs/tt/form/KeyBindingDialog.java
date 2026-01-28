package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputBinding;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class KeyBindingDialog extends Form {
    private final @NonNull GameAction action;
    private final @NonNull Consumer<List<InputBinding>> onBindingChosen;
    private final @NonNull GUIRoot guiRoot;

    public KeyBindingDialog(@NonNull GUIRoot guiRoot, @NonNull GameAction action, @NonNull Consumer<List<InputBinding>> onBindingChosen) {
        this.guiRoot = guiRoot;
        this.action = action;
        this.onBindingChosen = onBindingChosen;
        
        String actionName;
        try {
            actionName = AbstractOptionsMenu.i18n("action." + action.name());
        } catch (Exception e) {
            actionName = action.name();
        }
        
        LabelBox info_label = new LabelBox("Press key for: " + actionName, Skin.getSkin().getEditFont(), 300);
        addChild(info_label);
        
        Group button_group = new Group();
        addChild(button_group);

        HorizButton clear_button = new HorizButton(AbstractOptionsMenu.i18n("btn_clear"), 80);
        clear_button.addMouseClickListener((_, _, _, _) -> {
            onBindingChosen.accept(Collections.emptyList());
            remove();
        });
        button_group.addChild(clear_button);

        HorizButton reset_button = new HorizButton(AbstractOptionsMenu.i18n("btn_reset"), 80);
        reset_button.addMouseClickListener((_, _, _, _) -> {
            onBindingChosen.accept(Renderer.getLocalInput().getInputManager().getDefaultBindings(action));
            remove();
        });
        button_group.addChild(reset_button);
        
        HorizButton cancel_button = new CancelButton(80);
        cancel_button.addMouseClickListener((_, _, _, _) -> cancel());
        button_group.addChild(cancel_button);
        
        // Place objects
        info_label.place();
        clear_button.place();
        reset_button.place(clear_button, RIGHT_MID);
        cancel_button.place(reset_button, RIGHT_MID);
        button_group.compileCanvas();
        button_group.place(info_label, BOTTOM_MID);
        
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
                onBindingChosen.accept(List.of(binding));
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