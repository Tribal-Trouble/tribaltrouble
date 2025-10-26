package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

public final class ScrollTrigger extends TutorialTrigger {
	private final boolean[] scroll_dirs = new boolean[4];

	public ScrollTrigger(@NonNull Player player) {
		super(.1f, 2f, "scroll");
		player.enableMoving(false);
		player.enableRepairing(false);
		player.enableAttacking(false);
		player.enableBuilding(Race.BUILDING_QUARTERS, false);
		player.enableBuilding(Race.BUILDING_ARMORY, false);
		player.enableBuilding(Race.BUILDING_TOWER, false);
		player.enableChieftains(false);
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		GameCamera camera = tutorial.getViewer().getCamera();
		if (camera.getScrollX() > 0) {
			scroll_dirs[0] = true;
		} else if (camera.getScrollX() < 0) {
			scroll_dirs[1] = true;
		}
		if (camera.getScrollY() > 0) {
			scroll_dirs[2] = true;
		} else if (camera.getScrollY() < 0) {
			scroll_dirs[3] = true;
		}
            for (boolean scrollDir : scroll_dirs) {
                if (!scrollDir)
                    return;
            }
		tutorial.next(new ZoomTrigger(tutorial.getViewer()));
	}
}
