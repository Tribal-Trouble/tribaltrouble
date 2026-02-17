package com.oddlabs.tt.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

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

public class KeybindPanel extends Panel {
    GUIRoot gui_root;
    private final ResourceBundle bundle = ResourceBundle.getBundle(KeybindPanel.class.getName());
    MultiColumnComboBox keybinds_list_box;

    public KeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        this.gui_root = gui_root;

        Label keybinds_label = new Label(Utils.getBundleString(bundle, "keybinds"), Skin.getSkin().getEditFont());
        keybinds_label.place();

        // Add all the controls to a group
        Group keybinds_group = new Group();
        keybinds_group.addChild(keybinds_label);

        ColumnInfo[] keybind_options = new ColumnInfo[] {new ColumnInfo("", 300)};
        keybinds_list_box = new MultiColumnComboBox(gui_root, keybind_options, 200, false);
        keybinds_list_box.place(keybinds_label, BOTTOM_LEFT);
        keybinds_list_box.addRowListener(new KeybindListener());

        evaluateKeybindRows();

        HorizButton button_reset_keybinds = new HorizButton(Utils.getBundleString(bundle, "reset_keybinds"), 120);
        button_reset_keybinds.place(keybinds_list_box, BOTTOM_LEFT);
        button_reset_keybinds.addMouseClickListener(new ResetKeybindsListener());

        keybinds_group.addChild(keybinds_list_box);
        keybinds_group.addChild(button_reset_keybinds);
        keybinds_group.compileCanvas();
        keybinds_group.place();

        addChild(keybinds_group);
        this.compileCanvas();
    }

    private void evaluateKeybindRows() {
        HashMap<String, Integer> keybinds = Settings.getSettings().getKeybinds();

        // Convert to list and sort by display name to group categories together
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(keybinds.entrySet());
        sortedEntries.sort((Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) -> {
            String displayName1 = Utils.getBundleString(bundle, e1.getKey().toLowerCase());
            String displayName2 = Utils.getBundleString(bundle, e2.getKey().toLowerCase());
            return displayName1.compareTo(displayName2);
        });

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String actionName = entry.getKey();
            Integer keyCode = entry.getValue();
            String keyString = Keyboard.keyToString(keyCode);

            // Get display name from ResourceBundle
            String displayName = Utils.getBundleString(bundle, actionName.toLowerCase());

            Label label =
                    new Label(
                            displayName + " [" + keyString + "]",
                            Skin.getSkin().getMultiColumnComboBoxData().getFont());
            Row row = new Row(new GUIObject[] {label}, new ActionRowDataModel(actionName, keyCode));
            keybinds_list_box.addRow(row);
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
            if (o instanceof ActionRowDataModel actionRow) {
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
            gui_root.addModalForm(new QuestionForm(
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
