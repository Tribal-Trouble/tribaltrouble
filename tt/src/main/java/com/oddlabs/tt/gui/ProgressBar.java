package com.oddlabs.tt.gui;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.ResourceBundle;

public final class ProgressBar extends GUIObject {
	private static final ResourceBundle bundle = ResourceBundle.getBundle(ProgressBar.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	private record Waypoint(int point, float weight) {}

	private final @NonNull ProgressBarInfo @NonNull [] info;
	private final @NonNull Waypoint @NonNull [] waypoints;
	private final boolean text_only;
	
	private final @NonNull NetworkSelector network;
	private int left_margin;
	private int right_margin;

	private int index;
	private float step;

	public ProgressBar(@NonNull NetworkSelector network, int width, @NonNull ProgressBarInfo @NonNull [] info, boolean text_only) {
		this.info = info;
		this.network = network;
		this.text_only = text_only;
		if (text_only) {
			setDim(width, Skin.getSkin().getHeadlineFont().getHeight());
		} else {
			ProgressBarData data = Skin.getSkin().getProgressBarData();
			left_margin = data.leftFill().quad(ModeIconQuads.Mode.NORMAL).getWidth();
			right_margin = data.rightFill().quad(ModeIconQuads.Mode.NORMAL).getWidth();
			assert width > left_margin + right_margin : "Progress bar too small.";
			setDim(width, data.progressBar().getHeight());
		}
		this.waypoints = pixelize(info, width);
		setCanFocus(false);
	}

	public void progress() {
		assert index < info.length: "Too much progress";
		index++;
		step = 0;
		update();
	}

	public void progress(float fraction) {
		int current = 0;
		if (index > 0)
			current = waypoints[index - 1].point();

		step += fraction*(waypoints[index].point() - current);
		if (step > waypoints[index].point() - current) {
			step = waypoints[index].point() - current;
		}

		update();
	}

	private void renderText(@NonNull GUIRenderer renderer) {
		int offset = index > 0 ? waypoints[index - 1].point() : 0;
        float done = (offset + step)/getWidth();
		ResourceBundle bundle = ResourceBundle.getBundle(ProgressBar.class.getName());
		int percentage = (int)(done*100);
		String string = i18n("loading", percentage);
		TextLineRenderer.render(renderer, Skin.getSkin().getHeadlineFont(), string, 0, 0, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Color.WHITE);
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		if (text_only)
			renderText(renderer);
		else {
			Skin.getSkin().getProgressBarData().progressBar()
			    .render(renderer, 0, 0, getWidth(), ModeIconQuads.Mode.NORMAL);
			renderFill(renderer, 0);
		}
	}

	private Waypoint @NonNull [] pixelize(@NonNull ProgressBarInfo @NonNull [] info, int width) {
		float sum = (float) Arrays.stream(info).mapToDouble(ProgressBarInfo::getWeight).sum();

		Waypoint[] waypoints = new Waypoint[info.length];
		int currentWaypoint = 0;
		for (int i = 0; i < info.length; i++) {
			currentWaypoint += (int) ((info[i].getWeight() / sum) * width);
			int point = Math.min(currentWaypoint, width - right_margin);
			point = Math.max(point, left_margin);
			waypoints[i] = new Waypoint(point, info[i].getWaypoint());
		}
		waypoints[info.length - 1] = new Waypoint(width - right_margin, waypoints[info.length - 1].weight());
		return waypoints;
	}

	private void renderFill(@NonNull GUIRenderer renderer, int y) {
		ProgressBarData data = Skin.getSkin().getProgressBarData();
		ModeIconQuads left = data.leftFill();
        ModeIconQuads center = data.centerFill();
        ModeIconQuads right = data.rightFill();

		renderer.drawModeIcon(left, ModeIconQuads.Mode.NORMAL, 0, y);

		int offset = index > 0 ? waypoints[index - 1].point() : 0;
        int current_pos = offset + (int)step;
        int width = current_pos - left_margin;
        
        if (width > 0) {
            IconQuad c = center.quad(ModeIconQuads.Mode.NORMAL);
            renderer.drawTexture(c.getTexture(), left_margin, y, width, c.getHeight(), c.getU1(), c.getV1(), c.getU2(), c.getV2(), Color.WHITE);
            renderer.drawModeIcon(right, ModeIconQuads.Mode.NORMAL, current_pos, y);
        }
	}

	private void update() {
		network.tick();
	}
}
