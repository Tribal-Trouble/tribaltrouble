package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.CloseListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class KeybindPanel extends Panel {
    GUIRoot gui_root;
    private final ResourceBundle bundle = ResourceBundle.getBundle(KeybindPanel.class.getName());
    MultiColumnComboBox keybinds_list_box;

    public KeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        this.gui_root = gui_root;

        Label keybinds_label =
                new Label(Utils.getBundleString(bundle, "keybinds"), Skin.getSkin().getEditFont());
        keybinds_label.place();

        // Add all the controls to a group
        Group keybinds_group = new Group();
        keybinds_group.addChild(keybinds_label);

        ColumnInfo[] keybind_options = new ColumnInfo[] {new ColumnInfo("", 400)};
        keybinds_list_box = new MultiColumnComboBox(gui_root, keybind_options, 200, false);
        keybinds_list_box.place(keybinds_label, BOTTOM_LEFT);
        keybinds_list_box.addRowListener(new KeybindListener());

        evaluateKeybindRows();

        HorizButton button_reset_keybinds =
                new HorizButton(Utils.getBundleString(bundle, "reset_keybinds"), 120);
        button_reset_keybinds.place(keybinds_list_box, BOTTOM_LEFT);
        button_reset_keybinds.addMouseClickListener(new ResetKeybindsListener());

        keybinds_group.addChild(keybinds_list_box);
        keybinds_group.addChild(button_reset_keybinds);
        keybinds_group.compileCanvas();
        keybinds_group.place();

        addChild(keybinds_group);
        this.compileCanvas();
    }

    private static final float[] HEADER_COLOR = {0.9f, 0.75f, 0.4f, 1.0f};

    private String getCategoryName(String actionName) {
        String key = actionName.toLowerCase();
        if (key.contains("pan_camera") || key.startsWith("kb_camera_")) return "Camera";
        if (key.equals("kb_attack") || key.equals("kb_gather_repair") || key.equals("kb_move"))
            return "Unit";
        if (key.startsWith("kb_build_")) return "Build";
        if (key.startsWith("kb_armory_")) return "Armory";
        if (key.startsWith("kb_quarters_")) return "Quarters";
        if (key.startsWith("kb_chieftain_")) return "Chieftain";
        if (key.startsWith("kb_tower_")) return "Tower";
        if (key.startsWith("kb_army_group_")) return "Army Groups";
        if (key.startsWith("kb_gamespeed_")) return "Game Speed";
        return "Game";
    }

    private void evaluateKeybindRows() {
        keybinds_list_box.setAutoSort(false);
        HashMap<String, Integer> keybinds = Settings.getSettings().getKeybinds();

        // Group keybinds by category derived from action key
        TreeMap<String, List<Map.Entry<String, Integer>>> grouped = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : keybinds.entrySet()) {
            grouped.computeIfAbsent(getCategoryName(entry.getKey()), k -> new ArrayList<>())
                    .add(entry);
        }

        int categoryIndex = 0;
        for (Map.Entry<String, List<Map.Entry<String, Integer>>> group : grouped.entrySet()) {
            // Sort entries within category by display name
            group.getValue()
                    .sort(
                            (e1, e2) -> {
                                String d1 =
                                        Utils.getBundleString(bundle, e1.getKey().toLowerCase());
                                String d2 =
                                        Utils.getBundleString(bundle, e2.getKey().toLowerCase());
                                return d1.compareTo(d2);
                            });

            // Blank separator before each category (except the first)
            if (categoryIndex > 0) {
                Label spacer =
                        new Label(" ", Skin.getSkin().getMultiColumnComboBoxData().getFont());
                keybinds_list_box.addRow(new Row(new GUIObject[] {spacer}, null));
            }
            categoryIndex++;

            // Category header row
            Label headerLabel =
                    new Label(
                            "-- " + group.getKey() + " --",
                            Skin.getSkin().getMultiColumnComboBoxData().getFont());
            headerLabel.setColor(HEADER_COLOR);
            keybinds_list_box.addRow(new Row(new GUIObject[] {headerLabel}, null));

            // Keybind rows
            for (Map.Entry<String, Integer> entry : group.getValue()) {
                String actionName = entry.getKey();
                Integer keyCode = entry.getValue();
                String keyString = Keyboard.keyToString(keyCode);
                String displayName = Utils.getBundleString(bundle, actionName.toLowerCase());

                Label label =
                        new Label(
                                displayName + " [" + keyString + "]",
                                Skin.getSkin().getMultiColumnComboBoxData().getFont());
                if (Globals.getConflictingAction(actionName, keyCode, keybinds) != null) {
                    label.setColor(new float[] {1.0f, 0.3f, 0.3f, 1.0f});
                }
                keybinds_list_box.addRow(
                        new Row(
                                new GUIObject[] {label},
                                new ActionRowDataModel(actionName, keyCode)));
            }
        }
    }

    private class ActionRowDataModel {
        private final String actionName;
        private final int keyCode;

        public ActionRowDataModel(String actionName, int keyCode) {
            this.actionName = actionName;
            this.keyCode = keyCode;
        }

        public String getActionName() {
            return actionName;
        }

        public int getKeyCode() {
            return keyCode;
        }
    }

    private class KeybindListener implements RowListener {

        @Override
        public final void rowChosen(Object o) {
            if (o instanceof ActionRowDataModel) {
                ActionRowDataModel actionRow = (ActionRowDataModel) o;
                RebindActionForm rebindForm = new RebindActionForm(actionRow.getActionName());
                rebindForm.addCloseListener(new RebindActionFormClosedListener());
                gui_root.addModalForm(rebindForm);
            }
        }

        @Override
        public final void rowDoubleClicked(Object o) {}
    }

    private class RebindActionFormClosedListener implements CloseListener {
        @Override
        public final void closed() {
            keybinds_list_box.clear();
            System.out.println("RebindActionForm closed, refreshing keybinds list.");
            evaluateKeybindRows();
        }
    }

    private class ResetKeybindsListener implements MouseClickListener {
        @Override
        public final void mouseClicked(int button, int x, int y, int clicks) {
            gui_root.addModalForm(
                    new QuestionForm(
                            Utils.getBundleString(bundle, "reset_keybinds_confirm"),
                            new ResetKeybindsConfirmListener()));
        }
    }

    private class ResetKeybindsConfirmListener implements MouseClickListener {
        @Override
        public final void mouseClicked(int button, int x, int y, int clicks) {
            Settings.getSettings().resetKeybindsToDefaults();
            keybinds_list_box.clear();
            evaluateKeybindRows();
        }
    }
}
