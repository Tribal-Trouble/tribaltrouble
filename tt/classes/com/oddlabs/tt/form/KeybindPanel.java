package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.guievent.CloseListener;
import java.util.HashMap;
import java.util.Map;

// TODO: localization
public class KeybindPanel extends Panel {
    GUIRoot gui_root;
    MultiColumnComboBox keybinds_list_box;

    public KeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        this.gui_root = gui_root;

        Label keybinds_label = new Label("Camera Binds", Skin.getSkin().getEditFont());
        keybinds_label.place();

        // Add all the controls to a group
        Group keybinds_group = new Group();
        keybinds_group.addChild(keybinds_label);

        ColumnInfo[] keybind_options = new ColumnInfo[] { new ColumnInfo("", 300) };
        keybinds_list_box = new MultiColumnComboBox(gui_root, keybind_options, 200, false);
        keybinds_list_box.place(keybinds_label, BOTTOM_LEFT);
        keybinds_list_box.addRowListener(new KeybindListener());
        // TODO: Add the rest of the keybinds here.
        // TODO: Localization
        
        evaluateKeybindRows();

        keybinds_group.addChild(keybinds_list_box);
        keybinds_group.compileCanvas();
        keybinds_group.place();
        
        addChild(keybinds_group);
        this.compileCanvas();
    }

    private void evaluateKeybindRows() {
        HashMap<String, Integer> keybinds = Settings.getSettings().getKeybinds();
        for (Map.Entry<String, Integer> entry : keybinds.entrySet()) {
            String actionName = entry.getKey();
            Integer keyCode = entry.getValue();
            String keyString = Keyboard.keyToString(keyCode);
            Label label = new Label(actionName + " [" + keyString + "]", Skin.getSkin().getMultiColumnComboBoxData().getFont());
            Row row = new Row(new GUIObject[] { label }, new ActionRowDataModel(actionName, keyCode));
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

        public final void rowChosen(Object o) {

            if (o instanceof ActionRowDataModel) {
                ActionRowDataModel actionRow = (ActionRowDataModel) o;
                RebindActionForm rebindForm = new RebindActionForm(actionRow.getActionName());
                rebindForm.addCloseListener(new RebindActionFormClosedListener());
                gui_root.addModalForm(rebindForm);
            }
        }

        public final void rowDoubleClicked(Object o) {
        }
    }

    private class RebindActionFormClosedListener implements CloseListener {
        public final void closed() {
            keybinds_list_box.clear();
            System.out.println("RebindActionForm closed, refreshing keybinds list.");
            evaluateKeybindRows();
        }
    }
}
