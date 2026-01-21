package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.SortedLabel;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputBinding;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class KeyBindingPanel extends Panel {
    private final @NonNull MultiColumnComboBox<GameAction> list_box;
    private final @NonNull GUIRoot gui_root;
    private final @NonNull ResourceBundle bundle;

    public KeyBindingPanel(@NonNull GUIRoot gui_root, @NonNull ResourceBundle bundle) {
        super(Utils.getBundleString(bundle, "key_bindings_title"));
        this.gui_root = gui_root;
        this.bundle = bundle;

        ColumnInfo[] infos = new ColumnInfo[]{
            new ColumnInfo(Utils.getBundleString(bundle, "column_action"), 200),
            new ColumnInfo(Utils.getBundleString(bundle, "column_bindings"), 300)
        };
        
        list_box = new MultiColumnComboBox<>(gui_root, infos, 300, false);
        addChild(list_box);
        
        updateList();

        list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowDoubleClicked(@NonNull GameAction action) {
                gui_root.addModalForm(new KeyBindingDialog(gui_root, action, binding -> {
                    List<InputBinding> newBindings = new ArrayList<>();
                    newBindings.add(binding);
                    Renderer.getLocalInput().getInputManager().setBindings(action, newBindings);
                    updateList();
                }));
            }
        });

        // Buttons
        Group button_group = new Group();
        addChild(button_group);
        
        HorizButton btn_reset = new HorizButton(Utils.getBundleString(bundle, "btn_reset_all"), 100);
        btn_reset.addMouseClickListener((_,_,_,_) -> {
            gui_root.addModalForm(new QuestionForm(Utils.getBundleString(bundle, "confirm_reset_all"), (_,_,_,_) -> {
                Renderer.getLocalInput().getInputManager().resetToDefaults();
                updateList();
            }));
        });
        button_group.addChild(btn_reset);
        
        HorizButton btn_save = new HorizButton(Utils.getBundleString(bundle, "btn_save_bindings"), 100);
        btn_save.addMouseClickListener((_,_,_,_) -> saveMappings());
        button_group.addChild(btn_save);
        
        HorizButton btn_load = new HorizButton(Utils.getBundleString(bundle, "btn_load_bindings"), 100);
        btn_load.addMouseClickListener((_,_,_,_) -> loadMappings());
        button_group.addChild(btn_load);
        
        btn_reset.place();
        btn_save.place(btn_reset, RIGHT_MID);
        btn_load.place(btn_save, RIGHT_MID);
        button_group.compileCanvas();

        list_box.place();
        button_group.place(list_box, BOTTOM_LEFT);
        
        compileCanvas();
    }

    private void updateList() {
        list_box.clear();
        for (GameAction action : GameAction.values()) {
            if (action.name().startsWith("DEBUG_") && !Settings.getSettings().inDeveloperMode()) {
                continue;
            }
            String name;
            try {
                name = Utils.getBundleString(bundle, "action." + action.name());
            } catch (Exception e) {
                name = action.name();
            }
            
            List<InputBinding> bindings = Renderer.getLocalInput().getInputManager().getBindings(action);
            boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
            String bindingStr = bindings.stream().map(b -> {
                String s = b.key().getDisplayName();
                if (isMac) {
                    if (b.meta()) s = "⌘" + s;
                    if (b.alt()) s = "⌥" + s;
                    if (b.control()) s = "⌃" + s;
                    if (b.shift()) s = "⇧" + s;
                } else {
                    if (b.shift()) s = "Shift+" + s;
                    if (b.control()) s = "Ctrl+" + s;
                    if (b.alt()) s = "Alt+" + s;
                    if (b.meta()) s = "Meta+" + s;
                }
                return s;
            }).collect(Collectors.joining(", "));
            
            Label l1 = new SortedLabel(name, action.ordinal(), Skin.getSkin().getMultiColumnComboBoxData().font());
            Label l2 = new Label(bindingStr, Skin.getSkin().getMultiColumnComboBoxData().font());
            list_box.addRow(new Row<>(new Label[]{l1, l2}, action));
        }
    }
    
    private void saveMappings() {
        boolean wasFullscreen = Settings.getSettings().fullscreen;
        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }

        String path = TinyFileDialogs.tinyfd_saveFileDialog(Utils.getBundleString(bundle, "dialog_save_bindings"), "", null, Utils.getBundleString(bundle, "json_files"));
        if (path != null) {
            String json = Renderer.getLocalInput().getInputManager().exportBindings();
            try {
                Files.writeString(Path.of(path), json);
            } catch (IOException e) {
                gui_root.addModalForm(new MessageForm(Utils.getBundleString(bundle, "error_save_failed", e.getMessage())));
            }
        }

        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }
    }
    
    private void loadMappings() {
        boolean wasFullscreen = Settings.getSettings().fullscreen;
        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }

        String path = TinyFileDialogs.tinyfd_openFileDialog(Utils.getBundleString(bundle, "dialog_load_bindings"), "", null, Utils.getBundleString(bundle, "json_files"), false);
        if (path != null) {
            try {
                String json = Files.readString(Path.of(path));
                Renderer.getLocalInput().getInputManager().importBindings(json);
                updateList();
            } catch (IOException e) {
                gui_root.addModalForm(new MessageForm(Utils.getBundleString(bundle, "error_load_failed", e.getMessage())));
            }
        }

        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }
    }
}
