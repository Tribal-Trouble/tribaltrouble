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
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Localization for labels/sections
public class KeybindPanel extends Panel {
    GUIRoot gui_root;
    MultiColumnComboBox keybinds_list_box;
    private Label statusLabel;

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

    public KeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        this.gui_root = gui_root;

        Label keybinds_label = new Label("Keybinds", Skin.getSkin().getEditFont());
        keybinds_label.place();

        // Add all the controls to a group
        Group keybinds_group = new Group();
        keybinds_group.addChild(keybinds_label);

        // Clipboard controls
        HorizButton copyBtn = new HorizButton("Copy Binds", 120);
        copyBtn.addMouseClickListener((button, x, y, clicks) -> copyBinds());
        keybinds_group.addChild(copyBtn);

        HorizButton pasteBtn = new HorizButton("Paste Binds", 120);
        pasteBtn.addMouseClickListener((button, x, y, clicks) -> pasteBinds());
        keybinds_group.addChild(pasteBtn);

        HorizButton resetBtn = new HorizButton("Reset to defaults", 160);
        resetBtn.addMouseClickListener((button, x, y, clicks) -> resetBinds());
        keybinds_group.addChild(resetBtn);

        ColumnInfo[] keybind_options = new ColumnInfo[] {new ColumnInfo("", 300)};
        keybinds_list_box = new MultiColumnComboBox(gui_root, keybind_options, 200, false);
        // Vertical layout for controls
        copyBtn.place(keybinds_label, BOTTOM_LEFT);
        pasteBtn.place(copyBtn, BOTTOM_LEFT);
        resetBtn.place(pasteBtn, BOTTOM_LEFT);
        keybinds_list_box.place(resetBtn, BOTTOM_LEFT);
        keybinds_list_box.addRowListener(new KeybindListener());
        // TODO: Add the rest of the keybinds here.
        // TODO: Localization

        evaluateKeybindRows();

        keybinds_group.addChild(keybinds_list_box);
        statusLabel = new Label("", Skin.getSkin().getEditFont());
        keybinds_group.addChild(statusLabel);
        statusLabel.place(keybinds_list_box, BOTTOM_LEFT);
        keybinds_group.compileCanvas();
        keybinds_group.place();

