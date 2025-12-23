package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Model;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

abstract class ModelVisitor<M extends Model> {
	public void markDetailPoint(@NonNull ElementRenderState<M> render_state) {
		M model = render_state.model;
		render_state.getRenderer(model.getSpriteRenderer()).addToNoDetailList(render_state);
	}

	public void markDetailPolygon(@NonNull ElementRenderState<M> render_state, @NonNull PolyDetail detail) {
        M model = render_state.model;
		render_state.getRenderer(model.getSpriteRenderer()).addToRenderList(detail, render_state, render_state.render_state.isResponding(model));
	}

	public final int getTriangleCount(@NonNull ElementRenderState<M> render_state, @NonNull PolyDetail detail) {
        M model = render_state.model;
		return render_state.getRenderer(model.getSpriteRenderer()).getTriangleCount(detail);
	}

	public final float getEyeDistanceSquared(@NonNull ElementRenderState<M> render_state) {
        M model = render_state.model;
		CameraState camera = render_state.render_state.getCamera();
		return RenderTools.getEyeDistanceSquared(model, camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ());
	}

	public abstract void transform(@NonNull ElementRenderState<M> render_state);
    public abstract void getTransform(@NonNull ElementRenderState<M> render_state, @NonNull Matrix4f dest);
	public abstract float @NonNull [] getTeamColor(@NonNull ElementRenderState<M> render_state);
	public abstract float @NonNull [] getSelectionColor(@NonNull ElementRenderState<M> render_state);
}
