package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;
import com.oddlabs.tt.util.Utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Main keybind panel that organizes keybind categories into subtabs. Contains Essential, Combat,
 * Economy, and System keybind categories.
 */
public class KeybindPanel extends Panel {
    private static final ResourceBundle bundle =
            ResourceBundle.getBundle(KeybindPanel.class.getName());
    private final PanelGroup keybindsGroup;
    private final GUIRoot gui_root;

    // Keep references to category panels so we can refresh them when keybinds change
    private AbstractKeybindPanel essentialPanel;
    private AbstractKeybindPanel combatPanel;
    private AbstractKeybindPanel economyPanel;
    private AbstractKeybindPanel systemPanel;

    public KeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        this.gui_root = gui_root;

        // Create header with controls
        Group headerGroup = new Group();

        // Clipboard controls - these operate on ALL keybinds
        HorizButton copyBtn = new HorizButton(Utils.getBundleString(bundle, "copy_button"), 84);
        copyBtn.addMouseClickListener((button, x, y, clicks) -> copyBinds());
        headerGroup.addChild(copyBtn);

        HorizButton pasteBtn = new HorizButton(Utils.getBundleString(bundle, "paste_button"), 84);
        pasteBtn.addMouseClickListener((button, x, y, clicks) -> pasteBinds());
        headerGroup.addChild(pasteBtn);

        HorizButton resetBtn = new HorizButton(Utils.getBundleString(bundle, "reset_button"), 84);
        resetBtn.addMouseClickListener((button, x, y, clicks) -> resetBinds());
        headerGroup.addChild(resetBtn);

        HorizButton legendBtn = new HorizButton(Utils.getBundleString(bundle, "legend_button"), 84);
        legendBtn.addMouseClickListener(
                (button, x, y, clicks) -> {
                    LegendForm lf = new LegendForm();
                    gui_root.addModalForm(lf);
                });
        headerGroup.addChild(legendBtn);

        // Layout header controls (buttons row) — Legend first
        legendBtn.place();
        copyBtn.place(legendBtn, RIGHT_MID);
        pasteBtn.place(copyBtn, RIGHT_MID);
        resetBtn.place(pasteBtn, RIGHT_MID);
        // no additional controls beyond legend
        headerGroup.compileCanvas();

        // Create category panels without their own controls
        essentialPanel =
                new EssentialKeybindPanel(gui_root, Utils.getBundleString(bundle, "essential_tab"));
        combatPanel = new CombatKeybindPanel(gui_root, Utils.getBundleString(bundle, "combat_tab"));
        economyPanel =
                new EconomyKeybindPanel(gui_root, Utils.getBundleString(bundle, "tasks_tab"));
        systemPanel = new SystemKeybindPanel(gui_root, Utils.getBundleString(bundle, "system_tab"));

        // Create internal PanelGroup for subtabs
        // Essential first since it contains the most commonly used binds
        Panel[] subPanels = {essentialPanel, combatPanel, economyPanel, systemPanel};
        keybindsGroup = new PanelGroup(subPanels, 0);

        // Place keybinds list first, then controls header at the bottom
        addChild(keybindsGroup);
        addChild(headerGroup);
        keybindsGroup.place();
        // Place buttons group under the list with default spacing
        headerGroup.place(keybindsGroup, BOTTOM_LEFT);
        compileCanvas();
    }

    @Override
    public void onActivated() {
        // The PanelGroup will handle activation of the currently selected sub-panel
        // This ensures that when the Keybinds tab becomes active, the current sub-tab
        // also gets properly refreshed
    }

    private void setStatus(String msg, boolean ok) {
        float[] GREEN = KeybindColors.SUCCESS;
        float[] RED = KeybindColors.ERROR;
        if (gui_root != null && gui_root.getInfoPrinter() != null) {
            gui_root.getInfoPrinter().print(msg, ok ? GREEN : RED);
        }
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

    // Refresh all category panels when keybinds change
    private void refreshAllPanels() {
        essentialPanel.refreshKeybindRows();
        combatPanel.refreshKeybindRows();
        economyPanel.refreshKeybindRows();
        systemPanel.refreshKeybindRows();
    }

    // Clipboard actions that operate on ALL keybinds
    private void copyBinds() {
        String code = KeybindCodePanel.generateCode(Settings.getSettings().getKeybinds());
        copyToClipboard(code);
        setStatus(Utils.getBundleString(bundle, "copy_success"), true);
    }

    private void pasteBinds() {
        String fromClip = getClipboardText();
        if (fromClip == null || fromClip.isEmpty()) {
            // Fallback to GLFW clipboard (works in some headless/embedded contexts)
            try {
                fromClip = com.oddlabs.tt.render.Display.getClipboard();
            } catch (Throwable t) {
                // ignore
            }
        }
        if (fromClip == null || fromClip.isEmpty()) {
            setStatus(Utils.getBundleString(bundle, "clipboard_empty"), false);
            return;
        }
        Map<String, Integer> parsed = KeybindCodePanel.parseCode(fromClip);
        if (parsed == null) {
            setStatus(Utils.getBundleString(bundle, "clipboard_invalid"), false);
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
        refreshAllPanels();
        setStatus(Utils.getBundleString(bundle, "apply_success", new Object[] {applied}), true);
    }

    private void resetBinds() {
        Settings.getSettings().resetKeybindsToDefaults();
        Settings.getSettings().save();
        refreshAllPanels();
        // Do not touch clipboard on reset; only update UI
        setStatus(Utils.getBundleString(bundle, "reset_success"), true);
    }
}
