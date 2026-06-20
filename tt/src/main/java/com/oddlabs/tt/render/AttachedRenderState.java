package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Specialized render state for handling attached accessories.
 */
final class AttachedRenderState implements ModelState<Model> {
    private @Nullable ElementRenderState<?> parentState;
    private @Nullable Accessory accessory;

    AttachedRenderState() {
    }

    void setup(@NonNull ElementRenderState<?> parentState, @NonNull Accessory accessory) {
        this.parentState = parentState;
        this.accessory = accessory;
    }

    @Override
    public @NonNull Matrix4f getTransform(@NonNull Matrix4f dest) {
        assert parentState != null;
        assert accessory != null;

        parentState.getTransform(dest);
        accessory.getRelativeTransform(dest, parentState.model);

        if (accessory instanceof VisualSoundAccessory) {
            CameraState camera = parentState.render_state.getCamera();
            if (camera != null) {
                float tx = dest.m30();
                float ty = dest.m31();
                float tz = dest.m32();

                float sx = (float) Math.sqrt(dest.m00() * dest.m00() + dest.m01() * dest.m01() + dest.m02() * dest
                        .m02());
                float sy = (float) Math.sqrt(dest.m10() * dest.m10() + dest.m11() * dest.m11() + dest.m12() * dest
                        .m12());
                float sz = (float) Math.sqrt(dest.m20() * dest.m20() + dest.m21() * dest.m21() + dest.m22() * dest
                        .m22());

                dest.translation(tx, ty, tz);

                // Align billboard with camera (inverse of camera's model-view rotation)
                dest.m00(camera.getModelView().m00());
                dest.m01(camera.getModelView().m10());
                dest.m02(camera.getModelView().m20());
                dest.m10(camera.getModelView().m01());
                dest.m11(camera.getModelView().m11());
                dest.m12(camera.getModelView().m21());
                dest.m20(camera.getModelView().m02());
                dest.m21(camera.getModelView().m12());
                dest.m22(camera.getModelView().m22());

                dest.scale(sx, sy, sz);
            }
        }
        return dest;
    }

    @Override
    public @NonNull Color getTeamColor() {
        assert parentState != null;
        return parentState.getTeamColor();
    }

    @Override
    public @NonNull Color getSelectionColor() {
        assert parentState != null;
        return parentState.getSelectionColor();
    }

    @Override
    public @NonNull Color getColor() {
        assert parentState != null;
        Color.Linear parentColor = parentState.getColor();
        if (accessory instanceof VisualSoundAccessory visualSoundAccessory) {
            float alpha = visualSoundAccessory.getAlpha();
            if (alpha < 1.0f) {
                return parentColor.alpha(parentColor.a() * alpha);
            }
        }
        return parentColor;
    }

    @Override
    public Selectable.@NonNull VisualPattern getPattern() {
        assert parentState != null;
        return parentState.getPattern();
    }

    @Override
    public @Nullable Model getModel() {
        assert parentState != null;
        return parentState.model;
    }

    @Override
    public void markDetailPoint() {
        // Accessories typically don't have their own detail markers in the current model
    }

    @Override
    public void markDetailPolygon(@NonNull PolyDetail detail) {
        assert accessory != null;
        assert parentState != null;

        // Standard sprite rendering
        SpriteKey key = accessory.getSpriteRenderer();
        if (key != null) {
            parentState.render_state.getRenderQueues().getRenderer(key).addToRenderList(detail, this,
                    parentState.render_state.isResponding(parentState.model));
        }
    }

    @Override
    public int getTriangleCount(@NonNull PolyDetail detail) {
        assert accessory != null;
        assert parentState != null;

        SpriteKey key = accessory.getSpriteRenderer();
        if (key != null) {
            return parentState.render_state.getRenderQueues().getRenderer(key).getTriangleCount(detail);
        }
        return 0;
    }

    @Override
    public int getAnimation() {
        assert accessory != null;

        return accessory.getAnimation();
    }

    @Override
    public float getAnimationTicks() {
        assert accessory != null;
        assert parentState != null;

        return accessory.getAnimationTicks(parentState.model);
    }

    @Override
    public float getEyeDistanceSquared() {
        assert accessory != null;
        assert parentState != null;

        return parentState.getEyeDistanceSquared();
    }
}
