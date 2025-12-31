package com.oddlabs.tt.render;

import com.oddlabs.tt.particle.Particle;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

final class ParticleModelState implements ModelState<Particle> {
    private static final Vector4fc NO_SELECTION = Color.TRANSPARENT;
    private final @NonNull Particle particle;
    private final @NonNull Matrix4fc viewMatrix;

    public ParticleModelState(@NonNull Particle particle, @NonNull Matrix4fc viewMatrix) {
        this.particle = particle;
        this.viewMatrix = viewMatrix;
    }

    @Override
    public @NonNull Particle getModel() {
        return particle;
    }

    @Override
    public @NonNull Vector4fc getTeamColor() {
        return particle.getColor();
    }

    @Override
    public @NonNull Vector4fc getSelectionColor() {
        return NO_SELECTION;
    }

    @Override
    public @NonNull Vector4fc getColor() {
        return particle.getColor();
    }

    @Override
    public @NonNull Matrix4f getTransform(@NonNull Matrix4f dest) {
        // Create the billboard transformation
        // 1. Translate to the particle's position
        dest.translation(particle.getPosX(), particle.getPosY(), particle.getPosZ());

        // 2. Apply the inverse of the camera's rotation to face the camera
        // We can copy the transposed upper 3x3 of the view matrix to achieve this
        dest.m00(viewMatrix.m00()); dest.m01(viewMatrix.m10()); dest.m02(viewMatrix.m20());
        dest.m10(viewMatrix.m01()); dest.m11(viewMatrix.m11()); dest.m12(viewMatrix.m21());
        dest.m20(viewMatrix.m02()); dest.m21(viewMatrix.m12()); dest.m22(viewMatrix.m22());

        // 3. Scale the particle
        dest.scale(particle.getRadiusX(), particle.getRadiusY(), particle.getRadiusZ());

        return dest;
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
