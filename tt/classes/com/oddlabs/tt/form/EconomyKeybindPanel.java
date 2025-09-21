package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;
import java.util.Arrays;
import java.util.List;

/**
 * Economy keybinds panel containing all resource management and production controls.
 * Includes all armory functions and quarters actions for economic gameplay.
 */
public class EconomyKeybindPanel extends AbstractKeybindPanel {

    public EconomyKeybindPanel(GUIRoot gui_root, String caption) {
        super(gui_root, caption);
    }

    @Override
    protected List<Section> getSections() {
        return Arrays.asList(
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
                        Globals.KB_ARMORY_DEPLOY_ROCK_WARRIORS,
                        Globals.KB_ARMORY_DEPLOY_PEON),
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
                        "Quarters Actions",
                        Globals.KB_QUARTERS_CHIEFTAIN,
                        Globals.KB_QUARTERS_DEPLOY_PEON,
                        Globals.KB_QUARTERS_SET_RALLY_POINT)
        );
    }
}