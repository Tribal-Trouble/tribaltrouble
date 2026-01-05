package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

public interface NotificationListener {
	default void newAttackNotification(@NonNull Selectable<?> target) {}
	default void newSelectableNotification(@NonNull Selectable<?> target) {}
	default void registerTarget(@NonNull Target target) {}
	default void unregisterTarget(@NonNull Target target) {}
	default void patchesEdited(int patch_x0, int patch_y0, int patch_x1, int patch_y1) {}
	default void gamespeedChanged(int speed) {}
	default void playerGamespeedChanged() {}
}
