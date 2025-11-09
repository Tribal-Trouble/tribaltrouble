package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.camera.GameCamera;
import org.jspecify.annotations.NonNull;

public final class PitchTrigger extends TutorialTrigger {
	private final boolean[] pitch_dirs = new boolean[2];

	public PitchTrigger() {
		super(.1f, 2f, "pitch");
	}

	@Override
	protected void run(@NonNull Tutorial tutorial) {
		GameCamera camera = tutorial.getViewer().getCamera();
		if (camera.pitchUp()) {
			pitch_dirs[0] = true;
		} 
		if (camera.pitchDown()) {
			pitch_dirs[1] = true;
		}
            for (boolean pitchDir : pitch_dirs) {
                if (!pitchDir)
                    return;
            }
		tutorial.next(new FirstPersonCameraTrigger());
	}
}
