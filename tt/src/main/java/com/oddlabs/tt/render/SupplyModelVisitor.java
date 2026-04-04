package com.oddlabs.tt.render;

import com.oddlabs.tt.model.SupplyModel;
import org.jspecify.annotations.NonNull;

abstract class SupplyModelVisitor<SM extends SupplyModel> extends WhiteModelVisitor<SM> {
    @Override
    public final void markDetailPoint(@NonNull ElementRenderState<SM> render_state) {
        markDetailPolygon(render_state, PolyDetail.LOW_POLY);
    }
}
