package com.oddlabs.tt.form;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputBinding;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.InputManager;
import com.oddlabs.tt.input.KeyBindingConflicts;
import com.oddlabs.tt.input.Modifier;
import com.oddlabs.tt.render.Renderer;

public class KeyBindingDialog extends Form {
    // Spinner families: a base and its -1 / +10 / -10 variants. When the base is bound to a new
    // key, the variants that shared its old key follow along, each keeping its own modifiers, so
    // the family keeps a consistent layout. Variants bound to some other key are left alone, and
    // rebinding a variant changes only that variant.
    private static final Map<GameAction, GameAction[]> SPINNER_FAMILIES;
    static {
        GameAction[][] families = {{GameAction.RES_TREE, GameAction.RES_TREE_DEC, GameAction.RES_TREE_BATCH, GameAction.RES_TREE_BATCH_DEC}, {GameAction.RES_ROCK, GameAction.RES_ROCK_DEC, GameAction.RES_ROCK_BATCH, GameAction.RES_ROCK_BATCH_DEC}, {GameAction.RES_IRON, GameAction.RES_IRON_DEC, GameAction.RES_IRON_BATCH, GameAction.RES_IRON_BATCH_DEC}, {GameAction.RES_CHICKEN, GameAction.RES_CHICKEN_DEC, GameAction.RES_CHICKEN_BATCH, GameAction.RES_CHICKEN_BATCH_DEC}, {GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC, GameAction.TRAIN_PEON_BATCH, GameAction.TRAIN_PEON_BATCH_DEC}};
        Map<GameAction, GameAction[]> members = new EnumMap<>(GameAction.class);
        for (GameAction[] family : families) {
            for (GameAction member : family) {
                members.put(member, family);
            }
        }
        SPINNER_FAMILIES = members;
    }

    private final @NonNull GameAction action;
    private final @NonNull Consumer<@NonNull Set<@NonNull InputBinding>> onBindingChosen;
    private final @NonNull GUIRoot guiRoot;

