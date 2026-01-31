package com.oddlabs.tt.form;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.SortedLabel;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputBinding;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class KeyBindingPanel extends Panel {
    private static final int COL_ACTION_WIDTH = 200;
    private static final int COL_BINDINGS_WIDTH = 300;

    private final @NonNull MultiColumnComboBox<GameAction> list_box;
    private final @NonNull GUIRoot gui_root;

    public KeyBindingPanel(@NonNull GUIRoot gui_root) {
        super(AbstractOptionsMenu.i18n("key_bindings_title"));
        this.gui_root = gui_root;

        ColumnInfo[] infos = new ColumnInfo[]{
            new ColumnInfo(AbstractOptionsMenu.i18n("column_action"), COL_ACTION_WIDTH),
            new ColumnInfo(AbstractOptionsMenu.i18n("column_bindings"), COL_BINDINGS_WIDTH)
        };
        
        list_box = new MultiColumnComboBox<>(gui_root, infos, 300, false);
        addChild(list_box);
        
        updateList();

        list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowDoubleClicked(@NonNull GameAction action) {
                gui_root.addModalForm(new KeyBindingDialog(gui_root, action, bindings -> {
                    Renderer.getLocalInput().getInputManager().setBindings(action, bindings);
                    updateList();
                }));
            }
        });

        // Buttons
        Group button_group = new Group();
        addChild(button_group);
        
        HorizButton btn_reset = new HorizButton(AbstractOptionsMenu.i18n("btn_reset_all"), 100);
        btn_reset.addMouseClickListener((_,_,_,_) -> gui_root.addModalForm(new QuestionForm(AbstractOptionsMenu.i18n("confirm_reset_all"), (_, _, _, _) -> {
            Renderer.getLocalInput().getInputManager().resetToDefaults();
            updateList();
        })));
        button_group.addChild(btn_reset);
        
        HorizButton btn_save = new HorizButton(AbstractOptionsMenu.i18n("btn_save_bindings"), 100);
        btn_save.addMouseClickListener((_,_,_,_) -> saveMappings());
        button_group.addChild(btn_save);
        
        HorizButton btn_load = new HorizButton(AbstractOptionsMenu.i18n("btn_load_bindings"), 100);
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
            if (action.name().startsWith("CHEAT_") && !Renderer.getRenderer().isCheater()) {
                continue;
            }
            String name;
            try {
                name = AbstractOptionsMenu.i18n("action." + action.name());
            } catch (Exception e) {
                name = action.name();
            }
            
            List<InputBinding> bindings = Renderer.getLocalInput().getInputManager().getBindings(action);
            Label l2;
            
            if (bindings.isEmpty()) {
                l2 = new InvertedLabel(AbstractOptionsMenu.i18n("unassigned"), Skin.getSkin().getMultiColumnComboBoxData().font(), COL_BINDINGS_WIDTH);
            } else {
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
                l2 = new Label(bindingStr, Skin.getSkin().getMultiColumnComboBoxData().font());
            }
            
            Label l1 = new SortedLabel(name, action.ordinal(), Skin.getSkin().getMultiColumnComboBoxData().font());
            list_box.addRow(new Row<>(new Label[]{l1, l2}, action));
        }
    }
    
    private void saveMappings() {
        boolean wasFullscreen = Settings.getSettings().fullscreen;
        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }

        String path = TinyFileDialogs.tinyfd_saveFileDialog(AbstractOptionsMenu.i18n("dialog_save_bindings"), "", null, AbstractOptionsMenu.i18n("json_files"));
        if (path != null) {
            String json = Renderer.getLocalInput().getInputManager().exportBindings();
            try {
                Files.writeString(Path.of(path), json);
            } catch (IOException e) {
                gui_root.addModalForm(new MessageForm(AbstractOptionsMenu.i18n("error_save_failed", e.getMessage())));
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

        String path = TinyFileDialogs.tinyfd_openFileDialog(AbstractOptionsMenu.i18n("dialog_load_bindings"), "", null, AbstractOptionsMenu.i18n("json_files"), false);
        if (path != null) {
            try {
                String json = Files.readString(Path.of(path));
                Renderer.getLocalInput().getInputManager().importBindings(json);
                updateList();
            } catch (IOException e) {
                gui_root.addModalForm(new MessageForm(AbstractOptionsMenu.i18n("error_load_failed", e.getMessage())));
            }
        }

        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }
    }

    private static final class InvertedLabel extends Label {
        public InvertedLabel(@NonNull String text, @NonNull Font font, int width) {
            super(text, font, width, Origin.AT_MIDDLE);
            setColor(Color.BLACK);
        }

        @Override
        protected void renderGeometry(@NonNull GUIRenderer renderer) {
            renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), Label.DEFAULT_COLOR);
            super.renderGeometry(renderer);
        }
    }
}
