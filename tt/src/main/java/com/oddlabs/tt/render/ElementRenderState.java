package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import com.oddlabs.util.Color;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ElementRenderState<M extends Model> implements ModelState<M> {

    final @NonNull RenderState render_state;
    private ModelVisitor<M> visitor;
    M model;
    float f;
    final Vector4f color = new Vector4f(Color.WHITE);

    ElementRenderState(@NonNull RenderState render_state) {
        this.render_state = render_state;
    }
    public @NotNull Vector4f getColor() {
        return color;
    }

    public void resetColor() {
        color.set(Color.WHITE);
    }

    @Override
    public @Nullable M getModel() {
        return model;
    }

    @Override
    public @NonNull Matrix4f getTransform(@NonNull Matrix4f dest) {
        visitor.getTransform(this, dest);
        return dest;
    }

    @NotNull
    @Override
    public @NonNull Vector4fc getTeamColor() {
        return visitor.getTeamColor(this);
    }

    @NotNull
    @Override
    public @NonNull Vector4fc getSelectionColor() {
        return visitor.getSelectionColor(this);
    }

    void setup(@NonNull ModelVisitor<M> visitor, @NonNull M model, float f) {
        this.visitor = visitor;
        this.model = model;
        this.f = f;
        resetColor();
    }

    void setup(@NonNull ModelVisitor<M> visitor, @NonNull M model) {
        this.visitor = visitor;
        this.model = model;
        resetColor();
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

    @NonNull SpriteRenderer getRenderer(@NonNull SpriteKey key) {
        return render_state.getRenderQueues().getRenderer(key);
    }
}
