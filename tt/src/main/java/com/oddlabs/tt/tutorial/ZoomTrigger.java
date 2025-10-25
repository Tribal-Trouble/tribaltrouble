package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.viewer.WorldViewer;

public final class ZoomTrigger extends TutorialTrigger {
	private final boolean[] zoom_dirs = new boolean[2];

	public ZoomTrigger(WorldViewer viewer) {
		super(0f, 2f, "zoom");
		viewer.getCamera().resetLastZoomFactor();
	}

        @Override
	protected void run(Tutorial tutorial) {
		GameCamera camera = tutorial.getViewer().getCamera();
		if (camera.getLastZoomFactor() > 0f) {
			zoom_dirs[0] = true;
		} else if (camera.getLastZoomFactor() < 0f) {
			zoom_dirs[1] = true;
		}
            for (boolean zoomDir : zoom_dirs) {
                if (!zoomDir)
                    return;
            }
		tutorial.next(new RotateTrigger());
	}
}
