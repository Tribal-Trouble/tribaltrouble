package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;

abstract class SupplyModelVisitor extends WhiteModelVisitor {
	@Override
	public final void markDetailPoint(@NonNull ElementRenderState render_state) {
		markDetailPolygon(render_state, PolyDetail.LOW_POLY);
	}
}
