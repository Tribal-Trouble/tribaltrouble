package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import org.jspecify.annotations.NonNull;

public final class ProgressBarInfo {
	private final @NonNull Label label;
	private final float weight;
	private int waypoint;

	public ProgressBarInfo(@NonNull String title, float weight) {
		Font font = Skin.getSkin().getProgressBarData().getFont();
		label = new Label(title, font);
		this.weight = weight;
	}

	public float getWeight() {
		return weight;
	}

	public void setWaypoint(int waypoint) {
		this.waypoint = waypoint;
	}

	public int getWaypoint() {
		return waypoint;
	}

	public Label getLabel() {
		return label;
	}
}
