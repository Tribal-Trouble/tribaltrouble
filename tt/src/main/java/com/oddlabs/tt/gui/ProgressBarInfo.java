package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;

public final class ProgressBarInfo {
	private final Label label;
	private final float weight;
	private int waypoint;

	public ProgressBarInfo(String title, float weight) {
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
