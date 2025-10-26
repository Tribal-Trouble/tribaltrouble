package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.camera.MapCamera;
import org.jspecify.annotations.NonNull;

public final class MapModeTrigger extends TutorialTrigger {
	public MapModeTrigger() {
		super(.1f, 1f, "map_mode");
	}

        @Override
	protected void run(@NonNull Tutorial tutorial) {
		if (tutorial.getViewer().getDelegate().getCamera() instanceof MapCamera)
			tutorial.next(new FromMapModeTrigger());
	}
}