    public KeyBindingDialog(@NonNull GUIRoot guiRoot, @NonNull GameAction action,
            @NonNull Consumer<@NonNull Set<@NonNull InputBinding>> onBindingChosen) {
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
            onBindingChosen.accept(Set.of());
            remove();
        });
        button_group.addChild(clear_button);

        HorizButton reset_button = new HorizButton(AbstractOptionsMenu.i18n("btn_reset"), 80);
        reset_button.addMouseClickListener((_, _, _, _) -> {
            var manager = Renderer.getLocalInput().getInputManager();
            var defaults = manager.getDefaultBindings(action);
            propagateKeyChange(manager, action, manager.getBindings(action), defaults);
            onBindingChosen.accept(defaults);
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

            boolean isModifierKey = (key == Key.LSHIFT || key == Key.RSHIFT || key == Key.LCONTROL
                    || key == Key.RCONTROL || key == Key.LALT || key == Key.RALT || key == Key.LSUPER
                    || key == Key.RSUPER);

            if (!isModifierKey && key != null && key != Key.KEY_UNKNOWN) {
                var modifiers = EnumSet.noneOf(Modifier.class);
                if (event.isShiftDown()) modifiers.add(Modifier.SHIFT);
                if (event.isAltDown()) modifiers.add(Modifier.ALT);
                if (event.isControlDown()) modifiers.add(Modifier.CONTROL);
                if (event.isMetaDown()) modifiers.add(Modifier.META);
                InputBinding binding = new InputBinding(key, modifiers, action);

                var manager = Renderer.getLocalInput().getInputManager();
                GameAction conflict = KeyBindingConflicts.findConflict(action, binding, manager);
                if (conflict != null) {
                    String otherName;
                    try {
                        otherName = AbstractOptionsMenu.i18n("action." + conflict.name());
                    } catch (Exception e) {
                        otherName = conflict.name();
                    }
                    guiRoot.addModalForm(new SwapConflictForm(otherName, conflict, binding));
                    event.consume();
                    return;
                }

                propagateKeyChange(manager, action, manager.getBindings(action), Set.of(binding));
                onBindingChosen.accept(Set.of(binding));
                remove();
                event.consume();
                return;
            }

            // Consume all pressed events to prevent bleed-through
            event.consume();
            return;
        }
        super.handleInput(event);
    }

    // Gives this action the pressed key and gives the conflicting action this action's
    // previous bindings, minus the colliding one.
    private void swapBindings(@NonNull GameAction other, @NonNull InputBinding binding) {
        var manager = Renderer.getLocalInput().getInputManager();
        Set<InputBinding> mine_old = manager.getBindings(action);
        Set<InputBinding> their_old = manager.getBindings(other);
        Set<InputBinding> theirs = new HashSet<>();
        for (InputBinding b : their_old) {
            if (b.key() != binding.key() || !b.modifiers().equals(binding.modifiers())) {
                theirs.add(b);
            }
        }
        for (InputBinding b : mine_old) {
            theirs.add(new InputBinding(b.key(), b.modifiers(), other));
        }
        manager.setBindings(other, theirs);
        // A swap within one family is a deliberate reshuffle; the follow rule would immediately
        // overwrite the swapped keys, so it only applies across families.
        if (SPINNER_FAMILIES.get(action) != SPINNER_FAMILIES.get(other)) {
            propagateKeyChange(manager, other, their_old, theirs);
            propagateKeyChange(manager, action, mine_old, Set.of(binding));
        }
        onBindingChosen.accept(Set.of(binding));
        remove();
    }

    // Rebinding a family's base off its old key drags the variants still on that key along to
    // the new one, keeping each variant's own modifiers. Variants on a different key are left as
    // they are, and rebinding a variant never drags the rest of the family.
    private static void propagateKeyChange(@NonNull InputManager manager, @NonNull GameAction changed,
            @NonNull Set<@NonNull InputBinding> old_bindings, @NonNull Set<@NonNull InputBinding> new_bindings) {
        GameAction[] family = SPINNER_FAMILIES.get(changed);
        if (family == null || family[0] != changed) {
            return;
        }
        Set<Key> old_keys = new HashSet<>();
        for (InputBinding b : old_bindings) {
            old_keys.add(b.key());
        }
        Set<Key> new_keys = new HashSet<>();
        for (InputBinding b : new_bindings) {
            new_keys.add(b.key());
        }
        Set<Key> off_keys = new HashSet<>(old_keys);
        off_keys.removeAll(new_keys);
        new_keys.removeAll(old_keys);
        if (off_keys.isEmpty() || new_keys.size() != 1) {
            return;
        }
        Key new_key = new_keys.iterator().next();
        for (GameAction member : family) {
            if (member == changed) {
                continue;
            }
            Set<InputBinding> updated = new HashSet<>();
            boolean touched = false;
            for (InputBinding b : manager.getBindings(member)) {
                if (off_keys.contains(b.key())) {
                    updated.add(new InputBinding(new_key, b.modifiers(), member));
                    touched = true;
                } else {
                    updated.add(b);
                }
            }
            if (touched) {
                manager.setBindings(member, updated);
            }
        }
    }

    private final class SwapConflictForm extends Form {
        SwapConflictForm(@NonNull String otherName, @NonNull GameAction other, @NonNull InputBinding binding) {
            LabelBox info_label = new LabelBox(AbstractOptionsMenu.i18n("conflict_swap_message", otherName),
                    Skin.getSkin().getEditFont(), 300);
            addChild(info_label);

            Group button_group = new Group();
            HorizButton swap_button = new HorizButton(AbstractOptionsMenu.i18n("btn_swap"), 80);
            swap_button.addMouseClickListener((_, _, _, _) -> {
                swapBindings(other, binding);
                remove();
            });
            button_group.addChild(swap_button);
            HorizButton cancel_button = new CancelButton(80);
            cancel_button.addMouseClickListener((_, _, _, _) -> cancel());
            button_group.addChild(cancel_button);
            swap_button.place();
            cancel_button.place(swap_button, RIGHT_MID);
            button_group.compileCanvas();
            addChild(button_group);

            info_label.place();
            button_group.place(info_label, BOTTOM_MID);

            compileCanvas();
            centerPos();
        }
    }
}
