package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;
import com.oddlabs.tt.gui.Skin;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;

/**
 * Main keybind panel that organizes keybind categories into subtabs.
 * Contains Essential, Combat, Economy, and System keybind categories.
 */
public class KeybindPanel extends Panel {
    private final PanelGroup keybindsGroup;
    private Label statusLabel;
    
    // Keep references to category panels so we can refresh them when keybinds change
    private AbstractKeybindPanel essentialPanel;
    private AbstractKeybindPanel combatPanel;
    private AbstractKeybindPanel economyPanel;
    private AbstractKeybindPanel systemPanel;
    
    public KeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        
        // Create header with controls
        Group headerGroup = new Group();
        
        // Clipboard controls - these operate on ALL keybinds
        HorizButton copyBtn = new HorizButton("Copy All Binds", 130);
        copyBtn.addMouseClickListener((button, x, y, clicks) -> copyBinds());
        headerGroup.addChild(copyBtn);

        HorizButton pasteBtn = new HorizButton("Paste Binds", 120);
        pasteBtn.addMouseClickListener((button, x, y, clicks) -> pasteBinds());
        headerGroup.addChild(pasteBtn);

        HorizButton resetBtn = new HorizButton("Reset to defaults", 160);
        resetBtn.addMouseClickListener((button, x, y, clicks) -> resetBinds());
        headerGroup.addChild(resetBtn);
        
        // Status label for feedback
        statusLabel = new Label("", Skin.getSkin().getEditFont());
        headerGroup.addChild(statusLabel);
        
        // Layout header controls
        copyBtn.place();
        pasteBtn.place(copyBtn, RIGHT_MID);
        resetBtn.place(copyBtn, BOTTOM_LEFT);
        statusLabel.place(resetBtn, BOTTOM_LEFT);
        headerGroup.compileCanvas();
        
        // Create category panels without their own controls
        essentialPanel = new EssentialKeybindPanel(gui_root, "Essential");
        combatPanel = new CombatKeybindPanel(gui_root, "Combat");
        economyPanel = new EconomyKeybindPanel(gui_root, "Economy");
        systemPanel = new SystemKeybindPanel(gui_root, "System");
        
        // Create internal PanelGroup for subtabs
        // Essential first since it contains the most commonly used binds
        Panel[] subPanels = {essentialPanel, combatPanel, economyPanel, systemPanel};
        keybindsGroup = new PanelGroup(subPanels, 0);
        
        addChild(headerGroup);
        addChild(keybindsGroup);
        headerGroup.place();
        keybindsGroup.place(headerGroup, BOTTOM_LEFT, -32);
        compileCanvas();
    }
    
    @Override
    public void onActivated() {
        // The PanelGroup will handle activation of the currently selected sub-panel
        // This ensures that when the Keybinds tab becomes active, the current sub-tab
        // also gets properly refreshed
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
        setStatus("Copied all keybinds to clipboard.", true);
    }

    private void pasteBinds() {
        String fromClip = getClipboardText();
        if (fromClip == null || fromClip.isEmpty()) {
            setStatus("Clipboard is empty.", false);
            return;
        }
        Map<String, Integer> parsed = KeybindCodePanel.parseCode(fromClip);
        if (parsed == null) {
            setStatus("Clipboard doesn't contain a valid keybind code.", false);
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
        setStatus("Applied " + applied + " binds from clipboard.", true);
    }

    private void resetBinds() {
        Settings.getSettings().resetKeybindsToDefaults();
        Settings.getSettings().save();
        refreshAllPanels();
        String code = KeybindCodePanel.generateCode(Settings.getSettings().getKeybinds());
        copyToClipboard(code);
        setStatus("Reset all keybinds to defaults and copied to clipboard.", true);
    }
}