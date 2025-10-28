package com.oddlabs.tt.landscape;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.util.Target;
import org.lwjgl.util.vector.Matrix4f;

public interface NotificationListener {
	void newAttackNotification(Selectable target);
	void newSelectableNotification(Selectable target);
	void registerTarget(Target target);
	void unregisterTarget(Target target);
	void updateTreeLowDetail(Matrix4f matrix, TreeSupply tree);
	void patchesEdited(int patch_x0, int patch_y0, int patch_x1, int patch_y1);
	void gamespeedChanged(int speed);
	void playerGamespeedChanged();
}
