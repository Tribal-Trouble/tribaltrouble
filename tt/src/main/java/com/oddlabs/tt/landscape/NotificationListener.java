package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.util.Target;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

public interface NotificationListener {
	void newAttackNotification(@NonNull Selectable target);
	void newSelectableNotification(@NonNull Selectable target);
	void registerTarget(@NonNull Target target);
	void unregisterTarget(@NonNull Target target);
	void updateTreeLowDetail(@NonNull Matrix4f matrix, @NonNull TreeSupply tree);
	void patchesEdited(int patch_x0, int patch_y0, int patch_x1, int patch_y1);
	void gamespeedChanged(int speed);
	void playerGamespeedChanged();
}