        addChild(keybinds_group);
        this.compileCanvas();
    }

    private void setStatus(String msg, boolean ok) {
        if (statusLabel == null) return;
        statusLabel.set(msg);
        if (ok) statusLabel.setColor(new float[] {0.298f, 0.686f, 0.314f, 1});
        else statusLabel.setColor(new float[] {0.9f, 0.2f, 0.2f, 1});
    }

    private static void copyToClipboard(String text) {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(new StringSelection(text), null);
        } catch (Throwable t) {
            // ignore if no clipboard available
        }
    }

    private static String getClipboardText() {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return (String) cb.getData(DataFlavor.stringFlavor);
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    private void evaluateKeybindRows() {
        keybinds_list_box.clear();
        HashMap<String, Integer> keybinds = Settings.getSettings().getKeybinds();

        // Section order: Camera → Main Gameplay → Menus (A–Z) → System → Army Groups
        List<Section> sections =
                Arrays.asList(
                        sec(
                                "Camera Controls",
                                Globals.KB_PAN_CAMERA_LEFT,
                                Globals.KB_PAN_CAMERA_RIGHT,
                                Globals.KB_PAN_CAMERA_UP,
                                Globals.KB_PAN_CAMERA_DOWN),
                        sec(
                                "General Gameplay",
                                Globals.KB_TOGGLE_MAP_MODE,
                                Globals.KB_JUMP_TO_NOTIFICATION,
                                Globals.KB_PLACE_BEACON,
                                Globals.KB_NEXT_IDLE_PEON),
                        sec(
                                "Basic Unit Actions",
                                Globals.KB_MOVE,
                                Globals.KB_ATTACK,
                                Globals.KB_GATHER_REPAIR),
                        sec(
                                "Building Construction",
                                Globals.KB_BUILD_ARMORY,
                                Globals.KB_BUILD_QUARTERS,
                                Globals.KB_BUILD_TOWER),
                        sec(
                                "Armory Actions",
                                Globals.KB_ARMORY_DEPLOY_WARRIORS,
                                Globals.KB_ARMORY_HARVEST,
                                Globals.KB_ARMORY_MAKE_WEAPONS,
                                Globals.KB_ARMORY_TRANSPORT,
                                Globals.KB_ARMORY_RALLY_POINT),
                        sec(
                                "Armory - Deploy Units",
                                Globals.KB_ARMORY_DEPLOY_CHICKEN_WARRIORS,
                                Globals.KB_ARMORY_DEPLOY_IRON_WARRIORS,
                                Globals.KB_ARMORY_DEPLOY_PEON,
                                Globals.KB_ARMORY_DEPLOY_ROCK_WARRIORS),
                        sec(
                                "Armory - Resource Harvesting",
                                Globals.KB_ARMORY_HARVEST_CHICKEN,
                                Globals.KB_ARMORY_HARVEST_IRON,
                                Globals.KB_ARMORY_HARVEST_ROCK,
                                Globals.KB_ARMORY_HARVEST_TREE),
                        sec(
                                "Armory - Resource Transportation",
                                Globals.KB_ARMORY_TRANSPORT_CHICKEN,
                                Globals.KB_ARMORY_TRANSPORT_IRON,
                                Globals.KB_ARMORY_TRANSPORT_ROCK,
                                Globals.KB_ARMORY_TRANSPORT_TREE),
                        sec(
                                "Armory - Weapon Creation",
                                Globals.KB_ARMORY_CREATE_CHICKEN_WEAPON,
                                Globals.KB_ARMORY_CREATE_IRON_WEAPON,
                                Globals.KB_ARMORY_CREATE_ROCK_WEAPON),
                        sec(
                                "Chieftain Magic",
                                Globals.KB_CHIEFTAIN_MAGIC1,
                                Globals.KB_CHIEFTAIN_MAGIC2),
                        sec(
                                "Quarters Actions",
                                Globals.KB_QUARTERS_CHIEFTAIN,
                                Globals.KB_QUARTERS_DEPLOY_PEON,
                                Globals.KB_QUARTERS_SET_RALLY_POINT),
                        sec("Tower Actions", Globals.KB_TOWER_ATTACK, Globals.KB_TOWER_EXIT),
                        sec(
                                "System / Interface",
                                Globals.KB_CHAT_TOGGLE,
                                Globals.KB_BACK_CANCEL,
                                Globals.KB_GAMESPEED_INCREASE,
                                Globals.KB_GAMESPEED_DECREASE,
                                Globals.KB_PAUSE),
                        sec(
                                "Army Groups",
                                Globals.KB_ARMY_GROUP_0,
                                Globals.KB_ARMY_GROUP_1,
                                Globals.KB_ARMY_GROUP_2,
                                Globals.KB_ARMY_GROUP_3,
                                Globals.KB_ARMY_GROUP_4,
                                Globals.KB_ARMY_GROUP_5,
                                Globals.KB_ARMY_GROUP_6,
                                Globals.KB_ARMY_GROUP_7,
                                Globals.KB_ARMY_GROUP_8,
                                Globals.KB_ARMY_GROUP_9));

        int orderIndex = 0;
        for (Section sec : sections) {
            orderIndex = addSection(sec, keybinds, orderIndex);
        }
    }

    // Compact section model + helpers
    private static final class Section {
        final String title;
        final String[] actions;

        Section(String title, String[] actions) {
            this.title = title;
            this.actions = actions;
        }
    }

    private static Section sec(String title, String... actions) {
        return new Section(title, actions);
    }

    private int addSection(Section section, HashMap<String, Integer> keybinds, int orderIndex) {
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
    private static final class OrderedLabel extends Label {
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

    // Clipboard actions
    private void copyBinds() {
        String code = KeybindCodePanel.generateCode(Settings.getSettings().getKeybinds());
        copyToClipboard(code);
        setStatus("Copied to clipboard.", true);
    }

    private void pasteBinds() {
        String fromClip = getClipboardText();
        if (fromClip == null || fromClip.isEmpty()) {
            setStatus("Clipboard is empty.", false);
            return;
        }
        Map<String, Integer> parsed = KeybindCodePanel.parseCode(fromClip);
        if (parsed == null) {
            setStatus("Clipboard doesn’t contain a valid keybind code.", false);
            return;
        }
        HashMap<String, Integer> keybinds = Settings.getSettings().getKeybinds();
        int applied = 0;
        for (Map.Entry<String, Integer> e : parsed.entrySet()) {
            if (keybinds.containsKey(e.getKey())) {
                Settings.getSettings().setKeybind(e.getKey(), e.getValue());
                applied++;
            }
        }
        Settings.getSettings().save();
        evaluateKeybindRows();
        setStatus("Applied " + applied + " binds from clipboard.", true);
    }

    private void resetBinds() {
        Settings.getSettings().resetKeybindsToDefaults();
        Settings.getSettings().save();
        evaluateKeybindRows();
        String code = KeybindCodePanel.generateCode(Settings.getSettings().getKeybinds());
        copyToClipboard(code);
        setStatus("Reset to defaults and copied to clipboard.", true);
    }
}
