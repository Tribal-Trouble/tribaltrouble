package com.oddlabs.tt.gui;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.GUIRenderer;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

public final class Arrow extends GUIObject {
	private static final float SECONDS_PER_FLASH = .5f;
	private static final float COLOR_DELTA = .5f;

	private final float target_x;
	private final float target_y;
	private final float target_z;
	private final float r;
	private final float g;
	private final float b;
	private final boolean show_always;
	private final GUIRoot gui_root;

	public Arrow(@NonNull HeightMap heightmap, GUIRoot gui_root, float target_x, float target_y, float r, float g, float b, boolean show_always) {
		this.gui_root = gui_root;
		this.target_x = target_x;
		this.target_y = target_y;
		this.target_z = heightmap.getNearestHeight(target_x, target_y);
		this.r = r;
		this.g = g;
		this.b = b;
		this.show_always = show_always;
		displayChangedNotify(LocalInput.getViewWidth(), LocalInput.getViewHeight());
	}

	@Override
	protected void displayChangedNotify(int width, int height) {
		setDim(width, height);
	}

	private static final Vector4f point = new Vector4f();
	private @NonNull Vector4f project3DTo2D(float x, float y, float z) {
		point.set(x,y,z,1);
		gui_root.getDelegate().getCamera().getState().getProjectionModelView().transform(point, point);
		if (point.w < .1f)
			point.w = .1f;
		float inv_w = 1/point.w;
		point.set((point.x*inv_w + 1)*.5f*LocalInput.getViewWidth(), (point.y*inv_w + 1)*.5f*LocalInput.getViewHeight(), 0, 0);
		return point;
	}

	@Override
	protected void renderGeometry(@NonNull GUIRenderer renderer) {
		Vector4f result = project3DTo2D(target_x, target_y, target_z);
		float x = result.x;
		float y = result.y;
		float dx = x - LocalInput.getViewWidth()/2f;
		float dy = y - LocalInput.getViewHeight()/2f;
		float dist_sqr = dx*dx + dy*dy;
		if (dist_sqr < 1f) {
			dx = 1f;
			dy = 0f;
		} else {
			float inv_dist = 1f/(float)Math.sqrt(dist_sqr);
			dx *= inv_dist;
			dy *= inv_dist;
		}

		float angle = (float)Math.toDegrees(Math.acos(dx));
		if (dy < 0f)
			angle = 360f - angle;
		float real_t = (x - LocalInput.getViewWidth()/2f)/dx;
		float t = real_t;
		float t_min_x = (-LocalInput.getViewWidth()/2f)/dx;
		float t_max_x = (LocalInput.getViewWidth()/2f)/dx;
		float t_x = Math.max(t_min_x, t_max_x);
		t = Math.min(t, t_x);
		float t_min_y = (-LocalInput.getViewHeight()/2f)/dy;
		float t_max_y = (LocalInput.getViewHeight()/2f)/dy;
		float t_y = Math.max(t_min_y, t_max_y);
		t = Math.min(t, t_y);
		if (show_always || gui_root.getDelegate().getCamera().getState().inNoDetailMode() || t < real_t) {
			var data = GUIIcons.getIcons().getNotifyArrowData();
			float head_x = data.headX();
			float head_y = data.headY();
			renderer.getMatrixStack().push();
			renderer.getMatrixStack().translate(LocalInput.getViewWidth()/2f + dx*t, LocalInput.getViewHeight()/2f + dy*t, 0f);
			renderer.getMatrixStack().rotate(angle, 0f, 0f, 1f);
			float val = (LocalEventQueue.getQueue().getTime()%SECONDS_PER_FLASH)/(SECONDS_PER_FLASH*.5f);
			if (val > 1f)
				val = 2f - val;
			val = COLOR_DELTA*val;
			IconQuad arrow = data.arrow();
			renderer.drawIcon(arrow, -head_x, -head_y, new Vector4f(r, g, b, 1f - val));
			renderer.getMatrixStack().pop();
		}
	}
}
