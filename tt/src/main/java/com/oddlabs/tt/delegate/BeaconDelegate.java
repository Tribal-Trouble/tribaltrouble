package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.render.LandscapeLocation;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

public final class BeaconDelegate extends TargetDelegate {
	public BeaconDelegate(@NonNull WorldViewer viewer, @NonNull GameCamera camera) {
		super(viewer, camera, Action.DEFAULT);
	}

	@Override
	public void mousePressed (@NonNull MouseButton button, int x, int y) {
		LandscapeLocation landscape_hit = new LandscapeLocation();
		getViewer().getPicker().pickLocation(getCamera().getState(), landscape_hit);
		getViewer().getPeerHub().sendBeacon(landscape_hit.x, landscape_hit.y);
		pop();
	}
}
