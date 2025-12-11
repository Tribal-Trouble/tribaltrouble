package com.oddlabs.tt.render;

import com.oddlabs.tt.particle.Particle;
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
        modelViewStack.translate(particle.getPosX(), particle.getPosY(), particle.getPosZ());
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
