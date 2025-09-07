package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;

/**
 * Main keybind panel that organizes keybind categories into subtabs.
 * Contains Essential, Combat, Economy, and System keybind categories.
 */
public class KeybindPanel extends Panel {
    private final PanelGroup keybindsGroup;
    
    public KeybindPanel(GUIRoot gui_root, String caption) {
        super(caption);
        
        // Create category panels
        Panel essentialPanel = new EssentialKeybindPanel(gui_root, "Essential");
        Panel combatPanel = new CombatKeybindPanel(gui_root, "Combat");
        Panel economyPanel = new EconomyKeybindPanel(gui_root, "Economy");
        Panel systemPanel = new SystemKeybindPanel(gui_root, "System");
        
        // Create internal PanelGroup for subtabs
        // Essential first since it contains the most commonly used binds
        Panel[] subPanels = {essentialPanel, combatPanel, economyPanel, systemPanel};
        keybindsGroup = new PanelGroup(subPanels, 0);
        
        addChild(keybindsGroup);
        keybindsGroup.place();
        compileCanvas();
    }
    
    @Override
    public void onActivated() {
        // The PanelGroup will handle activation of the currently selected sub-panel
        // This ensures that when the Keybinds tab becomes active, the current sub-tab
        // also gets properly refreshed
    }
}