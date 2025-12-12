package com.oddlabs.tt.gui;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class ProgressBar extends GUIObject {
	private final @NonNull ProgressBarInfo @NonNull [] info;
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
			left_margin = data.getLeftFill().quad(ModeIconQuads.Mode.NORMAL).getWidth();
			right_margin = data.getRightFill().quad(ModeIconQuads.Mode.NORMAL).getWidth();
			assert width > left_margin + right_margin : "Progress bar too small.";
			setDim(width, data.getProgressBar().getHeight());
		}
		pixelize(info, width);
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
			current = info[index - 1].getWaypoint();

		step += fraction*(info[index].getWaypoint() - current);
		if (step > info[index].getWaypoint() - current) {
			step = info[index].getWaypoint() - current;
		}

		update();
	}

	private void renderText(@NonNull GUIRenderer renderer) {
		int offset = index > 0 ? info[index - 1].getWaypoint() : 0;
        float done = (offset + step)/getWidth();
		ResourceBundle bundle = ResourceBundle.getBundle(ProgressBar.class.getName());
		int percentage = (int)(done*100);
		String string = Utils.getBundleString(bundle, "loading", percentage);
		TextLineRenderer.render(renderer, Skin.getSkin().getHeadlineFont(), string, 0, 0, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Color.WHITE_INT);
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		if (text_only)
			renderText(renderer);
		else {
			Skin.getSkin().getProgressBarData().getProgressBar()
			    .render(renderer, 0, 0, getWidth(), ModeIconQuads.Mode.NORMAL);
			renderFill(renderer, 0);
		}
	}

	private void pixelize(@NonNull ProgressBarInfo @NonNull [] info, int width) {
		float sum = 0;
        for (ProgressBarInfo info1 : info) {
            sum += info1.getWeight();
        }

		info[0].setWaypoint((int)((info[0].getWeight()/sum)*width));
		for (int i = 1; i < info.length - 1; i++) {
			info[i].setWaypoint(info[i - 1].getWaypoint() + (int)((info[i].getWeight()/sum)*width));
		}
		info[info.length - 1].setWaypoint(width);

        for (ProgressBarInfo info1 : info) {
            if (info1.getWaypoint() > width - right_margin) {
                info1.setWaypoint(width - right_margin);
            } else if (info1.getWaypoint() < left_margin) {
                info1.setWaypoint(left_margin);
            }
        }
	}

	private void renderFill(@NonNull GUIRenderer renderer, int y) {
		if (index == 0 && step < left_margin)
			return;
		ProgressBarData data = Skin.getSkin().getProgressBarData();
		ModeIconQuads left = data.getLeftFill();
        ModeIconQuads center = data.getCenterFill();
        ModeIconQuads right = data.getRightFill();

		renderer.drawQuad(left.get(ModeIconQuads.Mode.NORMAL), 0, y, Color.WHITE_INT);

		int offset = index > 0 ? info[index - 1].getWaypoint() : 0;
        IconQuad c = center.quad(ModeIconQuads.Mode.NORMAL);
		renderer.drawQuad(c.getTexture(), left_margin, y, offset - left_margin + (int)step, c.getHeight(), c.getU1(), c.getV1(), c.getU2(), c.getV2(), Color.WHITE_INT);
		
		if (index == info.length) {
			renderer.drawQuad(right.get(ModeIconQuads.Mode.NORMAL), info[index - 1].getWaypoint(), y, Color.WHITE_INT);
		}
	}

	private void update() {
		network.tick();
		Renderer.getRenderer().getWindow().update();
	}
}
