package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

public interface NotificationListener {
    default void newAttackNotification(@NonNull Selectable<?> target) {
    }

    default void newSelectableNotification(@NonNull Selectable<?> target) {
    }

    default void registerTarget(@NonNull Target target) {
    }

    default void unregisterTarget(@NonNull Target target) {
    }

    default void patchesEdited(int patch_x0, int patch_y0, int patch_x1, int patch_y1) {
    }

    default void gamespeedChanged(int speed) {
    }

    default void playerGamespeedChanged() {
    }

    default void playerCamera(@NonNull Player player, float x, float y, float z, float horiz_angle, float vert_angle) {
    }

    default void playerCursor(@NonNull Player player, float x, float y, boolean on_map) {
    }

    default void playerSelection(@NonNull Player player, Selectable<?> @NonNull [] selection) {
    }
}
