package com.oddlabs.tt.viewer;

import com.oddlabs.tt.model.Army;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class Selection {
    private final @Nullable Army[] shortcut_armies = new Army[10];
    private final @NonNull Player local_player;
    private @NonNull SelectionArmy current_selection;

    public Selection(@NonNull Player local_player) {
        this.local_player = local_player;
        clearSelection();
    }

    public @NonNull SelectionArmy getCurrentSelection() {
        return current_selection;
    }

    public void clearSelection() {
        current_selection = new SelectionArmy(local_player);
    }

    public void clearShortcutArmies() {
        Arrays.fill(shortcut_armies, null);
    }

    void removeFromArmies(@NonNull Selectable<?> selectable) {
        current_selection.remove(selectable);
        for (Army shortcut_armie : shortcut_armies) {
            if (shortcut_armie != null) {
                shortcut_armie.remove(selectable);
            }
        }
    }

    public void setShortcutArmy(int index) {
        if (shortcut_armies[index] != null)
            shortcut_armies[index].clear();
        else
            shortcut_armies[index] = new Army();

        shortcut_armies[index].addAll(current_selection.getSet());
    }

    public boolean enableShortcutArmy(int index) {
        if (shortcut_armies[index] != null) {
            var set = shortcut_armies[index].getSet();
            current_selection.clear();
            if (!set.isEmpty()) {
                current_selection.addAll(set);
                return true;
            }
        }
        return false;
    }
}
