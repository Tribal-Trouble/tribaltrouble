package com.oddlabs.tt.render;

import com.oddlabs.tt.particle.Particle;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

public final class ParticleModelState implements ModelState<Particle> {
    private final @NonNull Particle particle;
    private final @NonNull MatrixStack modelViewStack;

    public ParticleModelState(@NonNull Particle particle, @NonNull MatrixStack modelViewStack) {
        this.particle = particle;
        this.modelViewStack = modelViewStack;
    }

    @Override
    public @NonNull Particle getModel() {
        return particle;
    }

    @Override
    public float @NonNull [] getTeamColor() {
        return new float[]{particle.getColorR(), particle.getColorG(), particle.getColorB(), particle.getColorA()};
    }

    @Override
    public float @NonNull [] getSelectionColor() {
        return new float[]{0f, 0f, 0f, 0f};
    }

    @Override
    public @NonNull Vector4f getColor() {
        return new Vector4f(particle.getColorR(), particle.getColorG(), particle.getColorB(), particle.getColorA());
    }

    @Override
    public void transform() {
        // The modelViewStack contains the camera's view matrix.
        // We want to apply a transformation that cancels out the camera's rotation for billboarding.

        // 1. Get the rotation from the view matrix and invert it (transpose for orthonormal)
        Matrix3f rotation = new Matrix3f();
        modelViewStack.current().get3x3(rotation);
        rotation.transpose();

        // 2. Apply transformations to the stack
        modelViewStack.translate(particle.getPosX(), particle.getPosY(), particle.getPosZ());
        modelViewStack.multiply(new Matrix4f(rotation));
        modelViewStack.scale(particle.getRadiusX(), particle.getRadiusY(), particle.getRadiusZ());
    }

    @Override
    public float getEyeDistanceSquared() {
        return 0;
    }

    @Override
    public int getTriangleCount(@NonNull PolyDetail detail) {
        return 2;
    }

    @Override
    public void markDetailPolygon(@NonNull PolyDetail detail) {
        // No-op for particles
    }

    @Override
    public void markDetailPoint() {
        // No-op for particles
    }
}
