package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;

abstract class ShadowListRenderer extends ShadowRenderer {
	protected abstract void renderShadows(@NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack);
}
