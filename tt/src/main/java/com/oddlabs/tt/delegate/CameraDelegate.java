package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class CameraDelegate extends Delegate {
	private final @NonNull GUIRoot gui_root;
	private @Nullable Camera camera;

	public CameraDelegate(@NonNull GUIRoot gui_root, @Nullable Camera camera) {
		this.camera = camera;
		this.gui_root = gui_root;
	}

	protected final @NonNull GUIRoot getGUIRoot() {
		return gui_root;
	}

	public final void setCamera(@Nullable Camera camera) {
		this.camera = camera;
	}

	public final @Nullable Camera getCamera() {
		return camera;
	}

	@Override
	protected void doAdd() {
		super.doAdd();
		getCamera().enable();
	}

	@Override
	protected void doRemove() {
		super.doRemove();
		getCamera().disable();
	}

	public boolean renderCursor() {
		return true;
	}

	public boolean canScroll() {
		return false;
	}

	public boolean forceRender() {
		return false;
	}

	public final void pop() {
		gui_root.removeDelegate(this);
	}
}
