package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.CloseListener;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.input.Keyboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for keybind panels that provides common functionality
 * for displaying and managing keybind sections.
 */
public abstract class AbstractKeybindPanel extends Panel {
    protected GUIRoot gui_root;
    protected MultiColumnComboBox keybinds_list_box;

    // Human-readable labels for all keybind actions
    public static final Map<String, String> KEYBIND_DISPLAY_NAMES =
            new HashMap<String, String>() {
                {
                    put(Globals.KB_TOGGLE_MAP_MODE, "Toggle Map Mode");
                    put(Globals.KB_JUMP_TO_NOTIFICATION, "Jump To Latest Notification");
                    put(Globals.KB_PLACE_BEACON, "Place Beacon (with Ctrl)");
                    put(Globals.KB_NEXT_IDLE_PEON, "Select Next Idle Peon");

                    put(Globals.KB_PAN_CAMERA_LEFT, "Pan Camera Left");
                    put(Globals.KB_PAN_CAMERA_RIGHT, "Pan Camera Right");
                    put(Globals.KB_PAN_CAMERA_UP, "Pan Camera Up");
                    put(Globals.KB_PAN_CAMERA_DOWN, "Pan Camera Down");
                    put(Globals.KB_CAMERA_ZOOM_IN, "Zoom In");
                    put(Globals.KB_CAMERA_ZOOM_OUT, "Zoom Out");
                    put(Globals.KB_CAMERA_ROTATE_LEFT, "Rotate Camera Left");
                    put(Globals.KB_CAMERA_ROTATE_RIGHT, "Rotate Camera Right");
                    put(Globals.KB_CAMERA_PITCH_UP, "Pitch Camera Up");
                    put(Globals.KB_CAMERA_PITCH_DOWN, "Pitch Camera Down");
                    put(Globals.KB_CAMERA_ZOOM_HOLD, "Hold to Zoom (Keyboard)");
                    put(Globals.KB_CAMERA_FIRST_PERSON_TOGGLE, "Toggle First-Person (Keyboard)");

                    put(Globals.KB_ATTACK, "Attack");
                    put(Globals.KB_GATHER_REPAIR, "Gather/Repair");
                    put(Globals.KB_MOVE, "Move");

                    put(Globals.KB_BUILD_ARMORY, "Build Armory");
                    put(Globals.KB_BUILD_QUARTERS, "Build Quarters");
                    put(Globals.KB_BUILD_TOWER, "Build Tower");

                    put(Globals.KB_ARMORY_DEPLOY_WARRIORS, "Armory - Deploy Warriors");
                    put(Globals.KB_ARMORY_HARVEST, "Armory - Harvest");
                    put(Globals.KB_ARMORY_MAKE_WEAPONS, "Armory - Make Weapons");
                    put(Globals.KB_ARMORY_RALLY_POINT, "Armory - Rally Point");
                    put(Globals.KB_ARMORY_TRANSPORT, "Armory - Transport");

                    put(
                            Globals.KB_ARMORY_DEPLOY_CHICKEN_WARRIORS,
                            "Armory - Deploy Chicken Warriors");
                    put(Globals.KB_ARMORY_DEPLOY_IRON_WARRIORS, "Armory - Deploy Iron Warriors");
                    put(Globals.KB_ARMORY_DEPLOY_PEON, "Armory - Deploy Peon");
                    put(Globals.KB_ARMORY_DEPLOY_ROCK_WARRIORS, "Armory - Deploy Rock Warriors");

                    put(Globals.KB_ARMORY_HARVEST_CHICKEN, "Armory - Harvest Chicken");
                    put(Globals.KB_ARMORY_HARVEST_IRON, "Armory - Harvest Iron");
                    put(Globals.KB_ARMORY_HARVEST_ROCK, "Armory - Harvest Rock");
                    put(Globals.KB_ARMORY_HARVEST_TREE, "Armory - Harvest Tree");

                    put(Globals.KB_ARMORY_TRANSPORT_CHICKEN, "Armory - Transport Chicken");
                    put(Globals.KB_ARMORY_TRANSPORT_IRON, "Armory - Transport Iron");
                    put(Globals.KB_ARMORY_TRANSPORT_ROCK, "Armory - Transport Rock");
                    put(Globals.KB_ARMORY_TRANSPORT_TREE, "Armory - Transport Tree");

                    put(Globals.KB_ARMORY_CREATE_CHICKEN_WEAPON, "Armory - Create Chicken Weapon");
                    put(Globals.KB_ARMORY_CREATE_IRON_WEAPON, "Armory - Create Iron Weapon");
                    put(Globals.KB_ARMORY_CREATE_ROCK_WEAPON, "Armory - Create Rock Weapon");

                    put(Globals.KB_QUARTERS_CHIEFTAIN, "Quarters - Chieftain");
                    put(Globals.KB_QUARTERS_DEPLOY_PEON, "Quarters - Deploy Peon");
                    put(Globals.KB_QUARTERS_SET_RALLY_POINT, "Quarters - Set Rally Point");

                    put(Globals.KB_CHIEFTAIN_MAGIC1, "Chieftain - Magic 1");
                    put(Globals.KB_CHIEFTAIN_MAGIC2, "Chieftain - Magic 2");

                    put(Globals.KB_TOWER_ATTACK, "Tower - Attack");
                    put(Globals.KB_TOWER_EXIT, "Tower - Exit Tower");

                    put(Globals.KB_ARMY_GROUP_0, "Army Group 0 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_1, "Army Group 1 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_2, "Army Group 2 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_3, "Army Group 3 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_4, "Army Group 4 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_5, "Army Group 5 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_6, "Army Group 6 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_7, "Army Group 7 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_8, "Army Group 8 (Ctrl to assign)");
                    put(Globals.KB_ARMY_GROUP_9, "Army Group 9 (Ctrl to assign)");

                    put(Globals.KB_CHAT_TOGGLE, "Chat Toggle");
                    put(Globals.KB_BACK_CANCEL, "Back / Cancel");
                    put(Globals.KB_GAMESPEED_INCREASE, "Increase Gamespeed");
                    put(Globals.KB_GAMESPEED_DECREASE, "Decrease Gamespeed");
                    put(Globals.KB_PAUSE, "Pause / Open Menu");
                }
            };

