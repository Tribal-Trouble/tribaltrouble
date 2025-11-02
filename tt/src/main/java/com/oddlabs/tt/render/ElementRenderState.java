package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import org.jspecify.annotations.NonNull;

final class ElementRenderState implements ModelState {

    final RenderState render_state;
    ModelVisitor visitor;
    Model model;
    float f;

    ElementRenderState(RenderState render_state) {
        this.render_state = render_state;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public void transform() {
        visitor.transform(this);
    }

    @Override
    public float[] getTeamColor() {
        return visitor.getTeamColor(this);
    }

    @Override
    public float[] getSelectionColor() {
        return visitor.getSelectionColor(this);
    }

    void setup(ModelVisitor visitor, Model model, float f) {
        this.visitor = visitor;
        this.model = model;
        this.f = f;
    }

    void setup(ModelVisitor visitor, Model model) {
        this.visitor = visitor;
        this.model = model;
    }

    @Override
    public void markDetailPoint() {
        visitor.markDetailPoint(this);
    }

    @Override
    public void markDetailPolygon(@NonNull PolyDetail detail) {
        visitor.markDetailPolygon(this, detail);
    }

    @Override
    public int getTriangleCount(@NonNull PolyDetail detail) {
        return visitor.getTriangleCount(this, detail);
    }

    @Override
    public float getEyeDistanceSquared() {
        return visitor.getEyeDistanceSquared(this);
    }

    SpriteRenderer getRenderer(@NonNull SpriteKey key) {
        return render_state.getRenderQueues().getRenderer(key);
    }
}
