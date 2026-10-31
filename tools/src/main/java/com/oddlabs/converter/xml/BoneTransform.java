package com.oddlabs.converter.xml;

import org.jspecify.annotations.NonNull;

/** One bone's 4x4 matrix in a pose, row by row. */
public record BoneTransform(@NonNull String name, float m00, float m01, float m02, float m03, float m10, float m11,
                            float m12, float m13, float m20, float m21, float m22, float m23, float m30, float m31,
                            float m32,
                            float m33) {

    public float @NonNull [] matrix() {
        return new float[]{m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33};
    }
}