    public AbstractKeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        this.gui_root = gui_root;

        // Simple layout - just the keybind list
        ColumnInfo[] keybind_options = new ColumnInfo[] {new ColumnInfo("", 300)};
        int dynamicHeight = calculateDynamicHeight();
        keybinds_list_box = new MultiColumnComboBox(gui_root, keybind_options, dynamicHeight, false);
        keybinds_list_box.addRowListener(new KeybindListener());

        evaluateKeybindRows();

        addChild(keybinds_list_box);
        keybinds_list_box.place();
        this.compileCanvas();
    }

    /**
     * Abstract method that subclasses must implement to define which sections
     * of keybinds they should display.
     */
    protected abstract List<Section> getSections();
    
    /**
     * Calculates dynamic height for the keybind list based on viewport size
     * and available content, with reasonable min/max bounds.
     */
    private int calculateDynamicHeight() {
        int viewHeight = LocalInput.getViewHeight();
        
        // Use 45% of screen height as base, with min 200px and max 400px
        int baseHeight = (int)(viewHeight * 0.20f);
        int minHeight = 100;
        int maxHeight = 300;
        
        return Math.max(minHeight, Math.min(maxHeight, baseHeight));
    }
    
    /**
     * Public method to refresh the keybind rows - called from parent panel
     * when keybinds are changed via copy/paste/reset operations.
     */
    public void refreshKeybindRows() {
        evaluateKeybindRows();
    }


    private HashMap<String, Integer> getDefaultKeybinds() {
        return Settings.getDefaultKeybinds();
    }

    /**
     * Calculates the display color for a keybind label based on modifiers.
     * Combines yellow tinting (changed from default) and darkening (duplicate) effects.
     * 
     * @param isChangedFromDefault true if this keybind differs from its default value
     * @param isDuplicate true if this key is assigned to multiple actions
     * @return RGBA color array for the label
     */
    private float[] calculateKeybindColor(boolean isChangedFromDefault, boolean isDuplicate) {
        // Base color: white
        float r = 1.0f, g = 1.0f, b = 1.0f;
        
        // Apply yellow tinting for changed-from-default (more yellow = less blue/red)
        if (isChangedFromDefault) {
            r = 1.0f;      // Keep red at full
            g = 0.92f;     // Slight yellow tint
            b = 0.23f;     // Reduce blue significantly for yellow effect
        }
        
        // Apply darkening for duplicates (multiply by darkening factor)
        if (isDuplicate) {
            float darkeningFactor = 0.7f; // 30% darker
            r *= darkeningFactor;
            g *= darkeningFactor;
            b *= darkeningFactor;
        }
        
        return new float[] {r, g, b, 1.0f}; // Full alpha
    }

    private void evaluateKeybindRows() {
        keybinds_list_box.clear();
        HashMap<String, Integer> keybinds = Settings.getSettings().getKeybinds();

        // Get default keybinds for comparison to detect changed-from-default settings
        HashMap<String, Integer> defaultKeybinds = getDefaultKeybinds();

        // Build reverse map keyCode -> count to detect duplicates
        java.util.HashMap<Integer, Integer> codeCounts = new java.util.HashMap<>();
        for (Integer code : keybinds.values()) {
            if (code == null) continue;
            codeCounts.put(code, codeCounts.getOrDefault(code, 0) + 1);
        }

        List<Section> sections = getSections();

        int orderIndex = 0;
        for (Section sec : sections) {
            orderIndex = addSection(sec, keybinds, defaultKeybinds, codeCounts, orderIndex);
        }
    }

    // Compact section model + helpers
    protected static final class Section {
        final String title;
        final String[] actions;

        Section(String title, String[] actions) {
            this.title = title;
            this.actions = actions;
        }
    }

    protected static Section sec(String title, String... actions) {
        return new Section(title, actions);
    }

    private int addSection(
            Section section,
            HashMap<String, Integer> keybinds,
            HashMap<String, Integer> defaultKeybinds,
            java.util.Map<Integer, Integer> codeCounts,
            int orderIndex) {
        // Header
        OrderedLabel header =
                new OrderedLabel(section.title, orderIndex++, Skin.getSkin().getEditFont());
        header.setColor(new float[] {0.85f, 0.85f, 0.85f, 1});
        keybinds_list_box.addRow(new Row(new GUIObject[] {header}, null));

        // Items
        for (String actionName : section.actions) {
            Integer keyCode = keybinds.get(actionName);
            if (keyCode == null) continue;
            String keyString = Keyboard.keyToString(keyCode);
            String displayName = KEYBIND_DISPLAY_NAMES.getOrDefault(actionName, actionName);
            OrderedLabel label =
                    new OrderedLabel(
                            displayName + " [" + keyString + "]",
                            orderIndex++,
                            Skin.getSkin().getMultiColumnComboBoxData().getFont());
            // Apply color modifiers based on keybind state
            Integer defaultKeyCode = defaultKeybinds.get(actionName);
            boolean isChangedFromDefault = defaultKeyCode != null && !keyCode.equals(defaultKeyCode);
            
            Integer count = codeCounts.get(keyCode);
            boolean isDuplicate = count != null && count > 1;
            
            // Apply combined color modifiers if needed
            if (isChangedFromDefault || isDuplicate) {
                float[] color = calculateKeybindColor(isChangedFromDefault, isDuplicate);
                label.setColor(color);
            }
            keybinds_list_box.addRow(
                    new Row(new GUIObject[] {label}, new ActionRowDataModel(actionName, keyCode)));
        }

        // Spacer
        keybinds_list_box.addRow(
                new Row(
                        new GUIObject[] {
                            new OrderedLabel(" ", orderIndex++, Skin.getSkin().getEditFont())
                        },
                        null));
        return orderIndex;
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
            } else {
                // ignore
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

    // Ensures rows keep insertion order by comparing an explicit index
    protected static final class OrderedLabel extends Label {
        private final int order;

        public OrderedLabel(String text, int order, com.oddlabs.tt.font.Font font) {
            super(text, font);
            this.order = order;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof OrderedLabel) {
                return this.order - ((OrderedLabel) o).order;
            }
            // Fallback to text compare if mixed types appear
            return super.compareTo(o);
        }
    }

    @Override
    public void onActivated() {
        // Refresh list on activation
        evaluateKeybindRows();
    }
}