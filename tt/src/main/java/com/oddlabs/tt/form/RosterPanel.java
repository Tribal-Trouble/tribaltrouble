package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.ScrollableGroup;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ResourceBundle;

/**
 * Tab 4 of the MP create-game dialog. Hosts the per-slot SLOT/RACE/TEAM editing UI that a preset stamps onto the lobby.
 * The slot grid is dynamic (the host can change player count from the Advanced tab), so {@link #setRoster} swaps in a
 * freshly built {@link ScrollableGroup} and recompiles the panel layout.
 */
public final class RosterPanel extends Panel {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(RosterPanel.class.getName());

    private @Nullable ScrollableGroup roster;

    public RosterPanel() {
        super(i18n("caption"));
        compileCanvas();
    }

    public void setRoster(@NonNull ScrollableGroup new_roster) {
        if (roster != null) {
            removeChild(roster);
        }
        roster = new_roster;
        addChild(new_roster);
        new_roster.place();
        compileCanvas();
    }

    private static @NonNull String i18n(@NonNull String key) {
        return Utils.getBundleString(bundle, key);
    }
}
