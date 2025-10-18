package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.camera.JumpCamera;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.viewer.WorldViewer;

public final class JumpDelegate extends InGameDelegate {
	private final Runnable runnable;

	public JumpDelegate(WorldViewer viewer, GameCamera old_camera, float x, float y) {
		super(viewer, null);
		setCamera(new JumpCamera(this, old_camera, x, y));
		runnable = null;
	}

	public JumpDelegate(WorldViewer viewer, GameCamera old_camera, float x, float y, float meters_per_second, float max_seconds) {
		this(viewer, old_camera, x, y, meters_per_second, max_seconds, null);
	}

	public JumpDelegate(WorldViewer viewer, GameCamera old_camera, float x, float y, float meters_per_second, float max_seconds, Runnable runnable) {
		super(viewer, null);
		setCamera(new JumpCamera(this, old_camera, x, y, meters_per_second, max_seconds));
		this.runnable = runnable;
	}

    @Override
	public void keyPressed(KeyboardEvent event) {
	}

    @Override
	public void keyRepeat(KeyboardEvent event) {
	}

    @Override
	public void keyReleased(KeyboardEvent event) {
	}

    @Override
	public void mouseScrolled(int amount) {
	}

    @Override
	public void doRemove() {
		super.doRemove();
		if (runnable != null)
			runnable.run();
	}
}
