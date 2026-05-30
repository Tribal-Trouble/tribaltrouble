package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * Tab 4 of the MP create-game dialog. Hosts the per-slot SLOT/RACE/TEAM editing UI that a preset stamps onto the lobby.
 * Content arrives in a later step — this is the empty shell so {@link TerrainMenu} can wire the 4-tab structure.
 */
public final class RosterPanel extends Panel {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(RosterPanel.class.getName());

    public RosterPanel() {
        super(i18n("caption"));
        compileCanvas();
    }

    private static @NonNull String i18n(@NonNull String key) {
        return Utils.getBundleString(bundle, key);
    }
}
