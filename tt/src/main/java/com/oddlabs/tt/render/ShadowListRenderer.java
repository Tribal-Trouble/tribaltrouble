package com.oddlabs.tt.render;

import com.oddlabs.tt.render.state.RenderContext;
import org.jspecify.annotations.NonNull;

abstract class ShadowListRenderer extends ShadowRenderer {
	protected abstract void renderShadows(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack);
}
