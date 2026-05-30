package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * Tab 1 of the MP create-game dialog. Hosts the mode card row, the preset grid, and the {@code +Save current as preset}
 * card. Content arrives in a later step — this is the empty shell so {@link TerrainMenu} can wire the 4-tab structure.
 */
public final class ModeAndPresetsPanel extends Panel {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ModeAndPresetsPanel.class.getName());

    public ModeAndPresetsPanel() {
        super(i18n("caption"));
        compileCanvas();
    }

    private static @NonNull String i18n(@NonNull String key) {
        return Utils.getBundleString(bundle, key);
    }
}
