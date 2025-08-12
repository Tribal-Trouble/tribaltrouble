package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
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
import com.oddlabs.tt.guievent.CloseListener;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

// TODO: localization
public class KeybindPanel extends Panel {
    GUIRoot gui_root;
    MultiColumnComboBox keybinds_list_box;
    
    // Display names for keybind actions
    public static final Map<String, String> KEYBIND_DISPLAY_NAMES = new HashMap<String, String>() {{
        // Camera Controls
        put(Globals.KB_PAN_CAMERA_LEFT, "Pan Camera Left");
        put(Globals.KB_PAN_CAMERA_RIGHT, "Pan Camera Right");
        put(Globals.KB_PAN_CAMERA_UP, "Pan Camera Up");
        put(Globals.KB_PAN_CAMERA_DOWN, "Pan Camera Down");
        
        // Basic Unit Actions
        put(Globals.KB_ATTACK, "Attack");
        put(Globals.KB_GATHER_REPAIR, "Gather/Repair");
        put(Globals.KB_MOVE, "Move");
        
        // Building Construction
        put(Globals.KB_BUILD_ARMORY, "Build Armory");
        put(Globals.KB_BUILD_QUARTERS, "Build Quarters");
        put(Globals.KB_BUILD_TOWER, "Build Tower");
        
        // Armory Actions
        put(Globals.KB_ARMORY_DEPLOY_WARRIORS, "Armory - Deploy Warriors");
        put(Globals.KB_ARMORY_HARVEST, "Armory - Harvest");
        put(Globals.KB_ARMORY_MAKE_WEAPONS, "Armory - Make Weapons");
        put(Globals.KB_ARMORY_RALLY_POINT, "Armory - Rally Point");
        put(Globals.KB_ARMORY_TRANSPORT, "Armory - Transport");
        
        // Armory - Deploy Units
        put(Globals.KB_ARMORY_DEPLOY_CHICKEN_WARRIORS, "Armory - Deploy Chicken Warriors");
        put(Globals.KB_ARMORY_DEPLOY_IRON_WARRIORS, "Armory - Deploy Iron Warriors");
        put(Globals.KB_ARMORY_DEPLOY_PEON, "Armory - Deploy Peon");
        put(Globals.KB_ARMORY_DEPLOY_ROCK_WARRIORS, "Armory - Deploy Rock Warriors");
        
        // Armory - Resource Harvesting
        put(Globals.KB_ARMORY_HARVEST_CHICKEN, "Armory - Harvest Chicken");
        put(Globals.KB_ARMORY_HARVEST_IRON, "Armory - Harvest Iron");
        put(Globals.KB_ARMORY_HARVEST_ROCK, "Armory - Harvest Rock");
        put(Globals.KB_ARMORY_HARVEST_TREE, "Armory - Harvest Tree");
        
        // Armory - Resource Transportation
        put(Globals.KB_ARMORY_TRANSPORT_CHICKEN, "Armory - Transport Chicken");
        put(Globals.KB_ARMORY_TRANSPORT_IRON, "Armory - Transport Iron");
        put(Globals.KB_ARMORY_TRANSPORT_ROCK, "Armory - Transport Rock");
        put(Globals.KB_ARMORY_TRANSPORT_TREE, "Armory - Transport Tree");
        
        // Armory - Weapon Creation
        put(Globals.KB_ARMORY_CREATE_CHICKEN_WEAPON, "Armory - Create Chicken Weapon");
        put(Globals.KB_ARMORY_CREATE_IRON_WEAPON, "Armory - Create Iron Weapon");
        put(Globals.KB_ARMORY_CREATE_ROCK_WEAPON, "Armory - Create Rock Weapon");
        
        // Quarters Actions
        put(Globals.KB_QUARTERS_CHIEFTAIN, "Quarters - Chieftain");
        put(Globals.KB_QUARTERS_DEPLOY_PEON, "Quarters - Deploy Peon");
        put(Globals.KB_QUARTERS_SET_RALLY_POINT, "Quarters - Set Rally Point");
        
        // Chieftain Magic
        put(Globals.KB_CHIEFTAIN_MAGIC1, "Chieftain - Magic 1");
        put(Globals.KB_CHIEFTAIN_MAGIC2, "Chieftain - Magic 2");
        
        // Tower Actions
        put(Globals.KB_TOWER_ATTACK, "Tower - Attack");
        put(Globals.KB_TOWER_EXIT, "Tower - Exit Tower");
    }};

    public KeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        this.gui_root = gui_root;

        Label keybinds_label = new Label("Camera Binds", Skin.getSkin().getEditFont());
        keybinds_label.place();

        // Add all the controls to a group
        Group keybinds_group = new Group();
        keybinds_group.addChild(keybinds_label);

        ColumnInfo[] keybind_options = new ColumnInfo[] {new ColumnInfo("", 300)};
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
            
            // Get display name from mapping, fallback to action name if not found
            String displayName = KEYBIND_DISPLAY_NAMES.getOrDefault(actionName, actionName);
            
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

        public final void rowChosen(Object o) {

            if (o instanceof ActionRowDataModel) {
                ActionRowDataModel actionRow = (ActionRowDataModel) o;
                RebindActionForm rebindForm = new RebindActionForm(actionRow.getActionName());
                rebindForm.addCloseListener(new RebindActionFormClosedListener());
                gui_root.addModalForm(rebindForm);
            }
        }

        public final void rowDoubleClicked(Object o) {}
    }

    private class RebindActionFormClosedListener implements CloseListener {
        public final void closed() {
            keybinds_list_box.clear();
            System.out.println("RebindActionForm closed, refreshing keybinds list.");
            evaluateKeybindRows();
        }
    }
}
