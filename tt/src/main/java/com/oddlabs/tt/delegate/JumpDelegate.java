package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.camera.JumpCamera;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class JumpDelegate extends InGameDelegate {
	private final @Nullable Runnable runnable;

	public JumpDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera old_camera, float x, float y) {
		super(viewer, null);
		setCamera(new JumpCamera(this, old_camera, x, y));
		runnable = null;
	}

	public JumpDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera old_camera, float x, float y, float meters_per_second, float max_seconds) {
		this(viewer, old_camera, x, y, meters_per_second, max_seconds, null);
	}

	public JumpDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera old_camera, float x, float y, float meters_per_second, float max_seconds, Runnable runnable) {
		super(viewer, null);
		setCamera(new JumpCamera(this, old_camera, x, y, meters_per_second, max_seconds));
		this.runnable = runnable;
	}

    @Override
	public void keyPressed(@NonNull KeyboardEvent event) {
	}

    @Override
	public void keyRepeat(KeyboardEvent event) {
	}

    @Override
	public void keyReleased(@NonNull KeyboardEvent event) {
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
