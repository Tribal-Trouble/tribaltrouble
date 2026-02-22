package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.joml.Matrix4f;
import org.joml.Vector4fc;

class WhiteModelVisitor<M extends Model> extends ModelVisitor<M> {
	private static final Vector4fc COLOR_TEAM = Color.WHITE;

    @Override
	public final @NonNull Vector4fc getSelectionColor(@NonNull ElementRenderState<M> render_state) {
		return COLOR_TEAM;
	}

    @Override
	public final @NonNull Vector4fc getTeamColor(@NonNull ElementRenderState<M> render_state) {
		return COLOR_TEAM;
	}

	@Override
	public void getTransform(@NonNull ElementRenderState<M> render_state, @NonNull Matrix4f dest) {
		Model model = render_state.getModel();
		float angle = (float) Math.toDegrees(Math.atan2(model.getDirectionY(), model.getDirectionX()));
		dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
				.rotate(angle, 0f, 0f, 1f);
	}
}
